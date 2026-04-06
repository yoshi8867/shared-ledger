package com.yoshi0311.sharedledger.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ── DTOs ─────────────────────────────────────────────────────────────────────

data class SharedUserDto(
    @SerializedName("shared_ledger_id") val sharedLedgerId: Long,
    @SerializedName("permission")       val permission: String,
    @SerializedName("user_id")          val userId: Long,
    @SerializedName("email")            val email: String,
    @SerializedName("name")             val name: String
)

data class SharedLedgerDto(
    @SerializedName("ledger_id")        val ledgerId: Long,
    @SerializedName("ledger_name")      val ledgerName: String,
    @SerializedName("owner_id")         val ownerId: Long,
    @SerializedName("owner_name")       val ownerName: String,
    @SerializedName("owner_email")      val ownerEmail: String,
    @SerializedName("permission")       val permission: String,
    @SerializedName("shared_ledger_id") val sharedLedgerId: Long
)

data class OwnLedgerDto(
    @SerializedName("ledger_id")   val ledgerId: Long,
    @SerializedName("ledger_name") val ledgerName: String,
    @SerializedName("owner_id")    val ownerId: Long
)

data class UserSearchDto(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("email")   val email: String,
    @SerializedName("name")    val name: String
)

// ── Requests ─────────────────────────────────────────────────────────────────

data class InviteRequest(
    @SerializedName("ledger_id")  val ledgerId: Long,
    @SerializedName("email")      val email: String,
    @SerializedName("permission") val permission: String = "view"
)

data class UpdatePermissionRequest(
    @SerializedName("permission") val permission: String
)

// ── Response wrappers ────────────────────────────────────────────────────────

data class SharedUsersResponse(
    @SerializedName("shared_users") val sharedUsers: List<SharedUserDto>
)

data class SharedLedgersResponse(
    @SerializedName("ledgers") val ledgers: List<SharedLedgerDto>
)

data class OwnLedgersResponse(
    @SerializedName("ledgers") val ledgers: List<OwnLedgerDto>
)

data class UserSearchResponse(
    @SerializedName("user") val user: UserSearchDto
)

// ── Retrofit 인터페이스 ────────────────────────────────────────────────────────

interface SharedApi {
    /** 특정 장부의 공유 사용자 목록 (소유자만) */
    @GET("api/shared-ledgers")
    suspend fun getSharedUsers(@Query("ledger_id") ledgerId: Long): SharedUsersResponse

    /** 이메일로 사용자 초대 */
    @POST("api/shared-ledgers")
    suspend fun invite(@Body request: InviteRequest): Any

    /** 권한 변경 */
    @PATCH("api/shared-ledgers/{id}")
    suspend fun updatePermission(
        @Path("id") id: Long,
        @Body request: UpdatePermissionRequest
    ): Any

    /** 공유 해제 */
    @DELETE("api/shared-ledgers/{id}")
    suspend fun revokeAccess(@Path("id") id: Long): Any

    /** 나와 공유된 장부 목록 */
    @GET("api/ledgers/shared")
    suspend fun getSharedLedgers(): SharedLedgersResponse

    /** 내가 소유한 장부 목록 */
    @GET("api/ledgers")
    suspend fun getMyLedgers(): OwnLedgersResponse

    /** 이메일로 사용자 검색 */
    @GET("api/users/search")
    suspend fun searchUser(@Query("email") email: String): UserSearchResponse
}
