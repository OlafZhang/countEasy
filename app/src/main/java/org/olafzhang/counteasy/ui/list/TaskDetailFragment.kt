package org.olafzhang.counteasy.ui.list

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Item
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao
import org.olafzhang.counteasy.ui.task.ItemAdapter
import org.olafzhang.counteasy.utils.NumberFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailFragment : Fragment() {
    private lateinit var taskDao: TaskDao
    private lateinit var toolbar: Toolbar
    private lateinit var tvTaskName: TextView
    private lateinit var tvSummary: TextView
    private lateinit var rvItemList: RecyclerView
    private lateinit var fabExport: FloatingActionButton
    private lateinit var task: Task
    private var items = mutableListOf<Item>()
    
    private val args: TaskDetailFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        taskDao = TaskDao(requireContext())
        
        initViews(view)
        setupToolbar()
        setupExportButton()
        loadTaskData()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        tvTaskName = view.findViewById(R.id.tvTaskName)
        tvSummary = view.findViewById(R.id.tvSummary)
        rvItemList = view.findViewById(R.id.rvItemList)
        fabExport = view.findViewById(R.id.fabExport)
        
        rvItemList.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupExportButton() {
        fabExport.setOnClickListener {
            checkPermissionAndExport()
        }
    }
    
    private fun checkPermissionAndExport() {
        // Android 10+（API 29+）使用分区存储，不需要WRITE_EXTERNAL_STORAGE权限就能访问Download目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 直接导出，不需要检查权限
            exportToExcel()
            return
        }
        
        // 只有Android 9及以下版本才需要检查WRITE_EXTERNAL_STORAGE权限
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        
        // 检查是否有权限
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED) {
            // 有权限，执行导出
            exportToExcel()
        } else {
            // 没有权限，直接提示
            Toast.makeText(
                requireContext(),
                getString(R.string.export_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            
            // 提示用户如何获取权限
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_permission_title)
                .setMessage(R.string.export_permission_manual)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
    
    private fun exportToExcel() {
        try {
            // 生成CSV内容
            val csvData = StringBuilder()
            
            // 添加标题行
            val unitText = when (task.unit) {
                "克" -> "重量（克）"
                "吨" -> "重量（吨）"
                else -> "重量（千克）" // 默认千克
            }
            csvData.append("序号,").append(unitText).append("\n")
            
            // 填充数据
            val validItems = items.filter { it.weight > 0 }
            validItems.forEachIndexed { index, item ->
                csvData.append(index + 1).append(",")
                csvData.append(item.weight).append("\n")
            }
            
            // 生成文件名（任务名称_年月日时分秒.csv）
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "${task.name}_${timestamp}.csv"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvData.toString().toByteArray())
                    }
                    Toast.makeText(requireContext(), getString(R.string.export_success, fileName), Toast.LENGTH_LONG).show()
                }
            } else {
                // Android 9及以下直接使用文件系统
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(csvData.toString().toByteArray())
                }
                
                Toast.makeText(requireContext(), getString(R.string.export_success, fileName), Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.export_failure, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun loadTaskData() {
        val loadedTask = taskDao.getTask(args.taskId) ?: run {
            findNavController().navigateUp()
            return
        }
        
        // 保存任务对象以供后续使用
        task = loadedTask
        
        // 设置任务名称
        tvTaskName.text = task.name
        
        // 获取所有条目
        items = taskDao.getItemsForTask(task.id).toMutableList()
        
        // 更新摘要信息
        updateSummary()
        
        // 设置条目列表
        setupItemList()
    }
    
    private fun setupItemList() {
        val adapter = ItemAdapter(
            items = items,
            unit = task.unit,
            decimalPlaces = task.decimalPlaces,
            onItemClick = { position -> 
                // 单击条目，显示编辑对话框
                showEditItemDialog(position)
            }
        )
        
        // 设置长按监听器
        adapter.setOnItemLongClickListener { position ->
            showDeleteItemDialog(position)
            true
        }
        
        rvItemList.adapter = adapter
    }
    
    private fun showEditItemDialog(position: Int) {
        val item = items[position]
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_item, null)
        
        val weightEditText = dialogView.findViewById<TextInputEditText>(R.id.etItemWeight)
        
        // 设置初始值
        weightEditText.setText(NumberFormatter.formatNumber(item.weight, task.decimalPlaces))
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("修改数据")
            .setView(dialogView)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
        
        // 手动处理保存按钮点击，以便进行验证
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val weightStr = weightEditText.text.toString()
            
            // 验证输入
            if (weightStr.isBlank()) {
                weightEditText.error = "请输入重量"
                return@setOnClickListener
            }
            
            val weight = weightStr.toDoubleOrNull()
            if (weight == null || weight <= 0) {
                weightEditText.error = "请输入有效的重量（大于0）"
                return@setOnClickListener
            }
            
            // 处理小数部分补全
            var formattedWeight = weight
            if (!weightStr.contains(".") && task.decimalPlaces > 0) {
                // 用户没有输入小数部分，自动补全
                formattedWeight = weight.toInt().toDouble()
            }
            
            // 更新数据
            val updatedItem = item.copy(weight = formattedWeight)
            taskDao.updateItem(updatedItem)
            
            // 更新内存中的数据
            items[position] = updatedItem
            
            // 刷新UI
            rvItemList.adapter?.notifyItemChanged(position)
            updateSummary()
            
            Toast.makeText(requireContext(), "数据已更新", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }
    
    private fun showDeleteItemDialog(position: Int) {
        val item = items[position]
        
        AlertDialog.Builder(requireContext())
            .setTitle("删除数据")
            .setMessage("确定要删除这条数据吗？")
            .setPositiveButton("删除") { _, _ ->
                // 从数据库删除
                if (item.id > 0) {
                    taskDao.deleteItem(item.id)
                }
                
                // 从内存中删除
                items.removeAt(position)
                
                // 刷新UI
                rvItemList.adapter?.notifyItemRemoved(position)
                rvItemList.adapter?.notifyItemRangeChanged(position, items.size - position)
                updateSummary()
                
                Toast.makeText(requireContext(), "数据已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun updateSummary() {
        // 只统计有实际数据的条目
        val validItems = items.filter { it.weight > 0 }
        val totalWeight = validItems.sumOf { it.weight }
        val itemCount = validItems.size
        
        val formattedWeight = NumberFormatter.formatNumber(totalWeight, task.decimalPlaces)
        tvSummary.text = "$formattedWeight${task.unit} (${itemCount}项)"
    }
} 