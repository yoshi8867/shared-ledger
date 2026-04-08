package com.yoshi0311.sharedledger.ui.screens.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
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

    // 내 장부 이름
    private val _ledgerName = MutableStateFlow("")
    val ledgerName: StateFlow<String> = _ledgerName.asStateFlow()

    // 이 장부의 소유자인지 여부 (getSharedUsers 성공 여부로 판단)
    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    // 이 장부를 공유 중인 사용자 목록
    private val _uiState = MutableStateFlow(SharedLedgerUiState())
    val uiState: StateFlow<SharedLedgerUiState> = _uiState.asStateFlow()

    // 나와 공유된 장부 목록 (다른 사람이 공유해준 장부)
    private val _sharedWithMe = MutableStateFlow<List<SharedLedgerDto>>(emptyList())
    val sharedWithMe: StateFlow<List<SharedLedgerDto>> = _sharedWithMe.asStateFlow()

    // 생성된 초대 코드
    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _inviteCodeExpiry = MutableStateFlow<String?>(null)
    val inviteCodeExpiry: StateFlow<String?> = _inviteCodeExpiry.asStateFlow()

    private val _actionState = MutableStateFlow<SharedActionState>(SharedActionState.Idle)
    val actionState: StateFlow<SharedActionState> = _actionState.asStateFlow()

    private val _searchResult = MutableStateFlow<UserSearchDto?>(null)
    val searchResult: StateFlow<UserSearchDto?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    init {
        if (ledgerId != -1L) {
            loadLedgerInfo()
            loadSharedUsers()
        }
        loadSharedWithMe()
    }

    private fun loadLedgerInfo() {
        viewModelScope.launch {
            sharedRepo.getLedger(ledgerId)
                .onSuccess { _ledgerName.value = it.ledgerName }
        }
    }

    fun loadSharedUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            sharedRepo.getSharedUsers(ledgerId)
                .onSuccess { users ->
                    _isOwner.value = true
                    _uiState.value = SharedLedgerUiState(sharedUsers = users, isLoading = false)
                }
                .onFailure {
                    _isOwner.value = false
                    _uiState.value = SharedLedgerUiState(isLoading = false)
                }
        }
    }

    fun loadSharedWithMe() {
        viewModelScope.launch {
            sharedRepo.getSharedLedgers()
                .onSuccess { _sharedWithMe.value = it }
        }
    }

    fun renameLedger(name: String) {
        if (ledgerId == -1L || name.isBlank()) return
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.renameLedger(ledgerId, name)
                .onSuccess {
                    _ledgerName.value = it.ledgerName
                    _actionState.value = SharedActionState.Success
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "이름 변경 실패")
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

    fun generateInviteCode() {
        if (ledgerId == -1L) return
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.generateInviteCode(ledgerId)
                .onSuccess { res ->
                    _inviteCode.value = res.inviteCode
                    _inviteCodeExpiry.value = res.expiresAt
                    _actionState.value = SharedActionState.Idle
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "코드 생성 실패")
                }
        }
    }

    fun joinLedger(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.joinLedger(code.trim().uppercase())
                .onSuccess {
                    _actionState.value = SharedActionState.Success
                    loadSharedWithMe()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "참가 실패")
                }
        }
    }

    fun resetActionState() { _actionState.value = SharedActionState.Idle }
}
