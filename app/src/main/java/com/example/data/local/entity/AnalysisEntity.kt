package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val symbol: String,
    val timeframe: String,
    val imageUri: String?,
    val signal: String, // "LONG" or "SHORT"
    val confidence: Int, // e.g. 88
    val reasonsJson: String, // Pipe or JSON joined reasons
    val entryPrice: String,
    val stopLoss: String,
    val takeProfit: String,
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
    val createdAt: Long = System.currentTimeMillis()
)
