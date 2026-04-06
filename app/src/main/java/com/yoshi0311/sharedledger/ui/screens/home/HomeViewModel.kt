package com.yoshi0311.sharedledger.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import com.yoshi0311.sharedledger.util.AppYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    // 추후 공유 장부 지원 시 동적으로 변경
    private val currentLedgerId = 1L

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
    val uiState: StateFlow<HomeUiState> = _selectedMonth
        .flatMapLatest { month ->
            combine(
                transactionRepo.getByLedgerIdAndMonth(currentLedgerId, month.format()),
                categoryRepo.getByLedgerId(currentLedgerId),
                transactionRepo.getTotalIncomeByMonth(currentLedgerId, month.format()),
                transactionRepo.getTotalExpenseByMonth(currentLedgerId, month.format())
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

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.next()
        _selectedCalendarDay.value = null
    }
    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.prev()
        _selectedCalendarDay.value = null
    }
}
