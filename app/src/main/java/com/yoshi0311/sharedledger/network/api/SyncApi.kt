package com.yoshi0311.sharedledger.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class HealthResponse(val status: String)

// ── Delta 응답 ────────────────────────────────────────────────────────────────

data class DeltaResponse(
    @SerializedName("server_time") val serverTime: String,
    @SerializedName("ledger_id")   val ledgerId: Long,
    val transactions: List<ServerTransactionDto>,
    val categories:   List<ServerCategoryDto>
)

data class ServerTransactionDto(
    @SerializedName("transaction_id")   val transactionId: Long,
    @SerializedName("ledger_id")        val ledgerId: Long,
    val amount: Double,
    val type: String,
    @SerializedName("category_id")      val categoryId: Long?,
    val description: String?,
    @SerializedName("transaction_date") val transactionDate: String, // "YYYY-MM-DD"
    @SerializedName("transaction_time") val transactionTime: String?, // "HH:mm" or null
    @SerializedName("is_deleted")       val isDeleted: Boolean,
    @SerializedName("created_at")       val createdAt: String,
    @SerializedName("updated_at")       val updatedAt: String
)

data class ServerCategoryDto(
    @SerializedName("category_id")   val categoryId: Long,
    @SerializedName("ledger_id")     val ledgerId: Long,
    @SerializedName("category_name") val categoryName: String,
    val color: String?,
    val type: String = "expense",
    @SerializedName("is_deleted")    val isDeleted: Boolean,
    @SerializedName("updated_at")    val updatedAt: String
)

// ── Push 요청/응답 ─────────────────────────────────────────────────────────────

data class PushRequest(
    @SerializedName("ledger_id") val ledgerId: Long,
    val transactions: List<PushTransactionDto>,
    val categories:   List<PushCategoryDto>
)

data class PushTransactionDto(
    @SerializedName("client_id")          val clientId: Long,
    @SerializedName("server_id")          val serverId: Long?,
    val amount: Long,
    val type: String,
    @SerializedName("category_server_id") val categoryServerId: Long?,
    val description: String,
    @SerializedName("transaction_date")   val transactionDate: String,
    @SerializedName("transaction_time")   val transactionTime: String?,
    @SerializedName("is_deleted")         val isDeleted: Boolean,
    @SerializedName("updated_at")         val updatedAt: String
)

data class PushCategoryDto(
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("server_id") val serverId: Long?,
    @SerializedName("category_name") val categoryName: String,
    val color: String?,
    val type: String,
    @SerializedName("is_deleted") val isDeleted: Boolean,
    @SerializedName("updated_at") val updatedAt: String
)

data class PushResponse(
    val transactions: List<PushResultDto>,
    val categories:   List<PushResultDto>
)

data class PushResultDto(
    @SerializedName("client_id")        val clientId: Long,
    @SerializedName("server_id")        val serverId: Long,
    @SerializedName("server_updated_at") val serverUpdatedAt: String
)

// ── Retrofit 인터페이스 ─────────────────────────────────────────────────────────

interface SyncApi {
    /** 서버 헬스 체크 (Render 서버 웜업용) */
    @GET("api/health")
    suspend fun ping(): HealthResponse

    /** 마지막 동기화 이후 변경된 데이터 조회 */
    @GET("api/sync/delta")
    suspend fun getDelta(
        @Query("since") since: String,
        @Query("ledger_id") ledgerId: Long
    ): DeltaResponse

    /** 로컬 변경사항 서버에 업로드 */
    @POST("api/sync/push")
    suspend fun push(@Body body: PushRequest): PushResponse
}
