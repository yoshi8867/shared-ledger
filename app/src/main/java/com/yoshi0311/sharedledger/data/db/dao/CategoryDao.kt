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

    @Query("SELECT * FROM categories WHERE ledger_id = :ledgerId AND is_deleted = 0 AND synced_at IS NULL")
    fun getUnsyncedCategories(ledgerId: Long): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET is_deleted = 1, deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Date)

    @Query("DELETE FROM categories WHERE is_deleted = 1 AND deleted_at < :beforeDate")
    suspend fun purgeDeletedCategories(beforeDate: Date)
}
