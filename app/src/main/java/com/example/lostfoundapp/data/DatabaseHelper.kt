package com.example.lostfoundapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "lost_found_database.db"


        private const val DATABASE_VERSION = 3

        private const val TABLE_ITEMS = "lost_found_items"

        private const val COL_ID = "id"
        private const val COL_POST_TYPE = "postType"
        private const val COL_CONTACT_NAME = "contactName"
        private const val COL_PHONE = "phone"
        private const val COL_DESCRIPTION = "description"
        private const val COL_CATEGORY = "category"
        private const val COL_DATE_TIME = "dateTime"
        private const val COL_LOCATION = "location"
        private const val COL_IMAGE_URI = "imageUri"



        private const val COL_LATITUDE = "latitude"
        private const val COL_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POST_TYPE TEXT NOT NULL,
                $COL_CONTACT_NAME TEXT NOT NULL,
                $COL_PHONE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_DATE_TIME TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_IMAGE_URI TEXT NOT NULL,
                $COL_LATITUDE REAL NOT NULL DEFAULT 0.0,
                $COL_LONGITUDE REAL NOT NULL DEFAULT 0.0
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        onCreate(db)
    }

    fun insertItem(item: LostFoundItem): Long {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_POST_TYPE, item.postType)
            put(COL_CONTACT_NAME, item.contactName)
            put(COL_PHONE, item.phone)
            put(COL_DESCRIPTION, item.description)
            put(COL_CATEGORY, item.category)
            put(COL_DATE_TIME, item.dateTime)
            put(COL_LOCATION, item.location)
            put(COL_IMAGE_URI, item.imageUri)
            put(COL_LATITUDE, item.latitude)
            put(COL_LONGITUDE, item.longitude)
        }

        return db.insert(TABLE_ITEMS, null, values)
    }

    fun getAllItems(): List<LostFoundItem> {
        val items = mutableListOf<LostFoundItem>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "$COL_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }

        return items
    }

    fun getItemById(id: Int): LostFoundItem? {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_ITEMS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToItem(it)
            }
        }

        return null
    }

    fun deleteItem(id: Int): Int {
        val db = writableDatabase

        return db.delete(
            TABLE_ITEMS,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun searchAndFilterItems(keyword: String, category: String): List<LostFoundItem> {
        val items = mutableListOf<LostFoundItem>()
        val db = readableDatabase

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (keyword.isNotBlank()) {
            selectionParts.add(
                "($COL_DESCRIPTION LIKE ? OR $COL_CONTACT_NAME LIKE ? OR $COL_LOCATION LIKE ? OR $COL_POST_TYPE LIKE ?)"
            )

            val searchKeyword = "%$keyword%"
            selectionArgs.add(searchKeyword)
            selectionArgs.add(searchKeyword)
            selectionArgs.add(searchKeyword)
            selectionArgs.add(searchKeyword)
        }

        if (category != "All") {
            selectionParts.add("$COL_CATEGORY = ?")
            selectionArgs.add(category)
        }

        val selection = if (selectionParts.isEmpty()) {
            null
        } else {
            selectionParts.joinToString(" AND ")
        }

        val cursor = db.query(
            TABLE_ITEMS,
            null,
            selection,
            if (selectionArgs.isEmpty()) null else selectionArgs.toTypedArray(),
            null,
            null,
            "$COL_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                items.add(cursorToItem(it))
            }
        }

        return items
    }

    private fun cursorToItem(cursor: Cursor): LostFoundItem {
        return LostFoundItem(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
            postType = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_TYPE)),
            contactName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_NAME)),
            phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE_TIME)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
            imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_URI)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE))
        )
    }
}