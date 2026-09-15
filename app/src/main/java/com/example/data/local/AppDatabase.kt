package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AnalysisDao
import com.example.data.local.dao.PaymentDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AnalysisEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, AnalysisEntity::class, PaymentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseConfig.ROOM_DB_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
