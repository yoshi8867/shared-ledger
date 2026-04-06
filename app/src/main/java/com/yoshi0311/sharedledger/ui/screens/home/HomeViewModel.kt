package com.yoshi0311.sharedledger.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.data.repository.SyncRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import com.yoshi0311.sharedledger.network.api.OwnLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.util.AppYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class SyncState {
    object Idle    : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val isLoading: Boolean = true
)

// 장부 목록 아이템 (소유 + 공유 통합)
data class LedgerItem(
    val ledgerId: Long,
    val ledgerName: String,
    val isOwner: Boolean,
    val permission: String = "owner",   // "owner" | "view" | "edit"
    val sharedLedgerId: Long? = null    // 공유 장부일 때만
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    private val syncRepo: SyncRepository,
    private val sharedRepo: SharedRepository
) : ViewModel() {

    // 활성 장부 ID: activeLedgerId 없으면 ledgerId 폴백
    val currentLedgerId: StateFlow<Long> = authRepo.activeLedgerId
        .map { it ?: authRepo.ledgerId.firstOrNull() ?: 1L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1L)

    // ── 장부 목록 (스위처용) ─────────────────────────────────────────────────
    private val _ledgers = MutableStateFlow<List<LedgerItem>>(emptyList())
    val ledgers: StateFlow<List<LedgerItem>> = _ledgers.asStateFlow()

    fun loadLedgers() {
        viewModelScope.launch {
            val own = sharedRepo.getMyLedgers().getOrElse { emptyList() }
            val shared = sharedRepo.getSharedLedgers().getOrElse { emptyList() }
            _ledgers.value =
                own.map { LedgerItem(it.ledgerId, it.ledgerName, isOwner = true) } +
                shared.map { LedgerItem(it.ledgerId, it.ledgerName, isOwner = false,
                    permission = it.permission, sharedLedgerId = it.sharedLedgerId) }
        }
    }

    fun switchLedger(ledgerId: Long) {
        viewModelScope.launch {
            authRepo.setActiveLedgerId(ledgerId)
        }
    }

    // 탭 선택 상태 — HomeScreen 재구성 후에도 유지
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    fun selectTab(tab: Int) { _selectedTab.value = tab }

    // 캘린더 선택 날짜 — 오늘로 초기화, 월 변경 시 null 리셋
    private val _selectedCalendarDay = MutableStateFlow<Int?>(
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )
    val selectedCalendarDay: StateFlow<Int?> = _selectedCalendarDay.asStateFlow()
    fun selectCalendarDay(day: Int) { _selectedCalendarDay.value = day }

    private val _selectedMonth = MutableStateFlow(AppYearMonth.now())
    val selectedMonth: StateFlow<AppYearMonth> = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        _selectedMonth, currentLedgerId
    ) { month, ledgerId -> Pair(month, ledgerId) }
        .flatMapLatest { (month, ledgerId) ->
            combine(
                transactionRepo.getByLedgerIdAndMonth(ledgerId, month.format()),
                categoryRepo.getByLedgerId(ledgerId),
                transactionRepo.getTotalIncomeByMonth(ledgerId, month.format()),
                transactionRepo.getTotalExpenseByMonth(ledgerId, month.format())
            ) { transactions, categories, income, expense ->
                HomeUiState(
                    transactions = transactions,
                    categories = categories,
                    totalIncome = income,
                    totalExpense = expense,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun sync() {
        if (_syncState.value is SyncState.Loading) return
        viewModelScope.launch {
            _syncState.value = SyncState.Loading
            val ledgerId = currentLedgerId.value
            val result = syncRepo.sync(ledgerId)
            _syncState.value = if (result.isSuccess) SyncState.Success
                               else SyncState.Error(result.exceptionOrNull()?.message ?: "동기화 실패")
        }
    }

    fun resetSyncState() { _syncState.value = SyncState.Idle }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.next()
        _selectedCalendarDay.value = null
    }
    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.prev()
        _selectedCalendarDay.value = null
    }
}
