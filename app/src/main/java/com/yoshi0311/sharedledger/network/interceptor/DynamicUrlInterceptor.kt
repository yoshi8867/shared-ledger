package com.yoshi0311.sharedledger.network.interceptor

import com.yoshi0311.sharedledger.network.ServerUrlProvider
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicUrlInterceptor @Inject constructor(
    private val provider: ServerUrlProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        return try {
            val base = provider.baseUrl.trimEnd('/').toHttpUrl()
            val newUrl = req.url.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
                .build()
            chain.proceed(req.newBuilder().url(newUrl).build())
        } catch (_: Exception) {
            chain.proceed(req)
        }
    }
}
