package org.olafzhang.counteasy.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDao(context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    fun insertTask(task: Task): Long {
        val values = ContentValues().apply {
            put("name", task.name)
            put("unit", task.unit)
            put("decimal_places", task.decimalPlaces)
            put("status", task.status)
        }
        return db.insert("tasks", null, values)
    }

    fun updateTask(task: Task) {
        val values = ContentValues().apply {
            put("name", task.name)
            put("unit", task.unit)
            put("decimal_places", task.decimalPlaces)
            put("status", task.status)
        }
        db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(taskId: Long) {
        db.delete("tasks", "id = ?", arrayOf(taskId.toString()))
        db.delete("items", "task_id = ?", arrayOf(taskId.toString()))
    }

    fun clearTaskItems(taskId: Long) {
        db.delete("items", "task_id = ?", arrayOf(taskId.toString()))
    }

    fun getTask(taskId: Long): Task? {
        val cursor = db.query(
            "tasks",
            null,
            "id = ?",
            arrayOf(taskId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            Task(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                decimalPlaces = cursor.getInt(cursor.getColumnIndexOrThrow("decimal_places")),
                status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
            )
        } else null
    }

    fun getAllTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.query(
            "tasks",
            null,
            null,
            null,
            null,
            null,
            "id DESC"
        )

        while (cursor.moveToNext()) {
            tasks.add(
                Task(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                    decimalPlaces = cursor.getInt(cursor.getColumnIndexOrThrow("decimal_places")),
                    status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                )
            )
        }
        cursor.close()
        return tasks
    }

    fun addItem(item: Item): Long {
        val values = ContentValues().apply {
            put("task_id", item.taskId)
            put("weight", item.weight)
            put("item_index", item.item_index)
        }
        return db.insert("items", null, values)
    }

    fun updateItem(item: Item) {
        val values = ContentValues().apply {
            put("weight", item.weight)
            put("item_index", item.item_index)
        }
        db.update("items", values, "id = ?", arrayOf(item.id.toString()))
    }

    fun deleteItem(itemId: Long) {
        db.delete("items", "id = ?", arrayOf(itemId.toString()))
    }

    fun updateTaskItemWeight(taskId: Long, index: Int, weight: Double) {
        val values = ContentValues().apply {
            put("weight", weight)
        }
        db.update(
            "items",
            values,
            "task_id = ? AND item_index = ?",
            arrayOf(taskId.toString(), index.toString())
        )
    }

    fun getItemsForTask(taskId: Long): List<Item> {
        val items = mutableListOf<Item>()
        val cursor = db.query(
            "items",
            null,
            "task_id = ?",
            arrayOf(taskId.toString()),
            null,
            null,
            "item_index ASC"
        )

        while (cursor.moveToNext()) {
            items.add(
                Item(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    taskId = cursor.getLong(cursor.getColumnIndexOrThrow("task_id")),
                    weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight")),
                    item_index = cursor.getInt(cursor.getColumnIndexOrThrow("item_index"))
                )
            )
        }
        cursor.close()
        return items
    }

    fun isTaskNameExists(name: String): Boolean {
        val cursor = db.query(
            "tasks",
            null,
            "name = ?",
            arrayOf(name),
            null,
            null,
            null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getTaskTotalWeight(taskId: Long): Double {
        val cursor = db.rawQuery(
            "SELECT SUM(weight) as total_weight FROM items WHERE task_id = ?",
            arrayOf(taskId.toString())
        )
        
        val totalWeight = if (cursor.moveToFirst()) {
            cursor.getDouble(cursor.getColumnIndexOrThrow("total_weight"))
        } else {
            0.0
        }
        cursor.close()
        return totalWeight
    }
    
    fun getTaskItemCount(taskId: Long): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) as item_count FROM items WHERE task_id = ?",
            arrayOf(taskId.toString())
        )
        
        val itemCount = if (cursor.moveToFirst()) {
            cursor.getInt(cursor.getColumnIndexOrThrow("item_count"))
        } else {
            0
        }
        cursor.close()
        return itemCount
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    decimal_places INTEGER NOT NULL,
                    status INTEGER NOT NULL DEFAULT 0
                )
            """)

            db.execSQL("""
                CREATE TABLE items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    weight REAL NOT NULL,
                    item_index INTEGER NOT NULL,
                    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
                )
            """)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS items")
            db.execSQL("DROP TABLE IF EXISTS tasks")
            onCreate(db)
        }

        companion object {
            private const val DATABASE_NAME = "counteasy.db"
            private const val DATABASE_VERSION = 1
        }
    }
} 