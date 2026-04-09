package com.yoshi0311.sharedledger.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.network.ServerUrlProvider
import com.yoshi0311.sharedledger.network.api.SyncApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination { NONE, LOGIN, HOME }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val syncApi: SyncApi
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.NONE)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // 저장된 서버 URL을 로드하여 네트워크 레이어에 적용
            serverUrlProvider.baseUrl = authRepository.serverUrl.first()
            // Render 서버 웜업 핑 (결과 무시, 백그라운드 실행)
            launch { runCatching { syncApi.ping() } }
            delay(1500L)
            _destination.value = if (authRepository.isLoggedIn()) {
                SplashDestination.HOME
            } else {
                SplashDestination.LOGIN
            }
        }
    }
}
