package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analysis_history WHERE userId = :userId OR userId = :userEmail ORDER BY createdAt DESC")
    fun getAnalysisHistoryByUser(userId: String, userEmail: String): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analysis_history WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAnalysisHistoryByUser(userId: String): Flow<List<AnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("DELETE FROM analysis_history WHERE id = :id")
    suspend fun deleteAnalysisById(id: String)

    @Query("DELETE FROM analysis_history WHERE userId = :userId OR userId = :userEmail")
    suspend fun clearUserHistory(userId: String, userEmail: String = "")
}
