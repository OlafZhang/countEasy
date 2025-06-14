package org.olafzhang.counteasy.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import org.olafzhang.counteasy.R

class SettingsFragment : Fragment() {
    
    private lateinit var spinnerDefaultUnit: Spinner
    private lateinit var spinnerDecimalPlaces: Spinner
    private lateinit var rgKeyboardLayout: RadioGroup
    private lateinit var rbKeyboardLayoutAsc: RadioButton
    private lateinit var rbKeyboardLayoutDesc: RadioButton
    private lateinit var rgZeroPosition: RadioGroup
    private lateinit var rbZeroCenter: RadioButton
    private lateinit var rbZeroRight: RadioButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        
        initViews(root)
        loadSettings()
        setupListeners()
        
        return root
    }
    
    private fun initViews(view: View) {
        spinnerDefaultUnit = view.findViewById(R.id.spinnerDefaultUnit)
        spinnerDecimalPlaces = view.findViewById(R.id.spinnerDecimalPlaces)
        rgKeyboardLayout = view.findViewById(R.id.rgKeyboardLayout)
        rbKeyboardLayoutAsc = view.findViewById(R.id.rbKeyboardLayoutAsc)
        rbKeyboardLayoutDesc = view.findViewById(R.id.rbKeyboardLayoutDesc)
        rgZeroPosition = view.findViewById(R.id.rgZeroPosition)
        rbZeroCenter = view.findViewById(R.id.rbZeroCenter)
        rbZeroRight = view.findViewById(R.id.rbZeroRight)
        
        // 设置单位选项
        val units = arrayOf("千克", "克", "吨")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDefaultUnit.adapter = unitAdapter
        
        // 设置小数点位数选项
        val decimalPlaces = arrayOf("0", "1", "2", "3", "4", "5")
        val decimalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, decimalPlaces)
        decimalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDecimalPlaces.adapter = decimalAdapter
    }
    
    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        // 加载默认单位设置
        val defaultUnitPosition = prefs.getInt("default_unit", 0) // 0 = 千克
        spinnerDefaultUnit.setSelection(defaultUnitPosition)
        
        // 加载默认小数位设置
        val defaultDecimalPosition = prefs.getInt("default_decimal", 2) // 默认2位小数
        spinnerDecimalPlaces.setSelection(defaultDecimalPosition)
        
        // 加载键盘布局设置，默认为倒序
        val isKeyboardLayoutAsc = prefs.getBoolean("keyboard_layout_asc", false) // 默认倒序
        if (isKeyboardLayoutAsc) {
            rbKeyboardLayoutAsc.isChecked = true
        } else {
            rbKeyboardLayoutDesc.isChecked = true
        }
        
        // 加载0的位置设置，默认为0在中间
        val isZeroCenter = prefs.getBoolean("zero_center", true) // 默认0在中间
        if (isZeroCenter) {
            rbZeroCenter.isChecked = true
        } else {
            rbZeroRight.isChecked = true
        }
    }
    
    private fun setupListeners() {
        // 为Spinner添加监听器
        spinnerDefaultUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerDecimalPlaces.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                saveSettings()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // 为RadioGroup添加监听器
        rgKeyboardLayout.setOnCheckedChangeListener { _, _ -> saveSettings() }
        rgZeroPosition.setOnCheckedChangeListener { _, _ -> saveSettings() }
    }
    
    private fun saveSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 保存默认单位设置
        editor.putInt("default_unit", spinnerDefaultUnit.selectedItemPosition)
        
        // 保存默认小数位设置
        editor.putInt("default_decimal", spinnerDecimalPlaces.selectedItemPosition)
        
        // 保存键盘布局设置
        editor.putBoolean("keyboard_layout_asc", rbKeyboardLayoutAsc.isChecked)
        
        // 保存0的位置设置
        editor.putBoolean("zero_center", rbZeroCenter.isChecked)
        
        editor.apply()
    }
} 