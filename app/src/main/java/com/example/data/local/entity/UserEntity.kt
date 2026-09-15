package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val isVerified: Boolean = false,
    val otpCode: String? = null,
    val otpExpiry: Long = 0L,
    val isPremium: Boolean = false,
    val subscriptionPlan: String = "FREE", // "FREE", "MONTHLY", "QUARTERLY", "SEMI_ANNUAL", "ANNUAL"
    val subscriptionStartDate: Long = 0L,
    val subscriptionExpiryDate: Long = 0L,
    val freeAnalysisCount: Int = 0,
    val lastAnalysisDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
