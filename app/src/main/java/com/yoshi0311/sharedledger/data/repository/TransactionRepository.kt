package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

class TransactionRepository(private val dao: TransactionDao) {

    fun getByLedgerId(ledgerId: Long): Flow<List<TransactionEntity>> =
        dao.getByLedgerId(ledgerId)

    fun getByLedgerIdAndMonth(ledgerId: Long, month: String): Flow<List<TransactionEntity>> =
        dao.getByLedgerIdAndMonth(ledgerId, month)

    suspend fun getById(id: Long): TransactionEntity? =
        dao.getById(id).firstOrNull()

    fun getTotalIncome(ledgerId: Long): Flow<Long> =
        dao.getTotalIncome(ledgerId)

    fun getTotalExpense(ledgerId: Long): Flow<Long> =
        dao.getTotalExpense(ledgerId)

    fun getTotalIncomeByMonth(ledgerId: Long, month: String): Flow<Long> =
        dao.getTotalIncomeByMonth(ledgerId, month)

    fun getTotalExpenseByMonth(ledgerId: Long, month: String): Flow<Long> =
        dao.getTotalExpenseByMonth(ledgerId, month)

    suspend fun insert(transaction: TransactionEntity): Long =
        dao.insert(transaction.copy(syncStatus = "pending"))

    suspend fun update(transaction: TransactionEntity) =
        dao.update(transaction.copy(syncStatus = "pending"))

    suspend fun softDelete(id: Long) =
        dao.softDelete(id, Date())
}
