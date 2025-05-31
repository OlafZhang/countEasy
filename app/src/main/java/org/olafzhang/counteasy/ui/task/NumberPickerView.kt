package org.olafzhang.counteasy.ui.task

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import org.olafzhang.counteasy.R

class NumberPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val itemAdapter = ItemAdapter()
    private var onItemSelectedListener: ((Int) -> Unit)? = null
    private var currentPosition = 0

    init {
        // 使用LinenearLayoutManager，但不进行循环滚动
        layoutManager = LinearLayoutManager(context)
        adapter = itemAdapter
        setHasFixedSize(true)
        
        // 使用SnapHelper让滚动总是停在中间位置
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(this)
        
        // 设置装饰器以调整间距
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.top = 8
                outRect.bottom = 8
            }
        })
        
        // 添加滚动监听器，更新选中项
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    // 获取中间可见的项
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    
                    // 计算中间位置
                    val middle = (firstVisible + lastVisible) / 2
                    if (middle >= 0 && middle < itemAdapter.itemCount) {
                        setCurrentPosition(middle)
                        onItemSelectedListener?.invoke(middle)
                    }
                }
            }
        })
    }

    fun setOnItemSelectedListener(listener: (Int) -> Unit) {
        onItemSelectedListener = listener
    }

    fun setItems(items: List<Int>) {
        itemAdapter.submitList(items)
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
        itemAdapter.updateSelectedPosition(position)
    }

    override fun scrollToPosition(position: Int) {
        super.smoothScrollToPosition(position)
    }

    private inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
        private var items: List<Int> = emptyList()
        private var selectedPosition = -1

        fun submitList(newItems: List<Int>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun updateSelectedPosition(position: Int) {
            val oldPosition = selectedPosition
            selectedPosition = position
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            if (position != -1) notifyItemChanged(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_number_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, position)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.tvWeight)

            fun bind(item: Int, position: Int) {
                textView.text = "条目 #${item + 1}"
                itemView.isSelected = position == selectedPosition
                itemView.setOnClickListener {
                    onItemSelectedListener?.invoke(position)
                    setCurrentPosition(position)
                }
            }
        }
    }
} 