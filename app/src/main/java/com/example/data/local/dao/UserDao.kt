package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET freeAnalysisCount = freeAnalysisCount + 1, lastAnalysisDate = :dateStr WHERE id = :userId")
    suspend fun incrementFreeAnalysisCount(userId: String, dateStr: String)

    @Query("UPDATE users SET freeAnalysisCount = :count, lastAnalysisDate = :dateStr WHERE id = :userId")
    suspend fun updateDailyFreeCount(userId: String, count: Int, dateStr: String)

    @Query("UPDATE users SET isPremium = :isPremium, subscriptionPlan = :plan, subscriptionStartDate = :startDate, subscriptionExpiryDate = :expiryDate WHERE id = :userId")
    suspend fun updateSubscription(userId: String, isPremium: Boolean, plan: String, startDate: Long, expiryDate: Long)
}
