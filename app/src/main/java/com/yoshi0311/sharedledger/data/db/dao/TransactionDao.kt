package com.yoshi0311.sharedledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    fun getById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 ORDER BY date DESC, time DESC")
    fun getByLedgerId(ledgerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND date = :date AND is_deleted = 0 ORDER BY time DESC")
    fun getByLedgerIdAndDate(ledgerId: Long, date: Date): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND synced_at IS NULL")
    fun getUnsyncedTransactions(ledgerId: Long): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Date)

    @Query("SELECT COUNT(*) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'income'")
    fun getTotalIncome(ledgerId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'expense'")
    fun getTotalExpense(ledgerId: Long): Flow<Long>

    @Query("DELETE FROM transactions WHERE is_deleted = 1 AND deleted_at < :beforeDate")
    suspend fun purgeDeletedTransactions(beforeDate: Date)
}
