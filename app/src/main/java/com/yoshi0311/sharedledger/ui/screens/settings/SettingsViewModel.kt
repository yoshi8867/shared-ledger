package com.yoshi0311.sharedledger.ui.screens.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.network.ServerUrlProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

sealed class HealthState {
    object Idle : HealthState()
    object Loading : HealthState()
    object Online : HealthState()
    object Offline : HealthState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverUrlProvider: ServerUrlProvider,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    val serverUrl: StateFlow<String> = authRepository.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerUrlProvider.DEFAULT_URL)

    val syncInterval: StateFlow<String> = authRepository.syncInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "manual")

    val notificationsEnabled: StateFlow<Boolean> = authRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _healthState = MutableStateFlow<HealthState>(HealthState.Idle)
    val healthState: StateFlow<HealthState> = _healthState

    fun saveServerUrl(url: String) {
        val normalized = url.trim().trimEnd('/') + "/"
        viewModelScope.launch {
            authRepository.setServerUrl(normalized)
            serverUrlProvider.baseUrl = normalized
        }
    }

    fun setSyncInterval(interval: String) {
        viewModelScope.launch {
            authRepository.setSyncInterval(interval)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setNotificationsEnabled(enabled)
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                _healthState.value = HealthState.Loading
            }

            val newState = try {
                val client = OkHttpClient()
                val url = serverUrlProvider.baseUrl.trimEnd('/') + "/api/health"
                Log.d(TAG, "Health check URL: $url")
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                Log.d(TAG, "Health check response: ${response.code}")
                val state = if (response.isSuccessful) {
                    Log.d(TAG, "Server ONLINE")
                    HealthState.Online
                } else {
                    Log.d(TAG, "Server response not successful: ${response.code}")
                    HealthState.Offline
                }
                response.close()
                state
            } catch (e: Exception) {
                Log.e(TAG, "Health check failed", e)
                HealthState.Offline
            }

            launch(Dispatchers.Main) {
                _healthState.value = newState
            }
        }
    }
}
