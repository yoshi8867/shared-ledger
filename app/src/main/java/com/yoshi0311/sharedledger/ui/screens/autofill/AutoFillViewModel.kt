package com.yoshi0311.sharedledger.ui.screens.autofill

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.sharedledger.data.datastore.AuthDataStore
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.PendingNotificationEntity
import com.yoshi0311.sharedledger.data.repository.AuthRepository
import com.yoshi0311.sharedledger.data.repository.AutoFillRepository
import com.yoshi0311.sharedledger.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val displayName: String,
    val isFinancial: Boolean,
    val icon: Drawable? = null
)

@HiltViewModel
class AutoFillViewModel @Inject constructor(
    private val autoFillRepo: AutoFillRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    private val authDataStore: AuthDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedSource = MutableStateFlow("push")
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    val pushItems: StateFlow<List<PendingNotificationEntity>> = autoFillRepo.getPendingPush()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val smsItems: StateFlow<List<PendingNotificationEntity>> = autoFillRepo.getPendingSms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enabledPackages: StateFlow<Set<String>> = authDataStore.enabledPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = authRepo.ledgerId
        .map { it ?: 1L }
        .flatMapLatest { ledgerId -> categoryRepo.getByLedgerId(ledgerId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    fun selectSource(source: String) {
        _selectedSource.value = source
        if (source == "sms") loadSmsItems()
    }

    fun loadSmsItems() {
        viewModelScope.launch(Dispatchers.IO) {
            autoFillRepo.loadSmsItems()
        }
    }

    fun approve(
        item: PendingNotificationEntity,
        amount: Long,
        type: String,
        date: Date,
        description: String,
        categoryId: Long? = null
    ) {
        viewModelScope.launch {
            autoFillRepo.approve(item, amount, type, date, description, categoryId)
        }
    }

    fun addCategory(name: String, color: String) {
        viewModelScope.launch {
            val ledgerId = authRepo.ledgerId.firstOrNull() ?: 1L
            categoryRepo.insert(CategoryEntity(ledgerId = ledgerId, name = name, color = color))
        }
    }

    fun reject(id: Long) {
        viewModelScope.launch {
            autoFillRepo.reject(id)
        }
    }

    fun togglePackage(packageName: String) {
        viewModelScope.launch {
            val current = enabledPackages.value.toMutableSet()
            if (packageName in current) current.remove(packageName) else current.add(packageName)
            authDataStore.saveEnabledPackages(current)
        }
    }

    fun loadInstalledApps() {
        if (_isLoadingApps.value || _installedApps.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    val pkg = appInfo.packageName
                    pkg != context.packageName &&
                    !pkg.startsWith("com.android.") &&
                    !pkg.startsWith("com.google.") &&
                    !pkg.startsWith("com.spec.") &&
                    !pkg.startsWith("com.samsung.") &&
                    !pkg.startsWith("com.samsung.android.") &&
                    !pkg.startsWith("com.samsung.android.knox.") &&
                    !pkg.startsWith("com.sec.android.")
                }
                .mapNotNull { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon  = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    InstalledApp(
                        packageName = appInfo.packageName,
                        displayName = label,
                        isFinancial = isFinancialApp(appInfo.packageName, label),
                        icon        = icon
                    )
                }
                .sortedWith(
                    compareByDescending<InstalledApp> { it.isFinancial }
                        .thenBy { it.displayName }
                )
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    private fun isFinancialApp(packageName: String, label: String): Boolean {
        val pkgLower   = packageName.lowercase()
        val labelLower = label.lowercase()
        return FINANCIAL_PKG_KEYWORDS.any { pkgLower.contains(it) } ||
                FINANCIAL_LABEL_KEYWORDS.any { labelLower.contains(it) }
    }

    companion object {
        private val FINANCIAL_PKG_KEYWORDS = setOf(
            "bank", "card", "finance", "fintech", "pay", "money", "wallet",
            "loan", "invest", "stock", "securities", "insurance",
            "kbstar", "kbbank", "kbcard", "shinhan", "hanabank", "hanacard",
            "wooribank", "wooricard", "nhbank", "nonghyup", "ibk", "toss",
            "kakaobank", "kakaopay", "bccard", "lottecard", "samsungcard",
            "hyundaicard", "citibank", "postbank", "saemaul", "kfcc",
            "kbank", "bankofkorea", "savingsbank", "imsb", "meritz",
            "hanwha", "kyobo", "mirae", "samsung.securities"
        )
        private val FINANCIAL_LABEL_KEYWORDS = setOf(
            "은행", "카드", "금융", "페이", "뱅킹", "머니", "증권",
            "보험", "투자", "저축", "대출", "환전", "송금", "앱카드", "주식"
        )
    }
}
