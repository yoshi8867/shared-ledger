package com.yoshi0311.sharedledger.network.interceptor

import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.network.api.AuthApi
import com.yoshi0311.sharedledger.network.api.RefreshRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val authApi: AuthApi   // plain OkHttpClient로 만든 AuthApi — 순환 참조 없음
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // refresh 엔드포인트 자체가 401이면 루프 방지
        if (response.request.url.encodedPath.contains("/auth/refresh")) return null
        // 동일 요청에서 이미 재시도했으면 포기
        if (response.responseCount >= 2) return null

        val newAccessToken = runBlocking {
            val refreshToken = authDataStore.refreshToken.firstOrNull() ?: return@runBlocking null
            runCatching {
                val res = authApi.refresh(RefreshRequest(refreshToken))
                authDataStore.saveTokens(res.accessToken, res.refreshToken)
                res.accessToken
            }.getOrNull()
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    /** priorResponse 체인을 따라 재시도 횟수를 센다 */
    private val Response.responseCount: Int
        get() {
            var count = 1
            var prior = priorResponse
            while (prior != null) { count++; prior = prior.priorResponse }
            return count
        }
}
