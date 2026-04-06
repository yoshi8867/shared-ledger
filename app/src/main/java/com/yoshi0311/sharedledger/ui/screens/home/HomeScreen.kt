package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.ui.components.MonthSelectorBar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactionEdit: (id: Long, dateMillis: Long) -> Unit = { _, _ -> },
    onNavigateToSharedLedger: (ledgerId: Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCalendarDay by viewModel.selectedCalendarDay.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val ledgers   by viewModel.ledgers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showLedgerSheet by rememberSaveable { mutableStateOf(false) }

    // 활성 장부 이름 계산
    val activeLedgerId = viewModel.currentLedgerId.collectAsStateWithLifecycle().value
    val activeLedgerName = ledgers.find { it.ledgerId == activeLedgerId }?.ledgerName ?: "내 장부"

    LaunchedEffect(Unit) { viewModel.loadLedgers() }

    LaunchedEffect(syncState) {
        when (val s = syncState) {
            is SyncState.Success -> {
                snackbarHostState.showSnackbar("동기화 완료")
                viewModel.resetSyncState()
            }
            is SyncState.Error -> {
                snackbarHostState.showSnackbar("동기화 실패: ${s.message}")
                viewModel.resetSyncState()
            }
            else -> Unit
        }
    }

    val tabs = listOf("목록", "캘린더", "통계")
    val tabIcons = listOf(
        Icons.Filled.List,
        Icons.Filled.CalendarMonth,
        Icons.Filled.PieChart
    )

    // 오늘 날짜 계산 (캘린더 탭용)
    val today = remember { Calendar.getInstance() }
    val isCurrentMonth = selectedMonth.year == today.get(Calendar.YEAR) &&
            selectedMonth.month == today.get(Calendar.MONTH) + 1
    val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    // 캘린더 탭에서 표시할 선택 날짜: ViewModel 값이 없으면 오늘(현재월) 또는 null
    val effectiveCalendarDay = selectedCalendarDay
        ?: if (isCurrentMonth) todayDay else null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            viewModel.loadLedgers()
                            scope.launch { showLedgerSheet = true }
                        }
                    ) {
                        Text(
                            text = activeLedgerName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedMonth.displayLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // 설정 버튼
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "설정")
                    }
                    // 공유 관리 버튼
                    IconButton(onClick = { onNavigateToSharedLedger(activeLedgerId) }) {
                        Icon(imageVector = Icons.Filled.People, contentDescription = "공유 관리")
                    }
                    // 동기화 버튼
                    IconButton(
                        onClick = { viewModel.sync() },
                        enabled = syncState !is SyncState.Loading
                    ) {
                        if (syncState is SyncState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Filled.Sync, contentDescription = "동기화")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToTransactionEdit(-1L, -1L) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "거래 추가",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthSelectorBar(
                currentMonth = selectedMonth,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() }
            )

            SummaryCard(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense
            )

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(imageVector = tabIcons[index], contentDescription = title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ListViewTab(
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    isLoading = uiState.isLoading,
                    onTransactionClick = { tx -> onNavigateToTransactionEdit(tx.id, -1L) }
                )
                1 -> CalendarViewTab(
                    yearMonth = selectedMonth,
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    isLoading = uiState.isLoading,
                    selectedDay = effectiveCalendarDay,
                    todayDay = todayDay,
                    onDaySelected = { viewModel.selectCalendarDay(it) },
                    onTransactionClick = { tx -> onNavigateToTransactionEdit(tx.id, -1L) }
                )
                2 -> StatisticViewTab(
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    isLoading = uiState.isLoading
                )
            }
        }
    }

    // 장부 전환 BottomSheet
    if (showLedgerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLedgerSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "장부 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider()

                if (ledgers.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    LazyColumn {
                        items(ledgers) { item ->
                            ListItem(
                                headlineContent = { Text(item.ledgerName) },
                                supportingContent = {
                                    Text(
                                        if (item.isOwner) "내 장부"
                                        else "공유됨 (${if (item.permission == "edit") "편집" else "조회"})"
                                    )
                                },
                                trailingContent = {
                                    if (item.ledgerId == activeLedgerId) {
                                        Icon(Icons.Filled.Check, contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                colors = if (item.ledgerId == activeLedgerId)
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                else ListItemDefaults.colors(),
                                modifier = Modifier.clickable {
                                    viewModel.switchLedger(item.ledgerId)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showLedgerSheet = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(totalIncome: Long, totalExpense: Long) {
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("수입", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "+${fmt.format(totalIncome)}원",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF44336)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("지출", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "-${fmt.format(totalExpense)}원",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2196F3)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("잔액", style = MaterialTheme.typography.labelMedium)
                val balance = totalIncome - totalExpense
                Text(
                    text = "${if (balance >= 0) "+" else ""}${fmt.format(balance)}원",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (balance >= 0) Color(0xFFF44336) else Color(0xFF2196F3)
                )
            }
        }
    }
}
