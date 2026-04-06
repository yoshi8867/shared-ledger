package com.yoshi0311.sharedledger.ui.screens.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.network.api.SharedUserDto
import com.yoshi0311.sharedledger.network.api.UserSearchDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedLedgerUiState(
    val sharedUsers: List<SharedUserDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed class SharedActionState {
    object Idle    : SharedActionState()
    object Loading : SharedActionState()
    object Success : SharedActionState()
    data class Error(val message: String) : SharedActionState()
}

@HiltViewModel
class SharedLedgerViewModel @Inject constructor(
    private val sharedRepo: SharedRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: -1L

    private val _uiState = MutableStateFlow(SharedLedgerUiState())
    val uiState: StateFlow<SharedLedgerUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<SharedActionState>(SharedActionState.Idle)
    val actionState: StateFlow<SharedActionState> = _actionState.asStateFlow()

    // 사용자 검색 결과
    private val _searchResult = MutableStateFlow<UserSearchDto?>(null)
    val searchResult: StateFlow<UserSearchDto?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    init {
        if (ledgerId != -1L) loadSharedUsers()
    }

    fun loadSharedUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            sharedRepo.getSharedUsers(ledgerId)
                .onSuccess { users ->
                    _uiState.value = SharedLedgerUiState(sharedUsers = users, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = SharedLedgerUiState(
                        isLoading = false,
                        errorMessage = e.message ?: "목록 조회 실패"
                    )
                }
        }
    }

    fun searchUser(email: String) {
        _searchResult.value = null
        _searchError.value = null
        viewModelScope.launch {
            sharedRepo.searchUser(email)
                .onSuccess { _searchResult.value = it }
                .onFailure { _searchError.value = it.message ?: "사용자를 찾을 수 없습니다" }
        }
    }

    fun clearSearchResult() {
        _searchResult.value = null
        _searchError.value = null
    }

    fun invite(email: String, permission: String) {
        if (ledgerId == -1L) return
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.invite(ledgerId, email, permission)
                .onSuccess {
                    _actionState.value = SharedActionState.Success
                    clearSearchResult()
                    loadSharedUsers()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "초대 실패")
                }
        }
    }

    fun updatePermission(sharedLedgerId: Long, permission: String) {
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.updatePermission(sharedLedgerId, permission)
                .onSuccess {
                    _actionState.value = SharedActionState.Success
                    loadSharedUsers()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "권한 변경 실패")
                }
        }
    }

    fun revokeAccess(sharedLedgerId: Long) {
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.revokeAccess(sharedLedgerId)
                .onSuccess {
                    _actionState.value = SharedActionState.Success
                    loadSharedUsers()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "공유 해제 실패")
                }
        }
    }

    fun resetActionState() { _actionState.value = SharedActionState.Idle }
}
