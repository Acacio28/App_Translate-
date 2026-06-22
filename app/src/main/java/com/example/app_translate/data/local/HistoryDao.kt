package com.example.app_translate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_table WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_table WHERE sourceText = :source AND targetText = :target AND sourceLang = :sLang AND targetLang = :tLang LIMIT 1")
    suspend fun findHistory(source: String, target: String, sLang: String, tLang: String): HistoryEntity?

    @Query("SELECT * FROM history_table WHERE id = :id LIMIT 1")
    suspend fun findHistoryById(id: Int): HistoryEntity?

    @Query("UPDATE history_table SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history_table")
    suspend fun clearAll()
}