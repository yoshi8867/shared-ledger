package com.yoshi0311.sharedledger.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class AuthDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val ACCESS_TOKEN     = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN    = stringPreferencesKey("refresh_token")
    private val LEDGER_ID        = longPreferencesKey("ledger_id")
    private val ACTIVE_LEDGER_ID = longPreferencesKey("active_ledger_id")
    private val LAST_SYNCED_AT   = stringPreferencesKey("last_synced_at")

    val accessToken:     Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken:    Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val ledgerId:        Flow<Long?>   = context.dataStore.data.map { it[LEDGER_ID] }
    val activeLedgerId:  Flow<Long?>   = context.dataStore.data.map { it[ACTIVE_LEDGER_ID] }
    val lastSyncedAt:    Flow<String?> = context.dataStore.data.map { it[LAST_SYNCED_AT] }

    suspend fun saveTokens(accessToken: String, refreshToken: String, ledgerId: Long? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN]  = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            if (ledgerId != null) {
                prefs[LEDGER_ID]        = ledgerId
                prefs[ACTIVE_LEDGER_ID] = ledgerId // 로그인 시 활성 장부 = 내 장부
            }
        }
    }

    suspend fun setActiveLedgerId(id: Long) {
        context.dataStore.edit { it[ACTIVE_LEDGER_ID] = id }
    }

    suspend fun saveLastSyncedAt(isoTimestamp: String) {
        context.dataStore.edit { it[LAST_SYNCED_AT] = isoTimestamp }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(LEDGER_ID)
            prefs.remove(LAST_SYNCED_AT)
        }
    }
}
