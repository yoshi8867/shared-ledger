package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.network.api.AuthApi
import com.yoshi0311.sharedledger.network.api.GoogleLoginRequest
import com.yoshi0311.sharedledger.network.api.LoginRequest
import com.yoshi0311.sharedledger.network.api.NaverLoginRequest
import com.yoshi0311.sharedledger.network.api.RefreshRequest
import com.yoshi0311.sharedledger.network.api.SignupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val authDataStore: AuthDataStore
) {
    val accessToken:          Flow<String?> = authDataStore.accessToken
    val ledgerId:             Flow<Long?>   = authDataStore.ledgerId
    val activeLedgerId:       Flow<Long?>   = authDataStore.activeLedgerId
    val serverUrl:            Flow<String>  = authDataStore.serverUrl
    val syncInterval:         Flow<String>  = authDataStore.syncInterval
    val notificationsEnabled: Flow<Boolean> = authDataStore.notificationsEnabled
    val enabledPackages:      Flow<Set<String>> = authDataStore.enabledPackages
    val lastLoginMethod:      Flow<String?> = authDataStore.lastLoginMethod

    suspend fun setActiveLedgerId(id: Long) = authDataStore.setActiveLedgerId(id)

    suspend fun setServerUrl(url: String) = authDataStore.saveServerUrl(url)

    suspend fun isLoggedIn(): Boolean =
        authDataStore.accessToken.firstOrNull() != null

    suspend fun login(email: String, password: String): AuthResult = runCatching {
        val res = api.login(LoginRequest(email, password))
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("email")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun loginWithGoogle(idToken: String): AuthResult = runCatching {
        val res = api.loginWithGoogle(GoogleLoginRequest(idToken))
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("google")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun loginWithNaver(accessToken: String): AuthResult = runCatching {
        val res = api.loginWithNaver(NaverLoginRequest(accessToken))
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("naver")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun signup(name: String, email: String, password: String): AuthResult = runCatching {
        val res = api.signup(SignupRequest(email, password, name))
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun refresh(): AuthResult = runCatching {
        val token = authDataStore.refreshToken.firstOrNull()
            ?: return AuthResult.Error("로그인이 필요합니다")
        val res = api.refresh(RefreshRequest(token))
        authDataStore.saveTokens(res.accessToken, res.refreshToken)
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun logout() {
        authDataStore.clearTokens()
    }

    suspend fun setSyncInterval(interval: String) =
        authDataStore.saveSyncInterval(interval)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        authDataStore.saveNotificationsEnabled(enabled)

    suspend fun setEnabledPackages(packages: Set<String>) =
        authDataStore.saveEnabledPackages(packages)

    private fun Throwable.toDisplayMessage(): String = when (this) {
        is HttpException -> try {
            JSONObject(response()?.errorBody()?.string() ?: "").getString("error")
        } catch (_: Exception) { "서버 오류가 발생했습니다" }
        else -> "네트워크 연결을 확인해주세요"
    }
}
