package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class CategoryRepository(private val dao: CategoryDao) {

    fun getByLedgerId(ledgerId: Long): Flow<List<CategoryEntity>> =
        dao.getByLedgerId(ledgerId)

    suspend fun insert(category: CategoryEntity): Long =
        dao.insert(category)

    suspend fun update(category: CategoryEntity) =
        dao.update(category)

    suspend fun softDelete(id: Long) =
        dao.softDelete(id, Date())
}
