package com.yoshi0311.sharedledger.data.repository

import com.yoshi0311.sharedledger.network.api.InviteCodeResponse
import com.yoshi0311.sharedledger.network.api.JoinLedgerRequest
import com.yoshi0311.sharedledger.network.api.JoinLedgerResponse
import com.yoshi0311.sharedledger.network.api.LedgerUpdateRequest
import com.yoshi0311.sharedledger.network.api.OwnLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedApi
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedUserDto
import com.yoshi0311.sharedledger.network.api.UpdatePermissionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedRepository @Inject constructor(
    private val api: SharedApi
) {
    suspend fun getSharedUsers(ledgerId: Long): Result<List<SharedUserDto>> =
        runCatching { api.getSharedUsers(ledgerId).sharedUsers }

    suspend fun updatePermission(sharedLedgerId: Long, permission: String): Result<Unit> =
        runCatching { api.updatePermission(sharedLedgerId, UpdatePermissionRequest(permission)) }

    suspend fun revokeAccess(sharedLedgerId: Long): Result<Unit> =
        runCatching { api.revokeAccess(sharedLedgerId) }

    suspend fun getSharedLedgers(): Result<List<SharedLedgerDto>> =
        runCatching { api.getSharedLedgers().ledgers }

    suspend fun getMyLedgers(): Result<List<OwnLedgerDto>> =
        runCatching { api.getMyLedgers().ledgers }

    suspend fun getLedger(ledgerId: Long): Result<OwnLedgerDto> =
        runCatching { api.getLedger(ledgerId).ledger }

    suspend fun renameLedger(ledgerId: Long, name: String): Result<OwnLedgerDto> =
        runCatching { api.updateLedger(ledgerId, LedgerUpdateRequest(name)).ledger }

    suspend fun generateInviteCode(ledgerId: Long): Result<InviteCodeResponse> =
        runCatching { api.generateInviteCode(ledgerId) }

    suspend fun joinLedger(inviteCode: String): Result<JoinLedgerResponse> =
        runCatching { api.joinLedger(JoinLedgerRequest(inviteCode)) }
}
