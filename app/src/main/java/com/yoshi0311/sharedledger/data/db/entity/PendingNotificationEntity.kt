package com.yoshi0311.sharedledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "pending_notifications",
    indices = [Index(value = ["dedup_hash"], unique = true)]
)
data class PendingNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** "push" | "sms" */
    @ColumnInfo(name = "source") val source: String,

    /** push: 앱 패키지명 / sms: 발신번호 */
    @ColumnInfo(name = "package_name") val packageName: String,

    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "body") val body: String,

    @ColumnInfo(name = "parsed_amount") val parsedAmount: Long? = null,
    @ColumnInfo(name = "parsed_type") val parsedType: String? = null,   // "income" | "expense"
    @ColumnInfo(name = "parsed_date") val parsedDate: Date? = null,
    @ColumnInfo(name = "parsed_description") val parsedDescription: String? = null,

    /** 중복 방지 해시 (UNIQUE) */
    @ColumnInfo(name = "dedup_hash") val dedupHash: String,

    /** "pending" | "approved" | "rejected" */
    @ColumnInfo(name = "status") val status: String = "pending",

    @ColumnInfo(name = "created_at") val createdAt: Date = Date()
)
