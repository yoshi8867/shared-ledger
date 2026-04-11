package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class CategoryRepository(private val dao: CategoryDao) {

    fun getByLedgerId(ledgerId: Long): Flow<List<CategoryEntity>> =
        dao.getByLedgerId(ledgerId)

    fun getByLedgerIdAndType(ledgerId: Long, type: String): Flow<List<CategoryEntity>> =
        dao.getByLedgerIdAndType(ledgerId, type)

    suspend fun insert(category: CategoryEntity): Long =
        dao.insert(category.copy(syncStatus = "pending"))

    suspend fun update(category: CategoryEntity) =
        dao.update(category.copy(syncStatus = "pending"))

    suspend fun getByIdSync(id: Long): CategoryEntity? =
        dao.getByIdSync(id)

    suspend fun softDelete(id: Long) =
        dao.softDelete(id, Date())
}
