package org.olafzhang.counteasy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "counteasy.db"
        private const val DATABASE_VERSION = 1

        // 任务列表表
        const val TABLE_TASKS = "tasks"
        const val COLUMN_TASK_ID = "id"
        const val COLUMN_TASK_NAME = "name"
        const val COLUMN_TASK_UNIT = "unit"
        const val COLUMN_TASK_DECIMAL_PLACES = "decimal_places"

        // 条目表
        const val TABLE_ITEMS = "items"
        const val COLUMN_ITEM_ID = "id"
        const val COLUMN_ITEM_TASK_ID = "task_id"
        const val COLUMN_ITEM_WEIGHT = "weight"
        const val COLUMN_ITEM_INDEX = "item_index"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建任务列表表
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TASK_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_TASK_UNIT TEXT NOT NULL,
                $COLUMN_TASK_DECIMAL_PLACES INTEGER NOT NULL
            )
        """.trimIndent()

        // 创建条目表
        val createItemsTable = """
            CREATE TABLE $TABLE_ITEMS (
                $COLUMN_ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ITEM_TASK_ID INTEGER NOT NULL,
                $COLUMN_ITEM_WEIGHT REAL NOT NULL,
                $COLUMN_ITEM_INDEX INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ITEM_TASK_ID) REFERENCES $TABLE_TASKS($COLUMN_TASK_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createTasksTable)
        db.execSQL(createItemsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }
} 