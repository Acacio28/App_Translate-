package com.example.app_translate.data.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.app_translate.viewmodel.DialogueMessage

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "translate_app.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HISTORY = "chat_history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_ORIGINAL = "original_text"
        private const val COLUMN_TRANSLATED = "translated_text"
        private const val COLUMN_IS_ME = "is_from_me"
        private const val COLUMN_SOURCE_LANG = "source_lang"
        private const val COLUMN_TARGET_LANG = "target_lang"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE $TABLE_HISTORY ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_ORIGINAL TEXT,"
                + "$COLUMN_TRANSLATED TEXT,"
                + "$COLUMN_IS_ME INTEGER,"
                + "$COLUMN_SOURCE_LANG TEXT,"
                + "$COLUMN_TARGET_LANG TEXT)")
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun saveMessage(msg: DialogueMessage) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ORIGINAL, msg.originalText)
            put(COLUMN_TRANSLATED, msg.translatedText)
            put(COLUMN_IS_ME, if (msg.isFromMe) 1 else 0)
            put(COLUMN_SOURCE_LANG, msg.sourceLangCode)
            put(COLUMN_TARGET_LANG, msg.targetLangCode)
        }
        db.insert(TABLE_HISTORY, null, values)
        db.close()
    }

    fun getAllMessages(): List<DialogueMessage> {
        val list = mutableListOf<DialogueMessage>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_HISTORY", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(DialogueMessage(
                    originalText = cursor.getString(1),
                    translatedText = cursor.getString(2),
                    isFromMe = cursor.getInt(3) == 1,
                    sourceLangCode = cursor.getString(4),
                    targetLangCode = cursor.getString(5)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    // Aumenta ida ne'e para rezolve error iha ViewModel
    fun clearHistory() {
        val db = this.writableDatabase
        db.delete(TABLE_HISTORY, null, null)
        db.close()
    }
}
