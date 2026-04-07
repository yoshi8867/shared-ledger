package com.yoshi0311.sharedledger.service.autofill

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
        Log.d(TAG, "onListenerConnected — 서비스 연결됨")
        serviceScope.launch {
            authDataStore.enabledPackages.collect { pkgs ->
                Log.d(TAG, "enabledPackages 업데이트: $pkgs")
                enabledPackagesCache = pkgs
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected — 서비스 연결 끊김")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        Log.d(TAG, "onNotificationPosted — pkg=$pkg")

        val extras = sbn.notification?.extras ?: run {
            Log.d(TAG, "[$pkg] extras == null → skip")
            return
        }

        // 사용 가능한 extra 키 목록 출력 (어떤 키에 텍스트가 있는지 확인용)
        val extraKeys = extras.keySet().joinToString()
        Log.d(TAG, "[$pkg] extra keys: $extraKeys")

        val bigText  = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text     = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val lines    = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ")
        val subText  = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val title    = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        Log.d(TAG, "[$pkg] title=$title")
        Log.d(TAG, "[$pkg] EXTRA_BIG_TEXT=$bigText")
        Log.d(TAG, "[$pkg] EXTRA_TEXT=$text")
        Log.d(TAG, "[$pkg] EXTRA_TEXT_LINES=$lines")
        Log.d(TAG, "[$pkg] EXTRA_SUB_TEXT=$subText")

        val body = (bigText ?: text ?: lines)?.trim() ?: run {
            Log.w(TAG, "[$pkg] body 추출 실패 (모든 text extra가 null) → skip")
            return
        }

        val hasAmount = BaseKoreanFinanceParser.AMOUNT_REGEX.containsMatchIn(body)
        Log.d(TAG, "[$pkg] body=$body | hasAmount=$hasAmount")

        if (!hasAmount) {
            Log.d(TAG, "[$pkg] 금액 패턴 없음 → skip")
            return
        }

        serviceScope.launch {
            val enabled = enabledPackagesCache.ifEmpty {
                Log.d(TAG, "[$pkg] 캐시 비어있음 → DataStore에서 직접 읽기")
                authDataStore.enabledPackages.first()
            }
            Log.d(TAG, "[$pkg] enabledPackages=$enabled | pkgEnabled=${pkg in enabled}")
            if (pkg !in enabled) {
                Log.d(TAG, "[$pkg] 수신 비활성화 패키지 → skip")
                return@launch
            }
            Log.d(TAG, "[$pkg] insertPush 호출 — amount regex matched, pkg enabled")
            autoFillRepository.insertPush(pkg, title, body, sbn.postTime)
        }
    }

    companion object {
        private const val TAG = "AutoFill"
    }
}
