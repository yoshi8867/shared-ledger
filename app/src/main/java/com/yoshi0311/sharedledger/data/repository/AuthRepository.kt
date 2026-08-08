package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.data.db.dao.CategoryDao
import com.yoshi0311.sharedledger.data.db.dao.TransactionDao
import com.yoshi0311.sharedledger.network.api.AuthApi
import com.yoshi0311.sharedledger.network.api.GoogleLoginRequest
import com.yoshi0311.sharedledger.network.api.LoginRequest
import com.yoshi0311.sharedledger.network.api.NaverLoginRequest
import com.yoshi0311.sharedledger.network.api.RefreshRequest
import com.yoshi0311.sharedledger.network.api.SignupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val authDataStore: AuthDataStore,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    companion object {
        /** 게스트(로그인 안 함)가 로컬로만 쓸 때 사용하는 임시 장부 ID. 서버 BIGSERIAL은 1부터라 절대 충돌 없음 */
        const val GUEST_LEDGER_ID = 0L
    }

    val accessToken:          Flow<String?> = authDataStore.accessToken
    val ledgerId:             Flow<Long?>   = authDataStore.ledgerId
    val activeLedgerId:       Flow<Long?>   = authDataStore.activeLedgerId
    val serverUrl:            Flow<String>  = authDataStore.serverUrl
    val syncInterval:         Flow<String>  = authDataStore.syncInterval
    val notificationsEnabled: Flow<Boolean> = authDataStore.notificationsEnabled
    val enabledPackages:      Flow<Set<String>> = authDataStore.enabledPackages
    val lastLoginMethod:      Flow<String?> = authDataStore.lastLoginMethod
    val guestMode:            Flow<Boolean> = authDataStore.guestMode

    suspend fun enterGuestMode() = authDataStore.setGuestMode(true)

    /**
     * 현재 활성 장부 ID. 모든 화면이 이걸 써야 데이터가 한 장부에 모인다.
     * 마지막에 쓰던 장부(activeLedgerId/ledgerId)가 있으면 게스트든 아니든 그대로 사용해
     * 로컬에 남은 이전 데이터를 이어서 보여준다. 한 번도 쓴 적 없는 진짜 신규만
     * 게스트=GUEST_LEDGER_ID / 로그인=1L로 폴백한다.
     */
    suspend fun resolveLedgerId(): Long =
        (activeLedgerId.firstOrNull() ?: ledgerId.firstOrNull())
            ?: if (authDataStore.guestMode.firstOrNull() == true) GUEST_LEDGER_ID else 1L

    /** resolveLedgerId의 reactive 버전 — 게스트/장부 전환에 따라 갱신 */
    val currentLedgerIdFlow: Flow<Long> = combine(
        authDataStore.guestMode, authDataStore.activeLedgerId
    ) { guest, active ->
        (active ?: authDataStore.ledgerId.firstOrNull())
            ?: if (guest) GUEST_LEDGER_ID else 1L
    }

    suspend fun setActiveLedgerId(id: Long) = authDataStore.setActiveLedgerId(id)

    /**
     * 로그인/가입 성공 시 호출. 게스트 장부(GUEST_LEDGER_ID)에 남은 로컬 거래·카테고리가 있으면
     * 로그인 계정의 장부로 옮기고 pending으로 재표시한다(첫 동기화에서 서버로 push).
     * 게스트 데이터가 없으면 이관 쿼리는 0행 갱신으로 무해하다.
     */
    private suspend fun migrateGuestDataIfNeeded(newLedgerId: Long?) {
        if (newLedgerId != null) {
            categoryDao.reassignLedger(GUEST_LEDGER_ID, newLedgerId)
            transactionDao.reassignLedger(GUEST_LEDGER_ID, newLedgerId)
        }
        authDataStore.setGuestMode(false)
    }

    suspend fun setServerUrl(url: String) = authDataStore.saveServerUrl(url)

    suspend fun isLoggedIn(): Boolean =
        authDataStore.accessToken.firstOrNull() != null

    suspend fun login(email: String, password: String): AuthResult = runCatching {
        val res = api.login(LoginRequest(email, password))
        migrateGuestDataIfNeeded(res.ledgerId)
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("email")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun loginWithGoogle(idToken: String): AuthResult = runCatching {
        val res = api.loginWithGoogle(GoogleLoginRequest(idToken))
        migrateGuestDataIfNeeded(res.ledgerId)
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("google")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun loginWithNaver(accessToken: String): AuthResult = runCatching {
        val res = api.loginWithNaver(NaverLoginRequest(accessToken))
        migrateGuestDataIfNeeded(res.ledgerId)
        authDataStore.saveTokens(res.accessToken, res.refreshToken, res.ledgerId)
        authDataStore.saveLastLoginMethod("naver")
        AuthResult.Success
    }.getOrElse { e -> AuthResult.Error(e.toDisplayMessage()) }

    suspend fun signup(name: String, email: String, password: String): AuthResult = runCatching {
        val res = api.signup(SignupRequest(email, password, name))
        migrateGuestDataIfNeeded(res.ledgerId)
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
        authDataStore.setGuestMode(false)
    }

    suspend fun setSyncInterval(interval: String) =
        authDataStore.saveSyncInterval(interval)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        authDataStore.saveNotificationsEnabled(enabled)

    suspend fun setEnabledPackages(packages: Set<String>) =
        authDataStore.saveEnabledPackages(packages)

    fun getTrendCategories(type: String): Flow<Set<String>> =
        authDataStore.getTrendCategories(type)

    suspend fun setTrendCategories(type: String, names: Set<String>) =
        authDataStore.saveTrendCategories(type, names)

    private fun Throwable.toDisplayMessage(): String = when (this) {
        is HttpException -> try {
            JSONObject(response()?.errorBody()?.string() ?: "").getString("error")
        } catch (_: Exception) { "서버 오류가 발생했습니다" }
        else -> "네트워크 연결을 확인해주세요"
    }
}
