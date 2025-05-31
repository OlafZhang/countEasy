package org.olafzhang.counteasy.data

data class Task(
    val id: Long = 0,
    val name: String,
    val unit: String,
    val decimalPlaces: Int,
    val status: Int = STATUS_IN_PROGRESS
) {
    companion object {
        const val STATUS_IN_PROGRESS = 0
        const val STATUS_COMPLETED = 1
    }
}

data class Item(
    val id: Long = 0,
    val taskId: Long,
    val weight: Double,
    val item_index: Int
)

data class TaskSummary(
    val task: Task,
    val totalWeight: Double,
    val itemCount: Int
) 