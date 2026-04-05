package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class TransactionRepository(private val dao: TransactionDao) {

    fun getByLedgerId(ledgerId: Long): Flow<List<TransactionEntity>> =
        dao.getByLedgerId(ledgerId)

    fun getTotalIncome(ledgerId: Long): Flow<Long> =
        dao.getTotalIncome(ledgerId)

    fun getTotalExpense(ledgerId: Long): Flow<Long> =
        dao.getTotalExpense(ledgerId)

    suspend fun insert(transaction: TransactionEntity): Long =
        dao.insert(transaction)

    suspend fun update(transaction: TransactionEntity) =
        dao.update(transaction)

    suspend fun softDelete(id: Long) =
        dao.softDelete(id, Date())
}
