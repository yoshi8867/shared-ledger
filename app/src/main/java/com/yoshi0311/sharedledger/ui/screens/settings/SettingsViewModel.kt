package com.yoshi0311.sharedledger.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.network.ServerUrlProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val serverUrlProvider: ServerUrlProvider
) : ViewModel() {

    val serverUrl: StateFlow<String> = authRepository.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerUrlProvider.DEFAULT_URL)

    fun saveServerUrl(url: String) {
        val normalized = url.trim().trimEnd('/') + "/"
        viewModelScope.launch {
            authRepository.setServerUrl(normalized)
            serverUrlProvider.baseUrl = normalized
        }
    }
}
