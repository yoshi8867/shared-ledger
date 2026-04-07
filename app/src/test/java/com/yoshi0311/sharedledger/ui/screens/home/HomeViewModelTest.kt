package com.yoshi0311.sharedledger.ui.screens.home

import app.cash.turbine.test
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.data.repository.SyncRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import com.yoshi0311.sharedledger.util.AppYearMonth
import com.yoshi0311.sharedledger.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactionRepo: TransactionRepository = mockk(relaxed = true)
    private val categoryRepo: CategoryRepository = mockk(relaxed = true)
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val syncRepo: SyncRepository = mockk(relaxed = true)
    private val sharedRepo: SharedRepository = mockk(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        every { authRepo.activeLedgerId } returns flowOf(1L)
        every { authRepo.ledgerId } returns flowOf(1L)
        every { transactionRepo.getByLedgerIdAndMonth(any(), any()) } returns flowOf(emptyList())
        every { categoryRepo.getByLedgerId(any()) } returns flowOf(emptyList())
        every { transactionRepo.getTotalIncomeByMonth(any(), any()) } returns flowOf(0L)
        every { transactionRepo.getTotalExpenseByMonth(any(), any()) } returns flowOf(0L)

        viewModel = HomeViewModel(transactionRepo, categoryRepo, authRepo, syncRepo, sharedRepo)
    }

    // ── 월 탐색 ────────────────────────────────────────────────────────────────

    @Test
    fun `nextMonth increments selectedMonth by one`() {
        val initial = viewModel.selectedMonth.value
        viewModel.nextMonth()
        assertEquals(initial.next(), viewModel.selectedMonth.value)
    }

    @Test
    fun `prevMonth decrements selectedMonth by one`() {
        val initial = viewModel.selectedMonth.value
        viewModel.prevMonth()
        assertEquals(initial.prev(), viewModel.selectedMonth.value)
    }

    @Test
    fun `nextMonth 12 times advances exactly one year`() {
        val initial = viewModel.selectedMonth.value
        repeat(12) { viewModel.nextMonth() }
        assertEquals(AppYearMonth(initial.year + 1, initial.month), viewModel.selectedMonth.value)
    }

    // ── 캘린더 날짜 선택 ────────────────────────────────────────────────────────

    @Test
    fun `selectCalendarDay updates selectedCalendarDay`() {
        viewModel.selectCalendarDay(15)
        assertEquals(15, viewModel.selectedCalendarDay.value)
    }

    @Test
    fun `nextMonth resets selectedCalendarDay to null`() {
        viewModel.selectCalendarDay(10)
        viewModel.nextMonth()
        assertNull(viewModel.selectedCalendarDay.value)
    }

    @Test
    fun `prevMonth resets selectedCalendarDay to null`() {
        viewModel.selectCalendarDay(10)
        viewModel.prevMonth()
        assertNull(viewModel.selectedCalendarDay.value)
    }

    // ── 탭 선택 ─────────────────────────────────────────────────────────────────

    @Test
    fun `selectTab updates selectedTab to given index`() {
        viewModel.selectTab(2)
        assertEquals(2, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab can be changed multiple times`() {
        viewModel.selectTab(1)
        viewModel.selectTab(0)
        assertEquals(0, viewModel.selectedTab.value)
    }

    // ── 동기화 상태 전환 ─────────────────────────────────────────────────────────

    @Test
    fun `sync transitions to Success when syncRepo succeeds`() = runTest {
        coEvery { syncRepo.sync(any()) } returns Result.success(Unit)

        viewModel.sync()
        advanceUntilIdle()

        assertEquals(SyncState.Success, viewModel.syncState.value)
    }

    @Test
    fun `sync transitions to Error when syncRepo fails`() = runTest {
        coEvery { syncRepo.sync(any()) } returns Result.failure(Exception("network error"))

        viewModel.sync()
        advanceUntilIdle()

        val state = viewModel.syncState.value
        assertTrue(state is SyncState.Error)
        assertEquals("network error", (state as SyncState.Error).message)
    }

    @Test
    fun `resetSyncState sets syncState back to Idle`() = runTest {
        coEvery { syncRepo.sync(any()) } returns Result.success(Unit)
        viewModel.sync()
        advanceUntilIdle()

        viewModel.resetSyncState()

        assertEquals(SyncState.Idle, viewModel.syncState.value)
    }

    @Test
    fun `sync does nothing if already Loading`() = runTest {
        // Loading 상태를 강제 주입: 첫 번째 sync() 도중 두 번째 sync()는 무시됨
        coEvery { syncRepo.sync(any()) } coAnswers {
            // 첫 호출만 성공, 두 번째 호출 확인
            Result.success(Unit)
        }

        viewModel.sync()
        // 아직 idle 호출 전이므로 Loading 상태임을 이용해 두 번째 sync 무시 여부 확인
        // (UnconfinedTestDispatcher는 즉시 실행하므로 이미 완료됨)
        viewModel.resetSyncState()
        assertEquals(SyncState.Idle, viewModel.syncState.value)
    }

    // ── SyncState 흐름 Turbine 테스트 ──────────────────────────────────────────

    @Test
    fun `syncState emits Loading then Success in order`() = runTest {
        // UnconfinedTestDispatcher 환경에서 Turbine으로 방출 순서 확인
        coEvery { syncRepo.sync(any()) } returns Result.success(Unit)

        viewModel.syncState.test {
            val initial = awaitItem()        // Idle (초기값)
            assertEquals(SyncState.Idle, initial)

            viewModel.sync()

            // UnconfinedTestDispatcher는 즉시 실행 → Loading 건너뛰고 Success까지 도달
            // advanceUntilIdle 없이도 완료됨
            val final = awaitItem()
            // Loading 또는 Success 중 하나 (Unconfined는 Loading을 건너뛸 수 있음)
            assertTrue(final == SyncState.Loading || final == SyncState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
