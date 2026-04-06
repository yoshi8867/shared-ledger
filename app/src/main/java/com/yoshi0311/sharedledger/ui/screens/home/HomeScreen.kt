package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.ui.components.MonthSelectorBar
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactionEdit: (id: Long, dateMillis: Long) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCalendarDay by viewModel.selectedCalendarDay.collectAsStateWithLifecycle()

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedMonth.displayLabel(),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: 동기화 (Phase 6) */ }) {
                        Icon(imageVector = Icons.Filled.Sync, contentDescription = "동기화")
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
