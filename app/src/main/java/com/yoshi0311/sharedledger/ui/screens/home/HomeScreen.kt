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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.MonthSelectorBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactionEdit: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("목록", "캘린더", "통계")
    val tabIcons = listOf(
        Icons.Filled.List,
        Icons.Filled.CalendarMonth,
        Icons.Filled.PieChart
    )

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
                onClick = { onNavigateToTransactionEdit(-1L) },
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
            // 월 선택 바
            MonthSelectorBar(
                currentMonth = selectedMonth,
                onPrev = { viewModel.prevMonth() },
                onNext = { viewModel.nextMonth() }
            )

            // 수입/지출 요약 카드
            SummaryCard(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense
            )

            // 탭 바
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        icon = { Icon(imageVector = tabIcons[index], contentDescription = title) }
                    )
                }
            }

            // 탭 콘텐츠
            when (selectedTab) {
                0 -> TransactionListTab(
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    isLoading = uiState.isLoading,
                    onTransactionClick = { tx -> onNavigateToTransactionEdit(tx.id) }
                )
                1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("캘린더 (Phase 4 구현 예정)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("통계 (Phase 4 구현 예정)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
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

@Composable
private fun TransactionListTab(
    transactions: List<TransactionEntity>,
    categories: List<com.yoshi0311.sharedledger.data.db.entity.CategoryEntity>,
    isLoading: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("불러오는 중...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        }
        return
    }

    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("이 달의 거래가 없습니다", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                Text("+ 버튼으로 추가해보세요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            }
        }
        return
    }

    val categoryMap = categories.associateBy { it.id }
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    // 날짜별 그룹핑
    val grouped = transactions.groupBy { dateFormat.format(it.date) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (dateStr, txList) ->
            item {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            items(txList) { tx ->
                val cat = categoryMap[tx.categoryId]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTransactionClick(tx) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.description.ifBlank { cat?.name ?: "내역 없음" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (cat != null) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Text(
                        text = "${if (tx.type == "income") "+" else "-"}${fmt.format(tx.amount)}원",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (tx.type == "income") Color(0xFFF44336) else Color(0xFF2196F3)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) } // FAB 공간
    }
}
