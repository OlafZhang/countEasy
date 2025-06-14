package org.olafzhang.counteasy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao

class MainActivity : AppCompatActivity() {
    private lateinit var taskDao: TaskDao
    private lateinit var navController: NavController
    private var lastSelectedTaskId: Long = -1L
    
    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限被授予，不需要特殊处理
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            // 权限被拒绝，只显示提示信息
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            
            // 将权限状态保存到SharedPreferences
            getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("storage_permission_requested", true)
                .apply()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 检查并请求存储权限
        checkAndRequestStoragePermission()
        
        taskDao = TaskDao(this)
        
        // 从SharedPreferences加载上次选择的任务ID
        lastSelectedTaskId = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getLong("last_selected_task_id", -1L)
        
        // 正确获取NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        
        // 设置导航UI
        navView.setupWithNavController(navController)
        
        // 监听导航变化，保存当前任务ID
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.id == R.id.taskFragment) {
                arguments?.getLong("taskId", -1L)?.let { taskId ->
                    if (taskId != -1L) {
                        lastSelectedTaskId = taskId
                        // 保存到SharedPreferences
                        getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                            .putLong("last_selected_task_id", taskId)
                            .apply()
                    }
                }
            }
        }
        
        // 添加自定义导航处理
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.listFragment -> {
                    if (navController.currentDestination?.id != R.id.listFragment) {
                        navController.navigate(R.id.listFragment)
                    }
                    true
                }
                R.id.taskFragment -> {
                    // 检查当前是否已经在任务页面，如果是则不需要重新导航
                    if (navController.currentDestination?.id == R.id.taskFragment) {
                        return@setOnItemSelectedListener true
                    }
                    
                    // 检查是否有至少一个任务
                    val tasks = taskDao.getAllTasks()
                    if (tasks.isNotEmpty()) {
                        // 使用上次选择的任务ID（如果有效）
                        var taskToShow = if (lastSelectedTaskId != -1L) {
                            // 确保该任务ID仍然存在
                            taskDao.getTask(lastSelectedTaskId) ?: tasks.first()
                        } else {
                            tasks.first()
                        }
                        
                        // 创建包含任务ID的参数
                        val args = Bundle().apply {
                            putLong("taskId", taskToShow.id)
                            putBoolean("goToLast", false) // 不跳转到最后一个条目
                        }
                        
                        // 使用全局导航动作
                        navController.navigate(R.id.global_action_to_task, args)
                    } else {
                        // 创建一个默认任务
                        val defaultTask = Task(
                            name = "默认任务",
                            unit = "千克",
                            decimalPlaces = 2,
                            status = Task.STATUS_IN_PROGRESS
                        )
                        val taskId = taskDao.insertTask(defaultTask)
                        lastSelectedTaskId = taskId
                        
                        // 保存到SharedPreferences
                        getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                            .putLong("last_selected_task_id", taskId)
                            .apply()
                        
                        // 创建包含任务ID的参数
                        val args = Bundle().apply {
                            putLong("taskId", taskId)
                            putBoolean("goToLast", false) // 不跳转到最后一个条目
                        }
                        
                        // 使用全局导航动作
                        navController.navigate(R.id.global_action_to_task, args)
                    }
                    true
                }
                R.id.counterFragment -> {
                    if (navController.currentDestination?.id != R.id.counterFragment) {
                        navController.navigate(R.id.counterFragment)
                    }
                    true
                }
                R.id.settingsFragment -> {
                    if (navController.currentDestination?.id != R.id.settingsFragment) {
                        navController.navigate(R.id.settingsFragment)
                    }
                    true
                }
                else -> false
            }
        }
        
        // 监听导航变化，更新音量键行为
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 当导航到计数器页面时，禁用系统音量键调整
            volumeControlStream = if (destination.id == R.id.counterFragment) {
                // 使用不会影响音量的音频流
                AudioManager.STREAM_NOTIFICATION
            } else {
                // 其他页面使用默认音频流
                AudioManager.USE_DEFAULT_STREAM_TYPE
            }
        }
    }
    
    private fun checkAndRequestStoragePermission() {
        // Android 10+（API 29+）使用分区存储，不需要WRITE_EXTERNAL_STORAGE权限就能访问Download目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 不需要请求权限，MediaStore API可以直接访问Download目录
            return
        }
        
        // 只有Android 9及以下版本才需要请求WRITE_EXTERNAL_STORAGE权限
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        
        // 先检查是否已经请求过权限
        val hasRequested = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("storage_permission_requested", false)
        
        // 检查权限状态
        when {
            // 已有权限，不需要请求
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，不需要操作
            }
            // 应该显示权限请求说明
            shouldShowRequestPermissionRationale(permission) -> {
                // 如果已经请求过并且被拒绝了，就不再显示对话框
                if (hasRequested) {
                    return
                }
                
                showPermissionRationaleDialog()
            }
            // 直接请求权限
            else -> {
                // 如果已经请求过并且被拒绝了，就不再请求
                if (hasRequested) {
                    return
                }
                
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.export_permission_title)
            .setMessage(R.string.export_permission_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                
                // 标记为已请求
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("storage_permission_requested", true)
                    .apply()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                // 标记为已请求
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("storage_permission_requested", true)
                    .apply()
            }
            .show()
    }
} 