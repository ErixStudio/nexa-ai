package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planName: String,
    val amountToman: Int,
    val paymentStatus: String, // "COMPLETED", "PENDING", "FAILED"
    val gateway: String = "ZarinPal",
    val transactionRef: String?,
    val createdAt: Long = System.currentTimeMillis()
)
