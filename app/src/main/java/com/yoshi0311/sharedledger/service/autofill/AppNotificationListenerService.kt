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
        // 활성화된 패키지 목록을 DataStore에서 실시간 구독
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
        if (pkg !in enabledPackagesCache) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim() ?: return

        // 금액 패턴이 없으면 금융 알림 아님 → 무시
        if (!BaseKoreanFinanceParser.AMOUNT_REGEX.containsMatchIn(body)) return

        serviceScope.launch {
            autoFillRepository.insertPush(pkg, title, body, sbn.postTime)
        }
    }
}
