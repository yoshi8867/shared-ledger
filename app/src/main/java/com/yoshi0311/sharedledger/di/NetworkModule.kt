package com.yoshi0311.sharedledger.di

import com.yoshi0311.sharedledger.network.ServerUrlProvider
import com.yoshi0311.sharedledger.network.api.AuthApi
import com.yoshi0311.sharedledger.network.api.SharedApi
import com.yoshi0311.sharedledger.network.api.SyncApi
import com.yoshi0311.sharedledger.network.interceptor.AuthInterceptor
import com.yoshi0311.sharedledger.network.interceptor.DynamicUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicUrlInterceptor: DynamicUrlInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor) // URL 교체 (인증 헤더보다 먼저)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(ServerUrlProvider.DEFAULT_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
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
