package org.olafzhang.counteasy.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao
import org.olafzhang.counteasy.utils.TtsManager
import androidx.appcompat.app.AlertDialog
import android.widget.ListView
import androidx.core.content.ContextCompat
import android.content.Context

class ListFragment : Fragment() {
    private lateinit var taskDao: TaskDao
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var ttsManager: TtsManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskDao = TaskDao(requireContext())
        ttsManager = TtsManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyList = view.findViewById(R.id.tvEmptyList)
        setupRecyclerView(view)
        setupFab(view)
    }

    private fun setupRecyclerView(view: View) {
        taskAdapter = TaskAdapter(
            taskDao,
            onTaskClick = { task ->
                val action = ListFragmentDirections.actionListToTask(task.id, true)
                findNavController().navigate(action)
            },
            onTaskLongClick = { task ->
                showTaskOptionsDialog(task)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }

        loadTasks()
    }

    private fun setupFab(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fabAddTask).setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun loadTasks() {
        // 添加日志输出
        android.util.Log.d("ListFragment", "loadTasks called")
        
        // 获取最新的任务列表
        val tasks = taskDao.getAllTasks()
        
        // 强制刷新适配器
        taskAdapter.submitList(null)
        taskAdapter.submitList(tasks)
        
        // 控制空列表提示的显示
        if (tasks.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyList.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyList.visibility = View.GONE
        }
        
        // 添加日志输出
        android.util.Log.d("ListFragment", "Tasks loaded: ${tasks.size}")
    }

    private fun showTaskOptionsDialog(task: Task) {
        val options = arrayOf("查看数据", "编辑任务", "清空任务", "删除任务")
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("任务操作")
            .create()
            
        // 创建自定义ListView
        val listView = ListView(requireContext())
        listView.adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                
                // 为删除和清空任务设置红色字体
                if (position == 2 || position == 3) { // 清空任务和删除任务的位置
                    textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                } else {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
                
                return view
            }
        }
        
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> navigateToTaskDetail(task)
                1 -> showEditTaskDialog(task)
                2 -> showClearDialog(task)  // 清空任务调整到前面
                3 -> showDeleteDialog(task) // 删除任务调整到最后
            }
            dialog.dismiss()
        }
        
        dialog.setView(listView)
        dialog.show()
    }

    private fun navigateToTaskDetail(task: Task) {
        val action = ListFragmentDirections.actionListToTaskDetail(task.id)
        findNavController().navigate(action)
    }

    private fun showEditTaskDialog(task: Task) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑任务")
            .setView(R.layout.dialog_edit_task)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        val taskNameEditText = dialog.findViewById<TextInputEditText>(R.id.etTaskName)
        val unitSpinner = dialog.findViewById<Spinner>(R.id.spinnerUnit)
        val numberPicker = dialog.findViewById<NumberPicker>(R.id.numberPicker)
        val tvDecimalPlacesInfo = dialog.findViewById<TextView>(R.id.tvDecimalPlacesInfo)

        // 设置初始值
        taskNameEditText?.setText(task.name)

        // 设置单位选项
        val units = arrayOf("千克", "克", "吨")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner?.adapter = spinnerAdapter
        
        // 设置初始选中的单位
        val unitIndex = units.indexOf(task.unit)
        if (unitIndex >= 0) {
            unitSpinner?.setSelection(unitIndex)
        }

        // 设置小数点位选择器 - 编辑时禁用
        numberPicker?.apply {
            minValue = 0
            maxValue = 5
            value = task.decimalPlaces
            isEnabled = false // 禁用小数点位选择器
        }
        
        // 更新小数点位的提示信息
        tvDecimalPlacesInfo?.text = "小数点位数 (${task.decimalPlaces})\n(只能在创建任务时设置)"

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val taskName = taskNameEditText?.text?.toString()
            if (taskName.isNullOrBlank()) {
                taskNameEditText?.error = "请输入任务名称"
                return@setOnClickListener
            }

            // 检查任务名称是否重复（排除当前任务）
            if (taskName != task.name && taskDao.isTaskNameExists(taskName)) {
                taskNameEditText?.error = "任务名称已存在"
                return@setOnClickListener
            }

            val unit = unitSpinner?.selectedItem?.toString() ?: "千克"
            // 保持原来的小数点位不变
            val decimalPlaces = task.decimalPlaces

            val updatedTask = task.copy(
                name = taskName,
                unit = unit,
                decimalPlaces = decimalPlaces
            )

            taskDao.updateTask(updatedTask)
            loadTasks()
            Toast.makeText(requireContext(), "任务已更新", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun showClearDialog(task: Task) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空任务")
            .setMessage("确定要清空这个任务的所有数据吗？\n此操作不可撤销！")
            .setPositiveButton("确定", null)  // 先设为null，后面手动处理
            .setNegativeButton("取消", null)
            .create()
            
        dialog.show()
        
        // 获取确定按钮并设置倒计时
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.text = "确定 (3)"
        
        val countDownHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var secondsLeft = 3
        
        val countDownRunnable = object : Runnable {
            override fun run() {
                secondsLeft--
                if (secondsLeft > 0) {
                    positiveButton.text = "确定 ($secondsLeft)"
                    countDownHandler.postDelayed(this, 1000)
                } else {
                    positiveButton.text = "确定"
                    positiveButton.isEnabled = true
                }
            }
        }
        
        countDownHandler.postDelayed(countDownRunnable, 1000)
        
        // 设置确定按钮的点击事件
        positiveButton.setOnClickListener {
            // 清空任务的所有数据，但保留任务本身
            taskDao.clearTaskItems(task.id)
            // 刷新列表显示
            loadTasks()
            Toast.makeText(requireContext(), "任务数据已清空", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun showDeleteDialog(task: Task) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？")
            .setPositiveButton("确定", null)  // 先设为null，后面手动处理
            .setNegativeButton("取消", null)
            .create()
            
        dialog.show()
        
        // 获取确定按钮并设置倒计时
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.text = "确定 (3)"
        
        val countDownHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var secondsLeft = 3
        
        val countDownRunnable = object : Runnable {
            override fun run() {
                secondsLeft--
                if (secondsLeft > 0) {
                    positiveButton.text = "确定 ($secondsLeft)"
                    countDownHandler.postDelayed(this, 1000)
                } else {
                    positiveButton.text = "确定"
                    positiveButton.isEnabled = true
                }
            }
        }
        
        countDownHandler.postDelayed(countDownRunnable, 1000)
        
        // 设置确定按钮的点击事件
        positiveButton.setOnClickListener {
            taskDao.deleteTask(task.id)
            loadTasks()
            Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun showAddTaskDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("新建任务")
            .setView(R.layout.dialog_add_task)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        val taskNameEditText = dialog.findViewById<TextInputEditText>(R.id.etTaskName)
        val unitSpinner = dialog.findViewById<Spinner>(R.id.spinnerUnit)
        val numberPicker = dialog.findViewById<NumberPicker>(R.id.numberPicker)
        val tvDecimalPlacesInfo = dialog.findViewById<TextView>(R.id.tvDecimalPlacesInfo)

        // 设置单位选项 - 只使用重量单位
        val units = arrayOf("千克", "克", "吨")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner?.adapter = spinnerAdapter
        
        // 读取用户设置的默认单位
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val defaultUnitPosition = prefs.getInt("default_unit", 1) // 默认为克
        unitSpinner?.setSelection(defaultUnitPosition)

        // 设置小数点位选择器
        numberPicker?.apply {
            minValue = 0
            maxValue = 5
            // 读取用户设置的默认小数位数
            value = prefs.getInt("default_decimal", 2)  // 默认2位小数
        }
        
        // 设置小数点位提示信息
        tvDecimalPlacesInfo?.text = "小数点位数\n(创建后不可修改)"

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            var taskName = taskNameEditText?.text?.toString()
            
            // 如果任务名为空，生成临时任务名
            if (taskName.isNullOrBlank()) {
                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.getDefault())
                val timestamp = dateFormat.format(java.util.Date())
                taskName = "临时任务$timestamp"
            }

            // 检查任务名称是否重复
            if (taskDao.isTaskNameExists(taskName)) {
                taskNameEditText?.error = "任务名称已存在"
                return@setOnClickListener
            }

            val unit = unitSpinner?.selectedItem?.toString() ?: "千克"
            val decimalPlaces = numberPicker?.value ?: 2

            val task = Task(
                name = taskName,
                unit = unit,
                decimalPlaces = decimalPlaces,
                status = Task.STATUS_IN_PROGRESS
            )

            taskDao.insertTask(task)
            loadTasks()
            Toast.makeText(requireContext(), "任务已创建", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次Fragment恢复可见状态时刷新列表
        loadTasks()
    }
} 