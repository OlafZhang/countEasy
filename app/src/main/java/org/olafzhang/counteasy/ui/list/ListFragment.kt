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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao
import org.olafzhang.counteasy.utils.TtsManager

class ListFragment : Fragment() {
    private lateinit var taskDao: TaskDao
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var ttsManager: TtsManager

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
        setupRecyclerView(view)
        setupFab(view)
    }

    private fun setupRecyclerView(view: View) {
        taskAdapter = TaskAdapter(
            taskDao,
            onTaskClick = { task ->
                val action = ListFragmentDirections.actionListToTask(task.id)
                findNavController().navigate(action)
            },
            onTaskLongClick = { task ->
                showTaskOptionsDialog(task)
            }
        )

        view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView).apply {
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
        val tasks = taskDao.getAllTasks()
        taskAdapter.submitList(tasks)
    }

    private fun showTaskOptionsDialog(task: Task) {
        val options = arrayOf("查看数据", "编辑任务", "删除任务")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("任务操作")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToTaskDetail(task)
                    1 -> showEditTaskDialog(task)
                    2 -> showDeleteDialog(task)
                }
            }
            .show()
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
            maxValue = 3
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

    private fun showDeleteDialog(task: Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除任务")
            .setMessage("确定要删除这个任务吗？")
            .setPositiveButton("确定") { _, _ ->
                taskDao.deleteTask(task.id)
                loadTasks()
                Toast.makeText(requireContext(), "任务已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
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

        // 设置小数点位选择器
        numberPicker?.apply {
            minValue = 0
            maxValue = 3
            value = 2  // 默认2位小数
        }
        
        // 设置小数点位提示信息
        tvDecimalPlacesInfo?.text = "小数点位数\n(创建后不可修改)"

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val taskName = taskNameEditText?.text?.toString()
            if (taskName.isNullOrBlank()) {
                taskNameEditText?.error = "请输入任务名称"
                return@setOnClickListener
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
} 