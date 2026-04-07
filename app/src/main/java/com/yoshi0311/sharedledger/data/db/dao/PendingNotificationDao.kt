package com.yoshi0311.sharedledger.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yoshi0311.sharedledger.data.db.entity.PendingNotificationEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PendingNotificationDao {

    /** dedup_hash 충돌 시 무시 — 중복 삽입 방지 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PendingNotificationEntity): Long

    @Query("SELECT * FROM pending_notifications WHERE status = 'pending' AND source = :source ORDER BY created_at DESC")
    fun getPendingBySource(source: String): Flow<List<PendingNotificationEntity>>

    @Query("SELECT COUNT(*) FROM pending_notifications WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE pending_notifications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    /** 처리 완료(approved/rejected)된 오래된 항목 삭제 */
    @Query("DELETE FROM pending_notifications WHERE status != 'pending' AND created_at < :before")
    suspend fun purgeOld(before: Date)
}
