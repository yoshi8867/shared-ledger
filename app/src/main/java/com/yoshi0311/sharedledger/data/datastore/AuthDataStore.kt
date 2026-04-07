package com.yoshi0311.sharedledger.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yoshi0311.sharedledger.network.ServerUrlProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class AuthDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val ACCESS_TOKEN          = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN         = stringPreferencesKey("refresh_token")
    private val LEDGER_ID             = longPreferencesKey("ledger_id")
    private val ACTIVE_LEDGER_ID      = longPreferencesKey("active_ledger_id")
    private val LAST_SYNCED_AT        = stringPreferencesKey("last_synced_at")
    private val SERVER_URL            = stringPreferencesKey("server_url")
    private val SYNC_INTERVAL         = stringPreferencesKey("sync_interval")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val ENABLED_PACKAGES      = stringPreferencesKey("enabled_packages")

    val accessToken:         Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken:        Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val ledgerId:            Flow<Long?>   = context.dataStore.data.map { it[LEDGER_ID] }
    val activeLedgerId:      Flow<Long?>   = context.dataStore.data.map { it[ACTIVE_LEDGER_ID] }
    val lastSyncedAt:        Flow<String?> = context.dataStore.data.map { it[LAST_SYNCED_AT] }
    val serverUrl:           Flow<String>  = context.dataStore.data.map { it[SERVER_URL] ?: ServerUrlProvider.DEFAULT_URL }
    val syncInterval:        Flow<String>  = context.dataStore.data.map { it[SYNC_INTERVAL] ?: "manual" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val enabledPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[ENABLED_PACKAGES]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: com.yoshi0311.sharedledger.service.autofill.NotificationParserRegistry.DEFAULT_ENABLED
    }

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

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(LEDGER_ID)
            prefs.remove(LAST_SYNCED_AT)
        }
    }

    suspend fun saveSyncInterval(interval: String) {
        context.dataStore.edit { it[SYNC_INTERVAL] = interval }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun saveEnabledPackages(packages: Set<String>) {
        context.dataStore.edit { it[ENABLED_PACKAGES] = packages.joinToString(",") }
    }
}
