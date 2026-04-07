package com.yoshi0311.sharedledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yoshi0311.sharedledger.data.db.converter.DateConverter
import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.dao.PendingNotificationDao
import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.PendingNotificationEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        PendingNotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun pendingNotificationDao(): PendingNotificationDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "shared_ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
