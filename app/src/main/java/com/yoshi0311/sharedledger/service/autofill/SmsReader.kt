package com.yoshi0311.sharedledger.service.autofill

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class RawSms(
        val id: Long,
        val address: String,    // 발신 번호
        val body: String,
        val dateMs: Long
    )

    /**
     * 최근 [sinceMs] 이후의 SMS 중 금액 패턴(N원)이 포함된 것만 반환.
     * READ_SMS 권한이 없으면 빈 리스트 반환.
     */
    fun readFinancialSms(
        sinceMs: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    ): List<RawSms> {
        val result = mutableListOf<RawSms>()
        runCatching {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                "date > ?",
                arrayOf(sinceMs.toString()),
                "date DESC"
            ) ?: return emptyList()

            cursor.use { c ->
                val idCol   = c.getColumnIndexOrThrow("_id")
                val addrCol = c.getColumnIndexOrThrow("address")
                val bodyCol = c.getColumnIndexOrThrow("body")
                val dateCol = c.getColumnIndexOrThrow("date")

                while (c.moveToNext()) {
                    val body = c.getString(bodyCol) ?: continue
                    if (AMOUNT_REGEX.containsMatchIn(body)) {
                        result += RawSms(
                            id      = c.getLong(idCol),
                            address = c.getString(addrCol) ?: "",
                            body    = body,
                            dateMs  = c.getLong(dateCol)
                        )
                    }
                }
            }
        }
        return result
    }

    companion object {
        private val AMOUNT_REGEX = Regex("\\d[\\d,]*원")
    }
}
