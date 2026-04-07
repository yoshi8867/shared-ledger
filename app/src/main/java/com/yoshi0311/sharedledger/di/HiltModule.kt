package com.yoshi0311.sharedledger.di

import android.content.Context
import com.yoshi0311.sharedledger.data.db.AppDatabase
import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.dao.PendingNotificationDao
import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao =
        db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao =
        db.categoryDao()

    @Provides
    fun providePendingNotificationDao(db: AppDatabase): PendingNotificationDao =
        db.pendingNotificationDao()

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository =
        TransactionRepository(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(dao: CategoryDao): CategoryRepository =
        CategoryRepository(dao)
}
