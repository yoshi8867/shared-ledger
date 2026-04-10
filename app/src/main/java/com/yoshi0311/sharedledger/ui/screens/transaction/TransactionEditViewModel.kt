package com.yoshi0311.sharedledger.ui.screens.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import com.yoshi0311.sharedledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class TransactionEditUiState(
    val existing: TransactionEntity? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val transactionId: Long = savedStateHandle.get<Long>("id") ?: -1L
    val initialDateMillis: Long = savedStateHandle.get<Long>("dateMillis") ?: -1L

    private suspend fun currentLedgerId(): Long = authRepo.ledgerId.firstOrNull() ?: 1L

    private val _uiState = MutableStateFlow(TransactionEditUiState())
    val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = authRepo.ledgerId
        .map { it ?: 1L }
        .flatMapLatest { ledgerId -> categoryRepo.getByLedgerId(ledgerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (transactionId != -1L) {
            viewModelScope.launch {
                val tx = transactionRepo.getById(transactionId)
                _uiState.value = TransactionEditUiState(existing = tx, isLoading = false)
            }
        } else {
            _uiState.value = TransactionEditUiState(isLoading = false)
        }
    }

    fun save(
        type: String,
        amount: Long,
        date: Date,
        time: String,
        categoryId: Long?,
        description: String
    ) {
        viewModelScope.launch {
            val ledgerId = currentLedgerId()
            val existing = _uiState.value.existing
            if (existing == null) {
                transactionRepo.insert(
                    TransactionEntity(
                        ledgerId = ledgerId,
                        categoryId = categoryId,
                        type = type,
                        amount = amount,
                        date = date,
                        time = time,
                        description = description
                    )
                )
            } else {
                transactionRepo.update(
                    existing.copy(
                        type = type,
                        amount = amount,
                        date = date,
                        time = time,
                        categoryId = categoryId,
                        description = description,
                        updatedAt = Date()
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun delete() {
        if (transactionId == -1L) return
        viewModelScope.launch {
            transactionRepo.softDelete(transactionId)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun addCategory(name: String, color: String, type: String) {
        viewModelScope.launch {
            categoryRepo.insert(
                CategoryEntity(
                    ledgerId = currentLedgerId(),
                    name = name,
                    color = color,
                    type = type
                )
            )
        }
    }
}
