package com.yoshi0311.sharedledger.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow("expense")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    fun selectType(type: String) { _selectedType.value = type }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = combine(
        authRepository.activeLedgerId, _selectedType
    ) { ledgerId, type -> Pair(ledgerId, type) }
        .flatMapLatest { (ledgerId, type) ->
            if (ledgerId != null) {
                categoryRepository.getByLedgerIdAndType(ledgerId, type)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, colorHex: String, type: String) {
        viewModelScope.launch {
            val ledgerId = authRepository.activeLedgerId.stateIn(viewModelScope).value
                ?: return@launch
            val category = CategoryEntity(
                id = 0,
                ledgerId = ledgerId,
                name = name,
                color = colorHex,
                type = type,
                syncStatus = "pending",
                isDeleted = false,
                deletedAt = null,
                serverId = null,
                createdAt = Date(),
                updatedAt = Date()
            )
            categoryRepository.insert(category)
        }
    }

    fun updateCategory(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            val existing = categoryRepository.getByIdSync(id) ?: return@launch
            categoryRepository.update(
                existing.copy(
                    name = name,
                    color = colorHex,
                    updatedAt = Date()
                )
            )
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.softDelete(id)
        }
    }
}
