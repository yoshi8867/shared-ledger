package com.yoshi0311.sharedledger.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yoshi0311.sharedledger.data.db.AppDatabase
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
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
import java.util.Date

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private fun cat(
        ledgerId: Long = 1L,
        name: String = "식비",
        color: String = "#FF0000",
        syncStatus: String = "pending",
        serverId: Long? = null
    ) = CategoryEntity(
        ledgerId = ledgerId,
        name = name,
        color = color,
        syncStatus = syncStatus,
        serverId = serverId
    )

    // ── 기본 CRUD ─────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(cat(name = "교통비"))
        val result = dao.getById(id).first()
        assertNotNull(result)
        assertEquals("교통비", result!!.name)
    }

    @Test
    fun updateChangesName() = runTest {
        val id = dao.insert(cat(name = "식비"))
        val original = dao.getById(id).first()!!
        dao.update(original.copy(name = "외식"))

        val updated = dao.getById(id).first()
        assertEquals("외식", updated!!.name)
    }

    @Test
    fun getByLedgerId_returnsOnlyMatchingLedger() = runTest {
        dao.insert(cat(ledgerId = 1L, name = "식비"))
        dao.insert(cat(ledgerId = 2L, name = "교통비"))

        val result = dao.getByLedgerId(1L).first()
        assertEquals(1, result.size)
        assertEquals("식비", result[0].name)
    }

    @Test
    fun getByLedgerId_sortedByNameAscending() = runTest {
        dao.insert(cat(name = "카페"))
        dao.insert(cat(name = "교통비"))
        dao.insert(cat(name = "식비"))

        val result = dao.getByLedgerId(1L).first()
        assertEquals(listOf("교통비", "식비", "카페"), result.map { it.name })
    }

    // ── 소프트 딜리트 ─────────────────────────────────────────────────────────

    @Test
    fun softDelete_hidesItemFromGetByLedgerId() = runTest {
        val id = dao.insert(cat())
        dao.softDelete(id, Date())

        val result = dao.getByLedgerId(1L).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun softDelete_hidesItemFromGetById() = runTest {
        val id = dao.insert(cat())
        dao.softDelete(id, Date())

        val result = dao.getById(id).first()
        assertNull(result)
    }

    @Test
    fun softDelete_doesNotAffectOtherItems() = runTest {
        val id1 = dao.insert(cat(name = "식비"))
        val id2 = dao.insert(cat(name = "교통비"))
        dao.softDelete(id1, Date())

        val result = dao.getByLedgerId(1L).first()
        assertEquals(1, result.size)
        assertEquals("교통비", result[0].name)
    }

    // ── 동기화 관련 쿼리 ─────────────────────────────────────────────────────

    @Test
    fun getPendingCategories_returnsOnlyPending() = runTest {
        dao.insert(cat(syncStatus = "pending", name = "식비"))
        dao.insert(cat(syncStatus = "synced", name = "교통비"))

        val pending = dao.getPendingCategories(1L)
        assertEquals(1, pending.size)
        assertEquals("pending", pending[0].syncStatus)
    }

    @Test
    fun markSynced_updatesServerIdAndStatus() = runTest {
        val id = dao.insert(cat(syncStatus = "pending"))
        dao.markSynced(id, serverId = 42L, syncedAt = Date())

        val result = dao.getByServerId(42L)
        assertNotNull(result)
        assertEquals("synced", result!!.syncStatus)
        assertEquals(42L, result.serverId)
    }

    @Test
    fun getByServerId_returnsNullWhenNotFound() = runTest {
        val result = dao.getByServerId(99999L)
        assertNull(result)
    }

    @Test
    fun getByIdSync_returnsItemIncludingDeleted() = runTest {
        val id = dao.insert(cat(name = "삭제될 항목"))
        dao.softDelete(id, Date())

        // getByIdSync는 is_deleted 필터 없이 조회
        val result = dao.getByIdSync(id)
        assertNotNull(result)
        assertEquals("삭제될 항목", result!!.name)
    }

    // ── 하드 퍼지 ─────────────────────────────────────────────────────────────

    @Test
    fun purgeDeletedCategories_removesOldDeleted() = runTest {
        val id = dao.insert(cat())
        val pastDate = Date(System.currentTimeMillis() - 1000)
        dao.softDelete(id, pastDate)

        // pastDate보다 이후 시점으로 퍼지 → 삭제됨
        dao.purgeDeletedCategories(Date(System.currentTimeMillis()))

        val result = dao.getByIdSync(id)
        assertNull(result)
    }
}
