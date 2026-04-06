package com.yoshi0311.sharedledger.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "이메일과 비밀번호를 입력해주세요")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            _uiState.value = when (val result = authRepository.login(email.trim(), password)) {
                is AuthResult.Success -> AuthUiState(isSuccess = true)
                is AuthResult.Error -> AuthUiState(error = result.message)
            }
        }
    }

    fun signup(name: String, email: String, password: String, passwordConfirm: String) {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() ->
                _uiState.value = AuthUiState(error = "모든 필드를 입력해주세요")
            password != passwordConfirm ->
                _uiState.value = AuthUiState(error = "비밀번호가 일치하지 않습니다")
            password.length < 8 ->
                _uiState.value = AuthUiState(error = "비밀번호는 8자 이상이어야 합니다")
            else -> viewModelScope.launch {
                _uiState.value = AuthUiState(isLoading = true)
                _uiState.value = when (val result = authRepository.signup(name.trim(), email.trim(), password)) {
                    is AuthResult.Success -> AuthUiState(isSuccess = true)
                    is AuthResult.Error -> AuthUiState(error = result.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
