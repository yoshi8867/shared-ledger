package com.yoshi0311.sharedledger.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination { NONE, LOGIN, HOME }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.NONE)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1500L) // 애니메이션 최소 표시 시간
            _destination.value = if (authRepository.isLoggedIn()) {
                SplashDestination.HOME
            } else {
                SplashDestination.LOGIN
            }
        }
    }
}
