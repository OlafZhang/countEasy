package org.olafzhang.counteasy.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Item
import org.olafzhang.counteasy.utils.NumberFormatter

class ItemAdapter(
    private val items: List<Item>,
    private val unit: String,
    private val decimalPlaces: Int,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    // 添加长按监听器支持
    private var onItemLongClickListener: ((Int) -> Boolean)? = null
    
    fun setOnItemLongClickListener(listener: (Int) -> Boolean) {
        onItemLongClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvItemIndex: TextView = itemView.findViewById(R.id.tvItemIndex)
        private val tvItemWeight: TextView = itemView.findViewById(R.id.tvItemWeight)

        fun bind(item: Item, position: Int) {
            // 显示序号（从1开始）
            tvItemIndex.text = "${position + 1}."
            
            // 格式化重量并显示
            val formattedWeight = NumberFormatter.formatNumber(item.weight, decimalPlaces)
            tvItemWeight.text = "$formattedWeight$unit"
            
            // 设置点击事件
            itemView.setOnClickListener {
                onItemClick(position)
            }
            
            // 设置长按事件
            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(position) ?: false
            }
        }
    }
} 