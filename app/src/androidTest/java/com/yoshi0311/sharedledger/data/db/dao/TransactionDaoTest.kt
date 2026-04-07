package com.yoshi0311.sharedledger.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yoshi0311.sharedledger.data.db.AppDatabase
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private fun dateForMonth(yyyyMm: String, day: Int = 15): Date {
        val (year, mon) = yyyyMm.split("-").map { it.toInt() }
        return Calendar.getInstance().apply {
            set(year, mon - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun tx(
        ledgerId: Long = 1L,
        type: String = "expense",
        amount: Long = 10_000L,
        month: String = "2026-04",
        syncStatus: String = "pending",
        serverId: Long? = null
    ) = TransactionEntity(
        ledgerId = ledgerId,
        type = type,
        amount = amount,
        date = dateForMonth(month),
        time = "12:00",
        syncStatus = syncStatus,
        serverId = serverId
    )

    // ── 기본 CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(tx(amount = 5_000L))
        val result = dao.getById(id).first()
        assertNotNull(result)
        assertEquals(5_000L, result!!.amount)
    }

    @Test
    fun updateChangesAmount() = runTest {
        val id = dao.insert(tx(amount = 1_000L))
        val original = dao.getById(id).first()!!
        dao.update(original.copy(amount = 9_999L))

        val updated = dao.getById(id).first()
        assertEquals(9_999L, updated!!.amount)
    }

    // ── 월별 필터링 ──────────────────────────────────────────────────────────

    @Test
    fun getByLedgerIdAndMonth_returnsOnlyMatchingMonth() = runTest {
        dao.insert(tx(ledgerId = 1L, amount = 5_000L, month = "2026-04"))
        dao.insert(tx(ledgerId = 1L, amount = 9_999L, month = "2026-03"))  // 다른 달
        dao.insert(tx(ledgerId = 2L, amount = 7_777L, month = "2026-04"))  // 다른 장부

        val result = dao.getByLedgerIdAndMonth(1L, "2026-04").first()
        assertEquals(1, result.size)
        assertEquals(5_000L, result[0].amount)
    }

    @Test
    fun getByLedgerIdAndMonth_emptyWhenNoData() = runTest {
        val result = dao.getByLedgerIdAndMonth(1L, "2026-04").first()
        assertTrue(result.isEmpty())
    }

    // ── 소프트 딜리트 ─────────────────────────────────────────────────────────

    @Test
    fun softDelete_hidesItemFromGetByLedgerId() = runTest {
        val id = dao.insert(tx())
        dao.softDelete(id, Date())

        val result = dao.getByLedgerId(1L).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun softDelete_hidesItemFromGetByLedgerIdAndMonth() = runTest {
        val id = dao.insert(tx(month = "2026-04"))
        dao.softDelete(id, Date())

        val result = dao.getByLedgerIdAndMonth(1L, "2026-04").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun softDelete_hidesItemFromGetById() = runTest {
        val id = dao.insert(tx())
        dao.softDelete(id, Date())

        val result = dao.getById(id).first()
        assertNull(result)
    }

    // ── 합계 계산 ─────────────────────────────────────────────────────────────

    @Test
    fun getTotalIncomeByMonth_sumsCorrectly() = runTest {
        dao.insert(tx(type = "income", amount = 300_000L, month = "2026-04"))
        dao.insert(tx(type = "income", amount = 200_000L, month = "2026-04"))
        dao.insert(tx(type = "expense", amount = 100_000L, month = "2026-04"))  // 제외
        dao.insert(tx(type = "income", amount = 99_000L, month = "2026-03"))    // 다른 달 제외

        val total = dao.getTotalIncomeByMonth(1L, "2026-04").first()
        assertEquals(500_000L, total)
    }

    @Test
    fun getTotalExpenseByMonth_sumsCorrectly() = runTest {
        dao.insert(tx(type = "expense", amount = 50_000L, month = "2026-04"))
        dao.insert(tx(type = "expense", amount = 30_000L, month = "2026-04"))
        dao.insert(tx(type = "income", amount = 100_000L, month = "2026-04"))   // 제외

        val total = dao.getTotalExpenseByMonth(1L, "2026-04").first()
        assertEquals(80_000L, total)
    }

    @Test
    fun getTotalIncomeByMonth_returnsZeroWhenEmpty() = runTest {
        val total = dao.getTotalIncomeByMonth(1L, "2026-04").first()
        assertEquals(0L, total)
    }

    @Test
    fun softDeleted_notIncludedInSum() = runTest {
        val id = dao.insert(tx(type = "income", amount = 100_000L, month = "2026-04"))
        dao.softDelete(id, Date())

        val total = dao.getTotalIncomeByMonth(1L, "2026-04").first()
        assertEquals(0L, total)
    }

    // ── 동기화 관련 쿼리 ─────────────────────────────────────────────────────

    @Test
    fun getPendingTransactions_returnsOnlyPending() = runTest {
        dao.insert(tx(syncStatus = "pending", amount = 1_000L))
        dao.insert(tx(syncStatus = "synced", amount = 2_000L))

        val pending = dao.getPendingTransactions(1L)
        assertEquals(1, pending.size)
        assertEquals("pending", pending[0].syncStatus)
    }

    @Test
    fun markSynced_updatesServerIdAndStatus() = runTest {
        val id = dao.insert(tx(syncStatus = "pending"))
        dao.markSynced(id, serverId = 999L, syncedAt = Date())

        val result = dao.getByServerId(999L)
        assertNotNull(result)
        assertEquals("synced", result!!.syncStatus)
        assertEquals(999L, result.serverId)
    }

    @Test
    fun getByServerId_returnsNullWhenNotFound() = runTest {
        val result = dao.getByServerId(12345L)
        assertNull(result)
    }

    // ── 장부별 목록 ───────────────────────────────────────────────────────────

    @Test
    fun getByLedgerId_returnsOnlyMatchingLedger() = runTest {
        dao.insert(tx(ledgerId = 1L, amount = 1_000L))
        dao.insert(tx(ledgerId = 2L, amount = 2_000L))

        val result = dao.getByLedgerId(1L).first()
        assertEquals(1, result.size)
        assertEquals(1_000L, result[0].amount)
    }

    // ── 카테고리 외래키 처리 ──────────────────────────────────────────────────

    @Test
    fun insertTransaction_withCategoryId_worksCorrectly() = runTest {
        val catDao = db.categoryDao()
        val catId = catDao.insert(
            CategoryEntity(ledgerId = 1L, name = "식비", color = "#FF0000")
        )

        val txId = dao.insert(tx(amount = 20_000L).copy(categoryId = catId))
        val result = dao.getById(txId).first()

        assertNotNull(result)
        assertEquals(catId, result!!.categoryId)
    }
}
