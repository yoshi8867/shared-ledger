package com.yoshi0311.sharedledger.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.dao.MonthlyCategorySum
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.AutoFillRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.data.repository.SyncRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import com.yoshi0311.sharedledger.network.api.OwnLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.util.AppYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException
import java.util.Calendar
import javax.inject.Inject

sealed class SyncState {
    object Idle        : SyncState()
    object Loading     : SyncState()
    object Success     : SyncState()
    object AuthExpired : SyncState()
    data class Error(val message: String) : SyncState()
}

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val isLoading: Boolean = true
)

/**
 * 검색 모달 필터. 각 조건은 서로 AND.
 * - 수입/지출 체크박스: 서로 OR
 * - 카테고리(복수 선택): 서로 OR (비어있으면 전체)
 * - 기간(startDate~endDate, 포함) / 금액 / 검색어(내역): 각각 AND
 */
data class SearchFilter(
    val query: String = "",
    val income: Boolean = true,
    val expense: Boolean = true,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val startDate: Long? = null,      // 포함, 해당 일 00:00:00.000
    val endDate: Long? = null,        // 포함, 해당 일 23:59:59.999
    val categoryIds: Set<Long> = emptySet()
) {
    fun matches(tx: TransactionEntity): Boolean {
        val typeOk = (income && tx.type == "income") || (expense && tx.type == "expense")
        if (!typeOk) return false
        if (query.isNotBlank() && !tx.description.contains(query.trim(), ignoreCase = true)) return false
        minAmount?.let { if (tx.amount < it) return false }
        maxAmount?.let { if (tx.amount > it) return false }
        startDate?.let { if (tx.date.time < it) return false }
        endDate?.let { if (tx.date.time > it) return false }
        if (categoryIds.isNotEmpty() && tx.categoryId !in categoryIds) return false
        return true
    }
}

