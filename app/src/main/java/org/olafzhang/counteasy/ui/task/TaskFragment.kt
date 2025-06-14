package org.olafzhang.counteasy.ui.task

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Item
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao
import org.olafzhang.counteasy.utils.NumberFormatter
import org.olafzhang.counteasy.utils.TtsManager
import android.widget.Toast

class TaskFragment : Fragment() {
    private lateinit var taskDao: TaskDao
    private lateinit var ttsManager: TtsManager
    private lateinit var task: Task
    private var currentPosition = 0
    private var currentInput = ""
    private var items = mutableListOf<Item>()
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var vibrator: Vibrator

    private lateinit var tvTaskName: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvCurrentInput: TextView
    private lateinit var rvItemList: RecyclerView
    private lateinit var numberPickerView: NumberPickerView
    
    // 三元素数据展示的UI引用
    private lateinit var tvPrevWeight: TextView
    private lateinit var tvPrevIndex: TextView
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvCurrentIndex: TextView
    private lateinit var tvNextWeight: TextView
    private lateinit var tvNextIndex: TextView
    
    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private lateinit var btn7: Button
    private lateinit var btn8: Button
    private lateinit var btn9: Button
    private lateinit var btnDot: Button
    private lateinit var btnBackspace: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    
    // 布局相关变量
    private var isKeyboardLayoutAsc: Boolean = false  // 默认倒序
    private var isZeroCenter: Boolean = true

    private val args: TaskFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskDao = TaskDao(requireContext())
        ttsManager = TtsManager.getInstance(requireContext())
        
