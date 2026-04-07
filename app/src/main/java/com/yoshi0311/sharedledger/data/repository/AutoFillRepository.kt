package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.db.dao.PendingNotificationDao
import com.yoshi0311.sharedledger.data.db.entity.PendingNotificationEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.service.autofill.NotificationParserRegistry
import com.yoshi0311.sharedledger.service.autofill.SmsReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoFillRepository @Inject constructor(
    private val dao: PendingNotificationDao,
    private val transactionRepo: TransactionRepository,
    private val authRepo: AuthRepository,
    private val smsReader: SmsReader
) {
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getPendingPush(): Flow<List<PendingNotificationEntity>> = dao.getPendingBySource("push")
    fun getPendingSms(): Flow<List<PendingNotificationEntity>> = dao.getPendingBySource("sms")
    fun getPendingCount(): Flow<Int> = dao.getPendingCount()

    /** 푸시 알림 수신 시 호출 — 파싱 후 DB 삽입 (중복은 IGNORE) */
    suspend fun insertPush(packageName: String, title: String?, body: String, postTimeMs: Long) {
        val hash = dedupHash("push", packageName, body, postTimeMs)
        val parsed = NotificationParserRegistry.parse(packageName, title, body)
        dao.insert(
            PendingNotificationEntity(
                source            = "push",
                packageName       = packageName,
                title             = title,
                body              = body,
                parsedAmount      = parsed.amount,
                parsedType        = parsed.type,
                parsedDate        = parsed.date ?: Date(postTimeMs),
                parsedDescription = parsed.description,
                dedupHash         = hash
            )
        )
    }

    /** 문자메시지 탭 진입 시 호출 — SMS 읽기 후 신규 항목만 DB 삽입 */
    suspend fun loadSmsItems() {
        val smsList = smsReader.readFinancialSms()
        for (sms in smsList) {
            // SMS는 고유 _id 기반 해시 → 중복 없음
            val hash = md5("sms|${sms.id}|${sms.body.take(80)}")
            val parsed = NotificationParserRegistry.parseForSms(sms.body)
            dao.insert(
                PendingNotificationEntity(
                    source            = "sms",
                    packageName       = sms.address,
                    body              = sms.body,
                    parsedAmount      = parsed.amount,
                    parsedType        = parsed.type,
                    parsedDate        = Date(sms.dateMs),
                    parsedDescription = parsed.description,
                    dedupHash         = hash
                )
            )
        }
    }

    /** 승인 → TransactionEntity 생성 후 pending 상태 변경 */
    suspend fun approve(
        item: PendingNotificationEntity,
        amount: Long,
        type: String,
        date: Date,
        description: String,
        categoryId: Long? = null
    ) {
        val ledgerId = authRepo.ledgerId.firstOrNull() ?: 1L
        transactionRepo.insert(
            TransactionEntity(
                ledgerId    = ledgerId,
                categoryId  = categoryId,
                type        = type,
                amount      = amount,
                date        = date,
                time        = timeFmt.format(item.createdAt),
                description = description
            )
        )
        dao.updateStatus(item.id, "approved")
    }

    /** 취소 → rejected 상태로 변경 (목록에서 사라짐) */
    suspend fun reject(id: Long) {
        dao.updateStatus(id, "rejected")
    }

    // ── 해시 유틸 ────────────────────────────────────────────────────────────

    /** 5분 버킷으로 같은 앱·금액 알림 중복 제거 */
    private fun dedupHash(source: String, packageName: String, body: String, timeMs: Long): String {
        val bucket = timeMs / (5 * 60 * 1000)
        return md5("$source|$packageName|${body.take(80)}|$bucket")
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
