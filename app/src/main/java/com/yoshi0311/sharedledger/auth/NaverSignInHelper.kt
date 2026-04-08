package com.yoshi0311.sharedledger.auth

import android.app.Activity
import android.content.Context
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object NaverSignInHelper {

    // 네이버 개발자 센터에서 발급받은 값으로 교체
    const val CLIENT_ID     = "YOUR_NAVER_CLIENT_ID"
    const val CLIENT_SECRET = "YOUR_NAVER_CLIENT_SECRET"
    const val CLIENT_NAME   = "Shared Ledger"

    fun initialize(context: Context) {
        NaverIdLoginSDK.initialize(context, CLIENT_ID, CLIENT_SECRET, CLIENT_NAME)
    }

    suspend fun getAccessToken(context: Context): Result<String> =
        suspendCancellableCoroutine { cont ->
            val callback = object : OAuthLoginCallback {
                override fun onSuccess() {
                    val token = NaverIdLoginSDK.getAccessToken()
                    if (token != null) {
                        cont.resume(Result.success(token))
                    } else {
                        cont.resume(Result.failure(Exception("액세스 토큰을 가져올 수 없습니다")))
                    }
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    val error = NaverIdLoginSDK.getLastErrorDescription() ?: message
                    cont.resume(Result.failure(Exception(error)))
                }

                override fun onError(errorCode: Int, message: String) {
                    onFailure(errorCode, message)
                }
            }
            NaverIdLoginSDK.authenticate(context as Activity, callback)
        }
}