        // 初始化振动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        task = taskDao.getTask(args.taskId) ?: run {
            requireActivity().onBackPressed()
            return
        }

        initViews(view)
        loadItems()
        
        // 检查是否需要跳转到最后一个条目
        if (args.goToLast && items.isNotEmpty()) {
            currentPosition = items.size - 1
            numberPickerView.setCurrentPosition(currentPosition)
            loadInputFromDb()
        }
        
        setupItemsList()
        setupKeyboard(view)
        updateTaskInfo() 
        updateTripleDataDisplay() // 更新三元素数据显示
    }

    private fun initViews(view: View) {
        tvTaskName = view.findViewById(R.id.tvTaskName)
        tvSummary = view.findViewById(R.id.tvSummary)
        tvCurrentInput = view.findViewById(R.id.tvCurrentInput)
        rvItemList = view.findViewById(R.id.rvItemList)
        numberPickerView = view.findViewById(R.id.numberPickerView)
        
        // 初始化三元素数据展示的UI
        tvPrevWeight = view.findViewById(R.id.tvPrevWeight)
        tvPrevIndex = view.findViewById(R.id.tvPrevIndex)
        tvCurrentWeight = view.findViewById(R.id.tvCurrentWeight)
        tvCurrentIndex = view.findViewById(R.id.tvCurrentIndex)
        tvNextWeight = view.findViewById(R.id.tvNextWeight)
        tvNextIndex = view.findViewById(R.id.tvNextIndex)
        
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        
        // 初始化键盘按钮
        btn0 = view.findViewById(R.id.btn0)
        btn1 = view.findViewById(R.id.btn1)
        btn2 = view.findViewById(R.id.btn2)
        btn3 = view.findViewById(R.id.btn3)
        btn4 = view.findViewById(R.id.btn4)
        btn5 = view.findViewById(R.id.btn5)
        btn6 = view.findViewById(R.id.btn6)
        btn7 = view.findViewById(R.id.btn7)
        btn8 = view.findViewById(R.id.btn8)
        btn9 = view.findViewById(R.id.btn9)
        btnDot = view.findViewById(R.id.btnDot)
        btnBackspace = view.findViewById(R.id.btnBackspace)
        
        // 加载设置并应用键盘布局
        loadKeyboardSettings()
        applyKeyboardLayout()
    }

    private fun setupItemsList() {
        rvItemList.layoutManager = LinearLayoutManager(requireContext())
        itemAdapter = ItemAdapter(
            items = items,
            unit = task.unit,
            decimalPlaces = task.decimalPlaces,
            onItemClick = { position ->
                // 点击列表项时，切换到对应的条目
                currentPosition = position
                numberPickerView.setCurrentPosition(position)
                loadInputFromDb()
                updateCurrentInputTextView()
            }
        )
        rvItemList.adapter = itemAdapter
    }

    private fun loadItems() {
        items = taskDao.getItemsForTask(task.id).toMutableList()
        numberPickerView.setItems(items.map { it.item_index })
        numberPickerView.setCurrentPosition(currentPosition)
        loadInputFromDb() // 加载当前输入
    }

    private fun refreshItemsList() {
        // 更新适配器数据
        itemAdapter = ItemAdapter(
            items = items,
            unit = task.unit,
            decimalPlaces = task.decimalPlaces,
            onItemClick = { position ->
                currentPosition = position
                numberPickerView.setCurrentPosition(position)
                loadInputFromDb()
                updateCurrentInputTextView()
            }
        )
        rvItemList.adapter = itemAdapter
        
        // 滚动到当前项
        if (items.isNotEmpty() && currentPosition < items.size) {
            rvItemList.scrollToPosition(currentPosition)
        }
    }

    private fun setupKeyboard(view: View) {
        val numberButtons = listOf(btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn0)
        numberButtons.forEachIndexed { index, button ->
            // 数字按钮的索引与实际数字可能不同，这里做个映射
            val numberValue = if (index == 9) 0 else index + 1
            button.setOnClickListener { onNumberClick(numberValue) }
        }

        btnDot.setOnClickListener { onDotClick() }
        btnBackspace.setOnClickListener { onBackspaceClick() }
        
        // 添加退格键长按事件
        btnBackspace.setOnLongClickListener { 
            onBackspaceLongClick()
            true // 返回true表示事件已处理
        }
        
        // 为上下按钮添加普通点击事件
        btnPrev.setOnClickListener { onPrevClick() }
        btnNext.setOnClickListener { onNextClick() }
        
        // 为上下按钮添加长按事件 - 使用标准Android长按
        btnPrev.setOnLongClickListener {
            onPrevLongClick()
            true // 返回true表示事件已处理
        }
        
        btnNext.setOnLongClickListener {
            onNextLongClick()
            true // 返回true表示事件已处理
        }
    }

    private fun onNumberClick(number: Int) {
        // 触发振动反馈
        performHapticFeedback()
        
        if (currentInput.length >= 10) return
        
        // 检查当前输入是否是需要清空的情况
        val shouldClear = when {
            currentInput.isEmpty() -> false // 空的直接输入
            currentInput == "0" -> true // 只有0时需要清空
            task.decimalPlaces > 0 && currentInput.matches(Regex("0\\.0{${task.decimalPlaces}}")) -> true // 0.00...（占满小数位）
            else -> false
        }
        
        if (shouldClear && number != 0) {
            currentInput = "" // 清空当前输入
        }
        
        // 检查小数位数限制
        if (currentInput.contains(".") && 
            currentInput.substringAfter(".").length >= task.decimalPlaces && task.decimalPlaces > 0) return
        
        // 防止输入多个0
        if (currentInput == "0" && number == 0 && task.decimalPlaces == 0) return
        
        currentInput += number
        updateCurrentInputTextView()
        ttsManager.speak(number.toString())
    }

    private fun onDotClick() {
        // 触发振动反馈
        performHapticFeedback()
        
        if (task.decimalPlaces == 0) return // 如果不允许小数，则不处理
        if (currentInput.contains(".")) return
        if (currentInput.isEmpty()) currentInput = "0"
        currentInput += "."
        updateCurrentInputTextView()
        ttsManager.speak("点")
    }

    private fun onBackspaceClick() {
        // 触发振动反馈
        performHapticFeedback()
        
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateCurrentInputTextView()
            ttsManager.speak("退格")
        }
    }

    private fun onBackspaceLongClick() {
        // 长按退格键清空输入
        currentInput = ""
        updateCurrentInputTextView()
        
        // 防止自动设置为0，我们需要确保界面上显示的是0但实际数据不受影响
        if (currentPosition < items.size) {
            // 不保存，只更新显示
            val position = currentPosition + 1
            tvCurrentInput.text = "0 (#$position)"
        }
        
        ttsManager.speak("清空")
    }

    private fun onPrevClick() {
        // 触发振动反馈
        performHapticFeedback()
        
        if (currentPosition > 0) {
            // 保存当前输入
            if (currentInput.isNotEmpty()) {
                saveCurrentInputToDb()
            }
            
            // 移动到上一个位置
            currentPosition--
            
            // 加载新位置的数据 - 不要清空，直接加载
            loadInputFromDb()
            
            numberPickerView.setCurrentPosition(currentPosition)
            numberPickerView.scrollToPosition(currentPosition)
            updateTripleDataDisplay() // 更新三元素数据显示
            ttsManager.speak("上") // TTS提示
            updateSummary() // 上下导航时更新总计
        }
    }

    private fun onNextClick() {
        // 触发振动反馈
        performHapticFeedback()
        
        // 检查当前输入是否为空或0
        val weight = currentInput.toDoubleOrNull() ?: 0.0
        if (weight <= 0.0) {
            ttsManager.speak("空数据") // TTS提示不允许添加空数据
            return // 直接返回，不执行后续添加操作
        }
        
        // 保存当前输入
        saveCurrentInputToDb()
        
        // 确定下一个位置
        if (currentPosition < items.size - 1) {
            // 如果不是最后一个条目，只需移动到下一个
            currentPosition++
            // 加载新位置的数据 - 不要清空，直接加载
            loadInputFromDb()
        } else if (items.size < 10000) { // 限制最多10000个条目
            // 如果是最后一个条目，添加新条目
            val newIndex = items.size  // 使用当前列表大小作为新索引
            val newItem = Item(taskId = task.id, weight = 0.0, item_index = newIndex) // 初始重量为0
            items.add(newItem) // 添加到内存列表
            
            // 更新UI相关内容
            numberPickerView.setItems(items.map { it.item_index })
            currentPosition = items.size - 1
            
            // 新条目时才清空输入
            currentInput = ""
            updateCurrentInputTextView()
        }
        
        // 更新UI
        numberPickerView.setCurrentPosition(currentPosition)
        numberPickerView.scrollToPosition(currentPosition)
        updateTripleDataDisplay() // 更新三元素数据显示
        ttsManager.speak("下") // TTS提示
        updateSummary() // 上下导航时更新总计
    }

    private fun saveCurrentInputToDb() {
        // 只有当有有效输入时才保存
        val weight = currentInput.toDoubleOrNull() ?: 0.0
        if (weight <= 0.0) return
        
        if (currentPosition < items.size) {
            // 现有条目
            val currentItem = items[currentPosition]
            if (currentItem.weight != weight) { // 只有当值变化时才更新
                val updatedItem = currentItem.copy(weight = weight)
                
                if (currentItem.id == 0L) {
                    // 新条目还未保存到数据库
                    val newId = taskDao.addItem(updatedItem)
                    items[currentPosition] = updatedItem.copy(id = newId)
                } else {
                    // 已有条目，更新数据库
                    taskDao.updateItem(updatedItem)
                    items[currentPosition] = updatedItem
                }
            }
        } else {
            // 超出范围，创建新条目
            val newItem = Item(taskId = task.id, weight = weight, item_index = currentPosition)
            val newId = taskDao.addItem(newItem)
            items.add(newItem.copy(id = newId))
            numberPickerView.setItems(items.map { it.item_index })
        }
    }

    private fun loadInputFromDb() {
        if (currentPosition < items.size) {
            // 如果是现有条目，显示其值
            val item = items[currentPosition]
            currentInput = NumberFormatter.formatNumber(item.weight, task.decimalPlaces)
            
            // 确保不会显示"0"，如果值为0则使用""以便用户可以直接输入
            if (currentInput == "0") {
                currentInput = ""
            }
        } else {
            // 新条目，输入为空
            currentInput = ""
        }
        updateCurrentInputTextView()
    }

    private fun updateCurrentInputTextView() {
        // 根据是否为空和是否有小数来格式化显示
        val displayInput = when {
            currentInput.isEmpty() -> "0"
            currentInput.endsWith(".") -> currentInput
            currentInput.toDoubleOrNull() == 0.0 && !currentInput.contains(".") -> "0" // 如果是0.0且没有小数点，显示0
            else -> currentInput
        }
        
        // 添加条目编号显示，不显示单位
        val position = if (currentPosition < items.size) currentPosition + 1 else items.size + 1
        tvCurrentInput.text = "$displayInput (#$position)"
        
        // 不自动保存，只在导航时保存
    }
    
    // 更新三元素数据显示（上一个、当前、下一个）
    private fun updateTripleDataDisplay() {
        // 更新上一个数据
        if (currentPosition > 0) {
            val prevItem = items[currentPosition - 1]
            val formattedWeight = NumberFormatter.formatNumber(prevItem.weight, task.decimalPlaces)
            tvPrevWeight.text = formattedWeight // 移除单位
            tvPrevIndex.text = "#${currentPosition}" // 使用位置索引+1，当前位置是currentPosition，所以上一个是currentPosition
            tvPrevWeight.visibility = View.VISIBLE
            tvPrevIndex.visibility = View.VISIBLE
        } else {
            // 如果没有上一个数据，隐藏上一个数据的显示
            tvPrevWeight.visibility = View.INVISIBLE
            tvPrevIndex.visibility = View.INVISIBLE
        }
        
        // 更新当前数据
        if (currentPosition < items.size) {
            val currentItem = items[currentPosition]
            val formattedWeight = NumberFormatter.formatNumber(currentItem.weight, task.decimalPlaces)
            tvCurrentWeight.text = formattedWeight // 移除单位
            tvCurrentIndex.text = "#${currentPosition + 1}" // 位置从0开始，显示从1开始
        } else {
            // 新条目显示
            tvCurrentWeight.text = "0" // 移除单位
            tvCurrentIndex.text = "#${currentPosition + 1}" // 确保新条目的编号也是连续的
        }
        
        // 更新下一个数据
        if (currentPosition < items.size - 1) {
            val nextItem = items[currentPosition + 1]
            val formattedWeight = NumberFormatter.formatNumber(nextItem.weight, task.decimalPlaces)
            tvNextWeight.text = formattedWeight // 移除单位
            tvNextIndex.text = "#${currentPosition + 2}" // 下一个位置是currentPosition+1，显示需要+1变成currentPosition+2
            tvNextWeight.visibility = View.VISIBLE
            tvNextIndex.visibility = View.VISIBLE
        } else {
            // 如果没有下一个数据，隐藏下一个数据的显示
            tvNextWeight.visibility = View.INVISIBLE
            tvNextIndex.visibility = View.INVISIBLE
        }
    }
    
    // 添加更新任务信息的方法，但不计算总计
    private fun updateTaskInfo() {
        // 处理任务名称，如果超过12个字符则截断
        val taskName = task.name
        tvTaskName.text = taskName
        updateSummary() // 现在改为同时更新总计
    }

    private fun updateSummary() {
        // 只统计有实际数据(weight > 0)的条目
        val validItems = items.filter { it.weight > 0 }
        val totalWeight = validItems.sumOf { it.weight }
        val itemCount = validItems.size
        
        val formattedWeight = NumberFormatter.formatNumber(totalWeight, task.decimalPlaces)
        tvSummary.text = "$formattedWeight${task.unit}\n${itemCount}项"
    }

    override fun onPause() {
        super.onPause()
        saveCurrentInputToDb() // 保存当前输入
        updateSummary() // 确保离开页面前更新总计
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentInputToDb() // 保存当前输入
        updateSummary() // 确保销毁页面前更新总计
    }

    // 长按上按钮，快速跳转到第一个条目
    private fun onPrevLongClick() {
        // 添加日志输出
        android.util.Log.d("TaskFragment", "onPrevLongClick called, currentPosition=$currentPosition, items.size=${items.size}")
        
        if (currentPosition > 0) {
            // 提供更强的振动反馈，表示长按被触发
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
            
            // 保存当前输入
            if (currentInput.isNotEmpty()) {
                saveCurrentInputToDb()
            }
            
            // 直接跳转到第一个条目
            currentPosition = 0
            
            // 加载新位置的数据
            loadInputFromDb()
            
            numberPickerView.setCurrentPosition(currentPosition)
            numberPickerView.scrollToPosition(currentPosition)
            updateTripleDataDisplay()
            ttsManager.speak("首条")
            updateSummary()
        }
    }
    
    // 长按下按钮，快速跳转到最后一个条目
    private fun onNextLongClick() {
        // 添加日志输出
        android.util.Log.d("TaskFragment", "onNextLongClick called, currentPosition=$currentPosition, items.size=${items.size}")
        
        if (items.isNotEmpty() && currentPosition < items.size - 1) {
            // 提供更强的振动反馈，表示长按被触发
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
            
            // 保存当前输入
            if (currentInput.isNotEmpty()) {
                saveCurrentInputToDb()
            }
            
            // 直接跳转到最后一个条目（不是新建）
            currentPosition = items.size - 1
            
            // 加载新位置的数据
            loadInputFromDb()
            
            numberPickerView.setCurrentPosition(currentPosition)
            numberPickerView.scrollToPosition(currentPosition)
            updateTripleDataDisplay()
            ttsManager.speak("末条")
            updateSummary()
        }
    }

    // 执行振动反馈
    private fun performHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0及以上使用VibrationEffect
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // 低版本Android使用旧API
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }

    private fun loadKeyboardSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        // 加载键盘布局设置，默认倒序
        isKeyboardLayoutAsc = prefs.getBoolean("keyboard_layout_asc", false) // 默认倒序
        
        // 加载0的位置设置，默认0在中间
        isZeroCenter = prefs.getBoolean("zero_center", true) // 默认0在中间
    }
    
    private fun applyKeyboardLayout() {
        val bottomRow = btnDot.parent as ViewGroup
        
        // 根据设置设置数字键盘布局（正序/倒序）
        if (!isKeyboardLayoutAsc) {
            // 倒序排列：从下到上是789, 456, 123
            val row1 = btn1.parent as ViewGroup
            val row2 = btn4.parent as ViewGroup
            val row3 = btn7.parent as ViewGroup
            
            // 在添加视图前，先确保它们从原父视图中移除
            // 第一行：7, 8, 9
            if (btn7.parent != null) (btn7.parent as? ViewGroup)?.removeView(btn7)
            if (btn8.parent != null) (btn8.parent as? ViewGroup)?.removeView(btn8)
            if (btn9.parent != null) (btn9.parent as? ViewGroup)?.removeView(btn9)
            
            row1.removeAllViews()
            row1.addView(btn7)
            row1.addView(btn8)
            row1.addView(btn9)
            
            // 第二行：4, 5, 6
            if (btn4.parent != null) (btn4.parent as? ViewGroup)?.removeView(btn4)
            if (btn5.parent != null) (btn5.parent as? ViewGroup)?.removeView(btn5)
            if (btn6.parent != null) (btn6.parent as? ViewGroup)?.removeView(btn6)
            
            row2.removeAllViews()
            row2.addView(btn4)
            row2.addView(btn5)
            row2.addView(btn6)
            
            // 第三行：1, 2, 3
            if (btn1.parent != null) (btn1.parent as? ViewGroup)?.removeView(btn1)
            if (btn2.parent != null) (btn2.parent as? ViewGroup)?.removeView(btn2)
            if (btn3.parent != null) (btn3.parent as? ViewGroup)?.removeView(btn3)
            
            row3.removeAllViews()
            row3.addView(btn1)
            row3.addView(btn2)
            row3.addView(btn3)
        }
        
        // 应用退格键位置和0的位置，退格键固定在最左边
        // 在添加视图前，先确保它们从原父视图中移除
        if (btnBackspace.parent != null) (btnBackspace.parent as? ViewGroup)?.removeView(btnBackspace)
        if (btn0.parent != null) (btn0.parent as? ViewGroup)?.removeView(btn0)
        if (btnDot.parent != null) (btnDot.parent as? ViewGroup)?.removeView(btnDot)
        
        bottomRow.removeAllViews()
        
        // 退格键始终在左边
        bottomRow.addView(btnBackspace)
        
        if (isZeroCenter) {
            // 0在中间
            bottomRow.addView(btn0)
            bottomRow.addView(btnDot)
        } else {
            // 0在右边
            bottomRow.addView(btnDot)
            bottomRow.addView(btn0)
        }
        
        // 更新每个按钮的布局参数，确保它们占据相同的空间
        for (i in 0 until bottomRow.childCount) {
            val child = bottomRow.getChildAt(i)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            params.setMargins(
                resources.getDimensionPixelSize(R.dimen.button_margin_horizontal),
                0,
                resources.getDimensionPixelSize(R.dimen.button_margin_horizontal),
                0
            )
            child.layoutParams = params
        }
    }
} 