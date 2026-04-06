package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.network.api.InviteRequest
import com.yoshi0311.sharedledger.network.api.OwnLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedApi
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedUserDto
import com.yoshi0311.sharedledger.network.api.UpdatePermissionRequest
import com.yoshi0311.sharedledger.network.api.UserSearchDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedRepository @Inject constructor(
    private val api: SharedApi
) {
    suspend fun getSharedUsers(ledgerId: Long): Result<List<SharedUserDto>> =
        runCatching { api.getSharedUsers(ledgerId).sharedUsers }

    suspend fun invite(ledgerId: Long, email: String, permission: String): Result<Unit> =
        runCatching { api.invite(InviteRequest(ledgerId, email, permission)); Unit }

    suspend fun updatePermission(sharedLedgerId: Long, permission: String): Result<Unit> =
        runCatching { api.updatePermission(sharedLedgerId, UpdatePermissionRequest(permission)); Unit }

    suspend fun revokeAccess(sharedLedgerId: Long): Result<Unit> =
        runCatching { api.revokeAccess(sharedLedgerId); Unit }

    suspend fun getSharedLedgers(): Result<List<SharedLedgerDto>> =
        runCatching { api.getSharedLedgers().ledgers }

    suspend fun getMyLedgers(): Result<List<OwnLedgerDto>> =
        runCatching { api.getMyLedgers().ledgers }

    suspend fun searchUser(email: String): Result<UserSearchDto> =
        runCatching { api.searchUser(email).user }
}
