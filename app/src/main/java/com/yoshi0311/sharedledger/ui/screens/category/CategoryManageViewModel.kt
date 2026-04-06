package com.yoshi0311.sharedledger.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val categories: StateFlow<List<CategoryEntity>> = authRepository.activeLedgerId
        .flatMapLatest { ledgerId ->
            if (ledgerId != null) {
                categoryRepository.getByLedgerId(ledgerId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            val ledgerId = authRepository.activeLedgerId.stateIn(viewModelScope).value
                ?: return@launch
            val category = CategoryEntity(
                id = 0,
                ledgerId = ledgerId,
                name = name,
                color = colorHex,
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
            val ledgerId = authRepository.activeLedgerId.stateIn(viewModelScope).value
                ?: return@launch
            val category = CategoryEntity(
                id = id,
                ledgerId = ledgerId,
                name = name,
                color = colorHex,
                syncStatus = "pending",
                isDeleted = false,
                deletedAt = null,
                serverId = null,
                createdAt = Date(),
                updatedAt = Date()
            )
            categoryRepository.update(category)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.softDelete(id)
        }
    }
}
