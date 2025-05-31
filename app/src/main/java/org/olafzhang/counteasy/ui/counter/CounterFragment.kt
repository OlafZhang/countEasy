package org.olafzhang.counteasy.ui.counter

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.utils.TtsManager

class CounterFragment : Fragment() {
    private lateinit var ttsManager: TtsManager
    private lateinit var viewModel: CounterViewModel
    private var resetHandler: Handler? = null
    private var resetRunnable: Runnable? = null

    private lateinit var tvCount: TextView
    private lateinit var btnReset: ImageButton
    private lateinit var btnPlus: Button
    private lateinit var btnMinus: Button
    private lateinit var btnAdd10: Button
    private lateinit var btnAdd50: Button
    private lateinit var btnAdd100: Button
    
    // 上一次音量键按下的时间，用于防止长按
    private var lastVolumeKeyPressTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_counter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取ViewModel
        viewModel = ViewModelProvider(requireActivity())[CounterViewModel::class.java]
        ttsManager = TtsManager.getInstance(requireContext())
        
        setupViews(view)
        setupVolumeKeyListener()
        updateCount() // 初始化显示
    }

    private fun setupViews(view: View) {
        tvCount = view.findViewById(R.id.tvCount)
        btnReset = view.findViewById(R.id.btnReset)
        btnPlus = view.findViewById(R.id.btnPlus)
        btnMinus = view.findViewById(R.id.btnMinus)
        btnAdd10 = view.findViewById(R.id.btnAdd10)
        btnAdd50 = view.findViewById(R.id.btnAdd50)
        btnAdd100 = view.findViewById(R.id.btnAdd100)

        btnReset.setOnLongClickListener {
            startResetTimer()
            true
        }

        btnReset.setOnClickListener {
            cancelResetTimer()
        }

        btnPlus.setOnClickListener {
            increment(1)
        }

        btnMinus.setOnClickListener {
            decrement()
        }
        
        btnAdd10.setOnClickListener {
            increment(10)
        }
        
        btnAdd50.setOnClickListener {
            increment(50)
        }
        
        btnAdd100.setOnClickListener {
            increment(100)
        }
    }
    
    private fun setupVolumeKeyListener() {
        // 设置视图树监听器来捕获按键事件
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { _, keyCode, event ->
            // 只处理按键按下事件（不处理按键抬起事件）
            if (event.action == KeyEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                
                // 防止长按：如果距离上次按键时间小于300毫秒，忽略此次按键
                if (currentTime - lastVolumeKeyPressTime < 300) {
                    return@setOnKeyListener true
                }
                
                lastVolumeKeyPressTime = currentTime
                
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        // 音量加键：增加计数
                        increment(1)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        // 音量减键：减少计数
                        decrement()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun startResetTimer() {
        resetHandler = Handler(Looper.getMainLooper())
        resetRunnable = Runnable {
            viewModel.count = 0
            updateCount()
            ttsManager.speak("重置")
        }
        resetHandler?.postDelayed(resetRunnable!!, 1500)
    }

    private fun cancelResetTimer() {
        resetHandler?.removeCallbacks(resetRunnable!!)
    }

    private fun increment(amount: Int = 1) {
        viewModel.count += amount
        updateCount()
        when (amount) {
            1 -> ttsManager.speak("加")
            10 -> ttsManager.speak("加十")
            50 -> ttsManager.speak("加五十")
            100 -> ttsManager.speak("加一百")
            else -> ttsManager.speak("加${amount}")
        }
    }

    private fun decrement() {
        if (viewModel.count > 0) {
            viewModel.count--
            updateCount()
            ttsManager.speak("减")
        }
    }

    private fun updateCount() {
        tvCount.text = viewModel.count.toString()
    }

    override fun onResume() {
        super.onResume()
        // 在Fragment可见时确保能接收到按键事件
        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelResetTimer()
    }
} 