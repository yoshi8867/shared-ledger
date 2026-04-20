package com.yoshi0311.sharedledger.di

import com.yoshi0311.sharedledger.network.ServerUrlProvider
import com.yoshi0311.sharedledger.network.api.AuthApi
import com.yoshi0311.sharedledger.network.api.SharedApi
import com.yoshi0311.sharedledger.network.api.SyncApi
import com.yoshi0311.sharedledger.network.interceptor.AuthInterceptor
import com.yoshi0311.sharedledger.network.interceptor.DynamicUrlInterceptor
import com.yoshi0311.sharedledger.network.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── Plain 클라이언트 (AuthApi 전용, 인증 없음) ─────────────────────────────
    // TokenAuthenticator → AuthApi → 이 클라이언트 → TokenAuthenticator 순환 참조 차단
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .build()
    }

    // ── Auth 클라이언트 (SyncApi / SharedApi 전용) ─────────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenAuthenticator)
            .build()
    }

    // ── Plain Retrofit (AuthApi 전용) ──────────────────────────────────────────
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainRetrofit(
        @Named("plain") client: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ServerUrlProvider.DEFAULT_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ── Auth Retrofit (SyncApi / SharedApi 전용) ───────────────────────────────
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(ServerUrlProvider.DEFAULT_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ── API 인스턴스 ───────────────────────────────────────────────────────────

    /** 로그인/회원가입/갱신 — plain 클라이언트 사용 (토큰 불필요) */
    @Provides
    @Singleton
    fun provideAuthApi(@Named("plain") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi =
        retrofit.create(SyncApi::class.java)

    @Provides
    @Singleton
    fun provideSharedApi(retrofit: Retrofit): SharedApi =
        retrofit.create(SharedApi::class.java)
}
