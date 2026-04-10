package com.yoshi0311.sharedledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ledger_id")
    val ledgerId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String, // hex color code

    @ColumnInfo(name = "type")
    val type: String = "expense", // "income" | "expense"

    @ColumnInfo(name = "icon")
    val icon: String? = null,

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