// 장부 목록 아이템 (소유 + 공유 통합)
data class LedgerItem(
    val ledgerId: Long,
    val ledgerName: String,
    val isOwner: Boolean,
    val permission: String = "owner",   // "owner" | "view" | "edit"
    val sharedLedgerId: Long? = null,   // 공유 장부일 때만
    val ownerName: String? = null,      // 공유받은 장부의 소유자명
    val ownerEmail: String? = null      // 공유받은 장부의 소유자 이메일
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    private val syncRepo: SyncRepository,
    private val sharedRepo: SharedRepository,
    private val autoFillRepo: AutoFillRepository
) : ViewModel() {

    val pendingCount: StateFlow<Int> = autoFillRepo.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // 활성 장부 ID: 게스트면 게스트 전용 ID, 아니면 activeLedgerId(없으면 ledgerId 폴백)
    val currentLedgerId: StateFlow<Long> = authRepo.currentLedgerIdFlow
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
                shared.map {
                    LedgerItem(
                        ledgerId = it.ledgerId,
                        ledgerName = it.ledgerName,
                        isOwner = false,
                        permission = it.permission,
                        sharedLedgerId = it.sharedLedgerId,
                        ownerName = it.ownerName,
                        ownerEmail = it.ownerEmail
                    )
                }
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

    // ── 검색 모달 (전 기간 대상, 목록/캘린더/통계와 독립) ─────────────────────────
    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()
    fun updateSearch(filter: SearchFilter) { _searchFilter.value = filter }

    /** 모달 열기 — 기간은 현재 보고 있는 달로 기본 설정, 나머지 조건은 마지막 값 유지 */
    fun openSearch() {
        val m = _selectedMonth.value
        val cal = Calendar.getInstance().apply {
            set(m.year, m.month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        _searchFilter.value = _searchFilter.value.copy(startDate = start, endDate = cal.timeInMillis)
        _searchOpen.value = true
    }

    fun closeSearch() { _searchOpen.value = false }

    // 전 기간 거래 (검색 결과 산출용)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allTransactions: StateFlow<List<TransactionEntity>> =
        currentLedgerId.flatMapLatest { transactionRepo.getByLedgerId(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchResults: StateFlow<List<TransactionEntity>> = combine(
        _searchFilter, allTransactions
    ) { filter, all ->
        all.filter(filter::matches)
            .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.time })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 월별 추이 그래프 ──────────────────────────────────────────────────────
    // 0 = 오늘 기준 최근 6개월, 1 = 그 이전 6개월 …
    private val _trendOffset = MutableStateFlow(0)
    val trendOffset: StateFlow<Int> = _trendOffset.asStateFlow()

    fun shiftTrendWindow(delta: Int) {
        _trendOffset.value = (_trendOffset.value + delta).coerceAtLeast(0)
    }

    /** offset에 해당하는 6개월 윈도우 (과거 → 최신 순) */
    fun trendMonths(offset: Int): List<AppYearMonth> {
        var end = AppYearMonth.now()
        repeat(offset * 6) { end = end.prev() }
        val months = ArrayDeque<AppYearMonth>()
        var cur = end
        repeat(6) { months.addFirst(cur); cur = cur.prev() }
        return months.toList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val trendSums: StateFlow<List<MonthlyCategorySum>> = combine(
        currentLedgerId, _trendOffset
    ) { ledgerId, offset -> Pair(ledgerId, offset) }
        .flatMapLatest { (ledgerId, offset) ->
            val months = trendMonths(offset)
            transactionRepo.getMonthlyCategorySums(
                ledgerId, months.first().format(), months.last().format()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 추이 그래프 체크된 카테고리 이름 (지출/수입 탭별로 저장·복원)
    val trendCheckedExpense: StateFlow<Set<String>> = authRepo.getTrendCategories("expense")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val trendCheckedIncome: StateFlow<Set<String>> = authRepo.getTrendCategories("income")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleTrendCategory(type: String, name: String) {
        viewModelScope.launch {
            val current = authRepo.getTrendCategories(type).firstOrNull() ?: emptySet()
            authRepo.setTrendCategories(
                type,
                if (name in current) current - name else current + name
            )
        }
    }

    // 추이 막대 탭 → 해당 월 거래 내역 모달
    data class TrendDetail(val month: AppYearMonth, val transactions: List<TransactionEntity>)

    private val _trendDetail = MutableStateFlow<TrendDetail?>(null)
    val trendDetail: StateFlow<TrendDetail?> = _trendDetail.asStateFlow()

    fun openTrendDetail(month: AppYearMonth) {
        viewModelScope.launch {
            val txs = transactionRepo
                .getByLedgerIdAndMonth(currentLedgerId.value, month.format())
                .firstOrNull() ?: emptyList()
            _trendDetail.value = TrendDetail(month, txs)
        }
    }

    fun closeTrendDetail() { _trendDetail.value = null }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var syncJob: Job? = null

    init {
        sync(silent = true)
    }

    fun sync(silent: Boolean = false) {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            // 게스트 모드: 서버 동기화 없이 로컬 전용. 수동 동기화 시에만 안내.
            if (authRepo.guestMode.firstOrNull() == true) {
                if (!silent) _syncState.value = SyncState.Error("로그인해야 동기화할 수 있습니다")
                return@launch
            }
            if (!silent) _syncState.value = SyncState.Loading
            while (isActive) {
                val ledgerId = currentLedgerId.value
                val result = syncRepo.sync(ledgerId)
                when {
                    result.isSuccess -> {
                        if (!silent) _syncState.value = SyncState.Success
                        break
                    }
                    result.exceptionOrNull() is IOException -> {
                        // 네트워크/타임아웃 오류 → 60초 후 재시도
                        delay(60_000L)
                    }
                    else -> {
                        val e = result.exceptionOrNull()
                        if (e is HttpException && e.code() == 401) {
                            _syncState.value = SyncState.AuthExpired
                        } else if (!silent) {
                            _syncState.value = SyncState.Error(e?.message ?: "동기화 실패")
                        }
                        break
                    }
                }
            }
        }
    }

    fun resetSyncState() {
        syncJob?.cancel()
        _syncState.value = SyncState.Idle
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.next()
        _selectedCalendarDay.value = null
    }
    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.prev()
        _selectedCalendarDay.value = null
    }
}
