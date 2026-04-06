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

    // SQLite: date는 milliseconds(Long)로 저장됨 → /1000 후 strftime으로 YYYY-MM 추출
    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND strftime('%Y-%m', date/1000, 'unixepoch') = :month ORDER BY date DESC, time DESC")
    fun getByLedgerIdAndMonth(ledgerId: Long, month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND date = :date AND is_deleted = 0 ORDER BY time DESC")
    fun getByLedgerIdAndDate(ledgerId: Long, date: Date): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND synced_at IS NULL")
    fun getUnsyncedTransactions(ledgerId: Long): Flow<List<TransactionEntity>>

    // ── 동기화용 쿼리 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM transactions WHERE ledger_id = :ledgerId AND sync_status = 'pending'")
    suspend fun getPendingTransactions(ledgerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): TransactionEntity?

    /** push 성공 후 server_id · synced_at · sync_status 갱신 */
    @Query("UPDATE transactions SET server_id = :serverId, sync_status = 'synced', synced_at = :syncedAt, updated_at = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, serverId: Long, syncedAt: Date)

    /** 로컬 삭제 → sync_status = 'pending' 포함 */
    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = :deletedAt, sync_status = 'pending', updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Date)

    /** 서버 측 삭제 반영 → sync_status = 'synced' 유지 */
    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = :deletedAt, sync_status = 'synced', updated_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteFromServer(id: Long, deletedAt: Date)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'income' AND strftime('%Y-%m', date/1000, 'unixepoch') = :month")
    fun getTotalIncomeByMonth(ledgerId: Long, month: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'expense' AND strftime('%Y-%m', date/1000, 'unixepoch') = :month")
    fun getTotalExpenseByMonth(ledgerId: Long, month: String): Flow<Long>

    // 기존 COUNT 쿼리 (HomeViewModel에서 사용)
    @Query("SELECT COUNT(*) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'income'")
    fun getTotalIncome(ledgerId: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM transactions WHERE ledger_id = :ledgerId AND is_deleted = 0 AND type = 'expense'")
    fun getTotalExpense(ledgerId: Long): Flow<Long>

    @Query("DELETE FROM transactions WHERE is_deleted = 1 AND deleted_at < :beforeDate")
    suspend fun purgeDeletedTransactions(beforeDate: Date)
}
