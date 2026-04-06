package com.yoshi0311.sharedledger.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class SignupRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

interface AuthApi {
    @POST("api/auth/signup")
    suspend fun signup(@Body req: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): AuthResponse
}
