package com.yoshi0311.sharedledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ledger_id")
    val ledgerId: Long,

    @ColumnInfo(name = "category_id", index = true)
    val categoryId: Long? = null, // 선택사항 — null이면 구분 없음

    @ColumnInfo(name = "type")
    val type: String, // "income" or "expense"

    @ColumnInfo(name = "amount")
    val amount: Long, // in won (₩)

    @ColumnInfo(name = "date")
    val date: Date,

    @ColumnInfo(name = "time")
    val time: String, // HH:mm format

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Date? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date(),

    @ColumnInfo(name = "synced_at")
    val syncedAt: Date? = null,

    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending"
)
