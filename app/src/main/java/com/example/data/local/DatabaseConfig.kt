package com.example.data.local

/**
 * NEXA AI Centralized API Configuration Layer
 * 
 * Android application communicates exclusively via HTTPS REST API with the PHP Backend.
 * Direct database credentials or secrets MUST NOT be present in the Android codebase.
 */
object DatabaseConfig {
    
    // PHP Backend Base REST API Endpoint URL
    const val BASE_URL: String = "https://erixstudio.shop/api/"

    // Room SQLite Cache File Name
    const val ROOM_DB_NAME: String = "nexa_ai_trading_cache.db"

    fun getConnectionSummary(): String {
        return "REST API Backend: $BASE_URL"
    }
}
