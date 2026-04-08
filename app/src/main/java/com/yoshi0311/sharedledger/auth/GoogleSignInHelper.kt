package com.yoshi0311.sharedledger.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInHelper {

    // Google Cloud Console > 웹 애플리케이션 OAuth 클라이언트 ID
    private const val WEB_CLIENT_ID = "61458654270-mi59saro5fqe4fio113jkjsio1ii4bj2.apps.googleusercontent.com"

    suspend fun getIdToken(context: Context): Result<String> {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(credential.idToken)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        }
    }
}
