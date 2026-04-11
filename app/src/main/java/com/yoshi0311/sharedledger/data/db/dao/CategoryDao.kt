package com.yoshi0311.sharedledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id AND is_deleted = 0")
    fun getById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND is_deleted = 0 ORDER BY name ASC")
    fun getByLedgerId(ledgerId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND type = :type AND is_deleted = 0 ORDER BY name ASC")
    fun getByLedgerIdAndType(ledgerId: Long, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND is_deleted = 0 AND synced_at IS NULL")
    fun getUnsyncedCategories(ledgerId: Long): Flow<List<CategoryEntity>>

    // ── 동기화용 쿼리 ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND sync_status = 'pending'")
    suspend fun getPendingCategories(ledgerId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE server_id = :serverId AND ledger_id = :ledgerId LIMIT 1")
    suspend fun getByServerId(serverId: Long, ledgerId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Long): CategoryEntity?

    /** push 성공 후 server_id · synced_at · sync_status 갱신 */
    @Query("UPDATE categories SET server_id = :serverId, sync_status = 'synced', synced_at = :syncedAt, updated_at = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: Long, serverId: Long, syncedAt: Date)

    /** 로컬 삭제 → sync_status = 'pending' 포함 */
    @Query("UPDATE categories SET is_deleted = 1, deleted_at = :deletedAt, sync_status = 'pending', updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Date)

    /** 서버 측 삭제 반영 → sync_status = 'synced' 유지 */
    @Query("UPDATE categories SET is_deleted = 1, deleted_at = :deletedAt, sync_status = 'synced', updated_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteFromServer(id: Long, deletedAt: Date)

    @Query("DELETE FROM categories WHERE is_deleted = 1 AND deleted_at < :beforeDate")
    suspend fun purgeDeletedCategories(beforeDate: Date)
}
