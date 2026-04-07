package com.yoshi0311.sharedledger.ui.screens.autofill

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.data.db.entity.PendingNotificationEntity
import com.yoshi0311.sharedledger.service.autofill.NotificationParserRegistry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoFillScreen(
    onNavigateBack: () -> Unit,
    viewModel: AutoFillViewModel = hiltViewModel()
) {
    val selectedSource  by viewModel.selectedSource.collectAsStateWithLifecycle()
    val pushItems       by viewModel.pushItems.collectAsStateWithLifecycle()
    val smsItems        by viewModel.smsItems.collectAsStateWithLifecycle()
    val enabledPackages by viewModel.enabledPackages.collectAsStateWithLifecycle()
    val installedApps   by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps   by viewModel.isLoadingApps.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPackageModal by remember { mutableStateOf(false) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isNotificationListenerEnabled by remember {
        mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName))
    }

    // 다이얼로그가 열릴 때 앱 목록 로드
    LaunchedEffect(showPackageModal) {
        if (showPackageModal) viewModel.loadInstalledApps()
    }

    // Settings에서 돌아오면 알림 권한 재확인
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationListenerEnabled =
                    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) viewModel.selectSource("sms")
    }

    val items = if (selectedSource == "push") pushItems else smsItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자동 입력") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (selectedSource == "push") {
                        IconButton(onClick = { showPackageModal = true }) {
                            Icon(Icons.Filled.Tune, contentDescription = "수집 앱 설정")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 소스 토글 ────────────────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedSource == "push",
                    onClick  = { viewModel.selectSource("push") },
                    shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon     = {
                        Icon(Icons.Filled.NotificationsActive, null, Modifier.size(16.dp))
                    }
                ) { Text("푸시 알림") }

                SegmentedButton(
                    selected = selectedSource == "sms",
                    onClick  = {
                        if (!hasSmsPermission) {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        } else {
                            viewModel.selectSource("sms")
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon  = { Icon(Icons.Filled.Message, null, Modifier.size(16.dp)) }
                ) { Text("문자메시지") }
            }

            // ── 알림 권한 경고 (푸시 탭) ──────────────────────────────────
            if (selectedSource == "push" && !isNotificationListenerEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "알림 접근 권한이 필요합니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }) {
                            Text("설정에서 활성화")
                        }
                    }
                }
            }

            // ── 목록 ─────────────────────────────────────────────────────────
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "수집된 항목이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        PendingNotificationItem(
                            item      = item,
                            onApprove = { amount, type, date, desc ->
                                viewModel.approve(item, amount, type, date, desc)
                            },
                            onReject  = { viewModel.reject(item.id) }
                        )
                    }
                }
            }
        }
    }

    // ── 수집 앱 선택 다이얼로그 ──────────────────────────────────────────────
    if (showPackageModal) {
        AppSelectionDialog(
            enabledPackages = enabledPackages,
            installedApps   = installedApps,
            isLoadingApps   = isLoadingApps,
            onToggle        = { viewModel.togglePackage(it) },
            onDismiss       = { showPackageModal = false }
        )
    }
}

