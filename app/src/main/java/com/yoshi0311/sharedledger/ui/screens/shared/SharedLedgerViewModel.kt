package com.yoshi0311.sharedledger.ui.screens.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.SharedRepository
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedUserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
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

// 장부 선택 드롭다운용 아이템
data class LedgerSelectItem(
    val ledgerId: Long,
    val ledgerName: String,
    val isOwner: Boolean
)

@HiltViewModel
class SharedLedgerViewModel @Inject constructor(
    private val sharedRepo: SharedRepository,
    private val authRepo: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val ledgerId: Long = savedStateHandle.get<Long>("ledgerId") ?: -1L

    private val _ledgerName = MutableStateFlow("")
    val ledgerName: StateFlow<String> = _ledgerName.asStateFlow()

    // getSharedUsers 성공(200) → 소유자, 403 → 비소유자, 기타 에러 → 불명(false 유지)
    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _uiState = MutableStateFlow(SharedLedgerUiState())
    val uiState: StateFlow<SharedLedgerUiState> = _uiState.asStateFlow()

    private val _sharedWithMe = MutableStateFlow<List<SharedLedgerDto>>(emptyList())
    val sharedWithMe: StateFlow<List<SharedLedgerDto>> = _sharedWithMe.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _inviteCodeExpiry = MutableStateFlow<String?>(null)
    val inviteCodeExpiry: StateFlow<String?> = _inviteCodeExpiry.asStateFlow()

    private val _actionState = MutableStateFlow<SharedActionState>(SharedActionState.Idle)
    val actionState: StateFlow<SharedActionState> = _actionState.asStateFlow()

    // ── 장부 선택 ──────────────────────────────────────────────────────────────
    private val _allLedgers = MutableStateFlow<List<LedgerSelectItem>>(emptyList())
    val allLedgers: StateFlow<List<LedgerSelectItem>> = _allLedgers.asStateFlow()

    // 현재 '기록 대상' 활성 장부 ID
    val activeLedgerId: StateFlow<Long> = authRepo.activeLedgerId
        .map { it ?: authRepo.ledgerId.firstOrNull() ?: -1L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1L)

    init {
        if (ledgerId != -1L) {
            loadLedgerInfo()
            loadSharedUsers()
        }
        loadSharedWithMe()
        loadAllLedgers()
    }

    private fun loadLedgerInfo() {
        viewModelScope.launch {
            sharedRepo.getLedger(ledgerId)
                .onSuccess { _ledgerName.value = it.ledgerName }
        }
    }

    private fun loadAllLedgers() {
        viewModelScope.launch {
            val own    = sharedRepo.getMyLedgers().getOrElse { emptyList() }
            val shared = sharedRepo.getSharedLedgers().getOrElse { emptyList() }
            _allLedgers.value =
                own.map { LedgerSelectItem(it.ledgerId, it.ledgerName, isOwner = true) } +
                shared.map { LedgerSelectItem(it.ledgerId, it.ledgerName, isOwner = false) }
        }
    }

    fun switchActiveLedger(ledgerId: Long) {
        viewModelScope.launch {
            authRepo.setActiveLedgerId(ledgerId)
        }
    }

    // ── 공유 관리 ──────────────────────────────────────────────────────────────

    fun loadSharedUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            sharedRepo.getSharedUsers(ledgerId)
                .onSuccess { users ->
                    _isOwner.value = true
                    _uiState.value = SharedLedgerUiState(sharedUsers = users, isLoading = false)
                }
                .onFailure { e ->
                    if (e is HttpException && e.code() == 403) {
                        _isOwner.value = false
                        _uiState.value = SharedLedgerUiState(isLoading = false)
                    } else {
                        _uiState.value = SharedLedgerUiState(
                            isLoading = false,
                            errorMessage = "공유 사용자 목록을 불러오지 못했습니다"
                        )
                    }
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

    fun leaveSharedLedger(sharedLedgerId: Long) {
        viewModelScope.launch {
            _actionState.value = SharedActionState.Loading
            sharedRepo.revokeAccess(sharedLedgerId)
                .onSuccess {
                    _actionState.value = SharedActionState.Success
                    loadSharedWithMe()
                    loadAllLedgers()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "나가기 실패")
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
                    loadAllLedgers()
                }
                .onFailure { e ->
                    _actionState.value = SharedActionState.Error(e.message ?: "참가 실패")
                }
        }
    }

    fun resetActionState() { _actionState.value = SharedActionState.Idle }
}
