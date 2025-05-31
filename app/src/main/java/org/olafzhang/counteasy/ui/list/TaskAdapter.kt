package org.olafzhang.counteasy.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.olafzhang.counteasy.R
import org.olafzhang.counteasy.data.Task
import org.olafzhang.counteasy.data.TaskDao
import org.olafzhang.counteasy.utils.NumberFormatter

class TaskAdapter(
    private val taskDao: TaskDao,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.ViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvTaskSummary: TextView = itemView.findViewById(R.id.tvTaskSummary)

        fun bind(task: Task) {
            tvTaskName.text = task.name
            
            // 获取任务总重量和条目数
            val totalWeight = taskDao.getTaskTotalWeight(task.id)
            val itemCount = taskDao.getTaskItemCount(task.id)
            
            // 格式化总重量，使用任务设定的小数点位数
            val formattedWeight = NumberFormatter.formatNumber(totalWeight, task.decimalPlaces)
            
            // 显示总重量和条目数
            tvTaskSummary.text = "${formattedWeight}${task.unit}，${itemCount}个条目"

            itemView.setOnClickListener { onTaskClick(task) }
            itemView.setOnLongClickListener {
                onTaskLongClick(task)
                true
            }
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
} 