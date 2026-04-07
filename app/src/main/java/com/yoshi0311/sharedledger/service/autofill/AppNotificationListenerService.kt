package com.yoshi0311.sharedledger.service.autofill

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.data.repository.AutoFillRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var autoFillRepository: AutoFillRepository
    @Inject lateinit var authDataStore: AuthDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var enabledPackagesCache: Set<String> = emptySet()

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            authDataStore.enabledPackages.collect { pkgs ->
                enabledPackagesCache = pkgs
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg in BLOCKED_PACKAGES) return

        val extras = sbn.notification?.extras ?: return
        val title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text    = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val lines   = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ")
        val body    = (bigText ?: text ?: lines)?.trim() ?: return

        serviceScope.launch {
            val enabled = enabledPackagesCache.ifEmpty { authDataStore.enabledPackages.first() }
            if (pkg !in enabled) return@launch
            autoFillRepository.insertPush(pkg, title, body, sbn.postTime)
        }
    }

    companion object {
        private val BLOCKED_PACKAGES = setOf("com.android.systemui")
    }
}
