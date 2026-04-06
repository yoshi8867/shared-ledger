package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.network.api.PushCategoryDto
import com.yoshi0311.sharedledger.network.api.PushRequest
import com.yoshi0311.sharedledger.network.api.PushTransactionDto
import com.yoshi0311.sharedledger.network.api.SyncApi
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val syncApi: SyncApi,
    private val authDataStore: AuthDataStore
) {
    // ── 날짜 파싱/포맷 ─────────────────────────────────────────────────────────

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFmtShort = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseIso(s: String): Date =
        runCatching { isoFmt.parse(s)!! }.getOrElse {
            runCatching { isoFmtShort.parse(s)!! }.getOrElse { Date(0) }
        }

    private fun parseDate(s: String): Date =
        runCatching { dateFmt.parse(s)!! }.getOrElse { Date() }

    private fun formatIso(d: Date): String = isoFmt.format(d)
    private fun formatDate(d: Date): String = dateFmt.format(d)

    // ── 공개 API ───────────────────────────────────────────────────────────────

    /**
     * 전체 동기화: pending 항목을 서버에 push한 뒤, 서버의 변경분을 pull 한다.
     * @return 성공 시 Result.success(Unit), 실패 시 Result.failure(exception)
     */
    suspend fun sync(ledgerId: Long): Result<Unit> = runCatching {
        pushPending(ledgerId)
        pullDelta(ledgerId)
    }

    // ── Push: 로컬 pending → 서버 ──────────────────────────────────────────────

    private suspend fun pushPending(ledgerId: Long) {
        val pendingCats = categoryDao.getPendingCategories(ledgerId)
        val pendingTxs  = transactionDao.getPendingTransactions(ledgerId)
        if (pendingCats.isEmpty() && pendingTxs.isEmpty()) return

        val request = PushRequest(
            categories = pendingCats.map { cat ->
                PushCategoryDto(
                    clientId     = cat.id,
                    serverId     = cat.serverId,
                    categoryName = cat.name,
                    color        = cat.color,
                    isDeleted    = cat.isDeleted,
                    updatedAt    = formatIso(cat.updatedAt)
                )
            },
            transactions = pendingTxs.map { tx ->
                // 로컬 categoryId → 해당 카테고리의 serverId 조회
                val catServerId = tx.categoryId?.let { localCatId ->
                    categoryDao.getByIdSync(localCatId)?.serverId
                }
                PushTransactionDto(
                    clientId         = tx.id,
                    serverId         = tx.serverId,
                    amount           = tx.amount,
                    type             = tx.type,
                    categoryServerId = catServerId,
                    description      = tx.description,
                    transactionDate  = formatDate(tx.date),
                    transactionTime  = tx.time.ifEmpty { null },
                    isDeleted        = tx.isDeleted,
                    updatedAt        = formatIso(tx.updatedAt)
                )
            }
        )

        val response = syncApi.push(request)

        for (r in response.categories) {
            categoryDao.markSynced(r.clientId, r.serverId, parseIso(r.serverUpdatedAt))
        }
        for (r in response.transactions) {
            transactionDao.markSynced(r.clientId, r.serverId, parseIso(r.serverUpdatedAt))
        }
    }

    // ── Pull: 서버 delta → 로컬 DB ────────────────────────────────────────────

    private suspend fun pullDelta(ledgerId: Long) {
        val since = authDataStore.lastSyncedAt.firstOrNull() ?: "1970-01-01T00:00:00.000Z"
        val delta = syncApi.getDelta(since)

        // 카테고리를 먼저 적용 (트랜잭션이 category_id에 의존)
        for (cat in delta.categories) {
            val serverDate = parseIso(cat.updatedAt)
            val existing   = categoryDao.getByServerId(cat.categoryId)

            if (cat.isDeleted) {
                existing?.let { categoryDao.softDeleteFromServer(it.id, serverDate) }
                continue
            }

            if (existing == null) {
                // 서버에만 있는 항목 → 새로 삽입
                categoryDao.insert(
                    CategoryEntity(
                        ledgerId   = ledgerId,
                        name       = cat.categoryName,
                        color      = cat.color ?: "#808080",
                        serverId   = cat.categoryId,
                        syncStatus = "synced",
                        syncedAt   = serverDate,
                        updatedAt  = serverDate
                    )
                )
            } else if (existing.syncStatus != "pending" || serverDate > existing.updatedAt) {
                // 충돌 해결: 서버가 더 최신이거나 로컬이 pending 아님 → 서버 우선
                categoryDao.update(
                    existing.copy(
                        name       = cat.categoryName,
                        color      = cat.color ?: existing.color,
                        serverId   = cat.categoryId,
                        syncStatus = "synced",
                        syncedAt   = serverDate,
                        updatedAt  = serverDate
                    )
                )
            }
            // else: 로컬 pending이 더 최신 → 그대로 유지 (다음 push에서 서버에 반영됨)
        }

        // 거래 내역 적용
        for (tx in delta.transactions) {
            val serverDate = parseIso(tx.updatedAt)
            val existing   = transactionDao.getByServerId(tx.transactionId)

            if (tx.isDeleted) {
                existing?.let { transactionDao.softDeleteFromServer(it.id, serverDate) }
                continue
            }

            // 서버의 categoryId(serverId) → 로컬 DB의 category row id
            val localCatId = tx.categoryId?.let { catServerId ->
                categoryDao.getByServerId(catServerId)?.id
            }

            if (existing == null) {
                transactionDao.insert(
                    TransactionEntity(
                        ledgerId    = ledgerId,
                        categoryId  = localCatId,
                        type        = tx.type,
                        amount      = tx.amount.toLong(),
                        date        = parseDate(tx.transactionDate),
                        time        = tx.transactionTime ?: "",
                        description = tx.description ?: "",
                        serverId    = tx.transactionId,
                        syncStatus  = "synced",
                        syncedAt    = serverDate,
                        updatedAt   = serverDate
                    )
                )
            } else if (existing.syncStatus != "pending" || serverDate > existing.updatedAt) {
                transactionDao.update(
                    existing.copy(
                        categoryId  = localCatId,
                        type        = tx.type,
                        amount      = tx.amount.toLong(),
                        date        = parseDate(tx.transactionDate),
                        time        = tx.transactionTime ?: existing.time,
                        description = tx.description ?: existing.description,
                        serverId    = tx.transactionId,
                        syncStatus  = "synced",
                        syncedAt    = serverDate,
                        updatedAt   = serverDate
                    )
                )
            }
        }

        authDataStore.saveLastSyncedAt(delta.serverTime)
    }
}