// ── 앱 선택 다이얼로그 (설치된 앱 목록 + 검색 + 필터) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionDialog(
    enabledPackages: Set<String>,
    installedApps: List<InstalledApp>,
    isLoadingApps: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery    by remember { mutableStateOf("") }
    var financialFilter by remember { mutableIntStateOf(0) } // 0=전체 1=금융앱 2=비금융앱
    var enabledFilter  by remember { mutableIntStateOf(0) }  // 0=전체 1=수신ON 2=수신OFF

    val filtered = remember(installedApps, searchQuery, financialFilter, enabledFilter, enabledPackages) {
        installedApps.filter { app ->
            val matchSearch = searchQuery.isBlank() ||
                    app.displayName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchFinancial = when (financialFilter) {
                1    -> app.isFinancial
                2    -> !app.isFinancial
                else -> true
            }
            val matchEnabled = when (enabledFilter) {
                1    -> app.packageName in enabledPackages
                2    -> app.packageName !in enabledPackages
                else -> true
            }
            matchSearch && matchFinancial && matchEnabled
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape         = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 제목
                Text(
                    "수집 대상 앱",
                    style    = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 검색창
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("앱 이름 또는 패키지명") },
                    leadingIcon   = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon  = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // 금융 필터 chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("전체", "금융앱", "비금융앱").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = financialFilter == idx,
                            onClick  = { financialFilter = idx },
                            label    = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 수신 필터 chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("전체", "수신 ON", "수신 OFF").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = enabledFilter == idx,
                            onClick  = { enabledFilter = idx },
                            label    = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()

                // 앱 목록 or 로딩
                if (isLoadingApps) {
                    Box(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment  = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggle(app.packageName) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked         = app.packageName in enabledPackages,
                                    onCheckedChange = { onToggle(app.packageName) }
                                )
                                AppIcon(drawable = app.icon, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        app.displayName,
                                        style    = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        app.packageName,
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (app.isFinancial) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "금융",
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 닫기 버튼
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

// ── 앱 아이콘 렌더러 ─────────────────────────────────────────────────────────

@Composable
private fun AppIcon(drawable: android.graphics.drawable.Drawable?, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        drawable?.let {
            val w = it.intrinsicWidth.takeIf  { w -> w > 0 } ?: 48
            val h = it.intrinsicHeight.takeIf { h -> h > 0 } ?: 48
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bmp.asImageBitmap()
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier)
    }
}

// ── 개별 알림 아이템 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingNotificationItem(
    item: PendingNotificationEntity,
    onApprove: (amount: Long, type: String, date: Date, description: String) -> Unit,
    onReject: () -> Unit
) {
    var selectedType by remember(item.id) { mutableStateOf(item.parsedType ?: "expense") }
    var amountText   by remember(item.id) { mutableStateOf(item.parsedAmount?.toString() ?: "") }
    var selectedDate by remember(item.id) { mutableStateOf(item.parsedDate ?: Date()) }
    var description  by remember(item.id) { mutableStateOf(item.parsedDescription ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
    val dateFmt  = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN) }
    val timeFmt  = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREAN) }

    val isAmountValid = amountText.toLongOrNull()?.let { it > 0 } == true

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // 헤더: 소스 아이콘 + 앱/발신자명 + 수신 시각
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = if (item.source == "push") Icons.Filled.NotificationsActive else Icons.Filled.Message,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = NotificationParserRegistry.displayName(item.packageName),
                    style    = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = timeFmt.format(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            // 원본 텍스트 (작은 글씨)
            Text(
                text     = buildString {
                    if (!item.title.isNullOrBlank()) append("[${item.title}] ")
                    append(item.body)
                },
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 수입 / 지출 토글
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == "income",
                    onClick  = { selectedType = "income" },
                    shape    = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("수입") }
                SegmentedButton(
                    selected = selectedType == "expense",
                    onClick  = { selectedType = "expense" },
                    shape    = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("지출") }
            }

            Spacer(Modifier.height(8.dp))

            // 금액 입력
            OutlinedTextField(
                value         = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label         = { Text("금액") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon  = { Text("원", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp)) },
                isError       = amountText.isNotBlank() && !isAmountValid
            )

            Spacer(Modifier.height(8.dp))

            // 날짜 (클릭하면 DatePicker)
            Box {
                OutlinedTextField(
                    value         = dateFmt.format(selectedDate),
                    onValueChange = {},
                    label         = { Text("날짜") },
                    modifier      = Modifier.fillMaxWidth(),
                    readOnly      = true,
                    enabled       = false,
                    trailingIcon  = { Icon(Icons.Filled.CalendarMonth, null) },
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledTextColor         = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor        = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor       = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            Spacer(Modifier.height(8.dp))

            // 내용 입력
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("내용") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            Spacer(Modifier.height(8.dp))

            // 취소 / 저장 버튼
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onReject,
                    modifier = Modifier.weight(1f)
                ) { Text("취소") }

                Button(
                    onClick  = {
                        val amt = amountText.toLongOrNull() ?: return@Button
                        onApprove(amt, selectedType, selectedDate, description)
                    },
                    enabled  = isAmountValid,
                    modifier = Modifier.weight(1f)
                ) { Text("저장") }
            }
        }
    }

    // DatePicker 다이얼로그
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = Date(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
