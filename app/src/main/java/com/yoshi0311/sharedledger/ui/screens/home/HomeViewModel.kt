package com.yoshi0311.sharedledger.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val incomeCount: Long = 0L,
    val expenseCount: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    // 현재 장부 ID (추후 동적으로 변경 예정)
    private val currentLedgerId = 1L

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                transactionRepo.getByLedgerId(currentLedgerId),
                categoryRepo.getByLedgerId(currentLedgerId),
                transactionRepo.getTotalIncome(currentLedgerId),
                transactionRepo.getTotalExpense(currentLedgerId)
            ) { transactions, categories, incomeCount, expenseCount ->
                HomeUiState(
                    transactions = transactions,
                    categories = categories,
                    incomeCount = incomeCount,
                    expenseCount = expenseCount,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
