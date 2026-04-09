package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.BarChart
import com.yoshi0311.sharedledger.ui.components.PieChart
import com.yoshi0311.sharedledger.ui.components.TransactionItem
import com.yoshi0311.sharedledger.ui.components.parseHexColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** 카테고리별 통계 데이터 */
data class CategoryStat(
    val name: String,
    val amount: Long,
    val percentage: Float,
    val color: Color,
    val excluded: Boolean = false
)

private data class RawCategoryStat(val name: String, val amount: Long, val color: Color)

private fun computeRawStats(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    type: String
): List<RawCategoryStat> {
    val categoryMap = categories.associateBy { it.id }
    val filtered = transactions.filter { it.type == type }
    if (filtered.isEmpty()) return emptyList()

    return filtered
        .groupBy { it.categoryId }
        .map { (categoryId, txs) ->
            val amount = txs.sumOf { it.amount }
            val category = categoryId?.let { categoryMap[it] }
            RawCategoryStat(
                name = category?.name ?: "구분 없음",
                amount = amount,
                color = category?.let { parseHexColor(it.color) } ?: Color(0xFF9E9E9E)
            )
        }
        .sortedByDescending { it.amount }
}

private fun buildDisplayStats(
    rawStats: List<RawCategoryStat>,
    excludedNames: Set<String>
): List<CategoryStat> {
    val activeTotal = rawStats.filter { it.name !in excludedNames }.sumOf { it.amount }
    return rawStats.map { raw ->
        val isExcluded = raw.name in excludedNames
        CategoryStat(
            name = raw.name,
            amount = raw.amount,
            percentage = if (isExcluded || activeTotal == 0L) 0f
                         else raw.amount.toFloat() / activeTotal.toFloat() * 100f,
            color = raw.color,
            excluded = isExcluded
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatisticViewTab(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    totalIncome: Long,
    totalExpense: Long,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var selectedType by remember { mutableStateOf("expense") }
    var excludedNames by remember { mutableStateOf(emptySet<String>()) }
    var selectedStat by remember { mutableStateOf<CategoryStat?>(null) }

    val rawStats = remember(transactions, categories, selectedType) {
        computeRawStats(transactions, categories, selectedType)
    }
    val displayStats = remember(rawStats, excludedNames) {
        buildDisplayStats(rawStats, excludedNames)
    }
    val activeStats = remember(displayStats) { displayStats.filter { !it.excluded } }

    val currentTotal = if (selectedType == "expense") totalExpense else totalIncome
    val typeLabel = if (selectedType == "expense") "지출" else "수입"
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 수입/지출 전환 버튼
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            listOf("expense" to "지출", "income" to "수입").forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selectedType == value,
                    onClick = {
                        selectedType = value
                        excludedNames = emptySet()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    label = { Text(label) }
                )
            }
        }

        // 합계 금액
        Text(
            text = "${fmt.format(currentTotal)}원",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "이달의 총 $typeLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (displayStats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이 달의 $typeLabel 내역이 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 도넛 차트 (활성 항목만 표시)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activeStats.isEmpty()) {
                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "표시할\n항목 없음",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    PieChart(
                        stats = activeStats,
                        centerLabel = typeLabel,
                        modifier = Modifier.size(180.dp)
                    )
                }
            }

            // 카테고리별 바 차트
            Text(
                text = "카테고리별 내역",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            BarChart(
                stats = displayStats,
                modifier = Modifier.padding(horizontal = 16.dp),
                onToggle = { name ->
                    excludedNames = if (name in excludedNames) {
                        excludedNames - name
                    } else {
                        excludedNames + name
                    }
                },
                onItemClick = { stat -> selectedStat = stat }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // 카테고리 상세 내역 모달
    if (selectedStat != null) {
        val stat = selectedStat!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val categoryMap = remember(categories) { categories.associateBy { it.id } }
        val dateFormat = remember { SimpleDateFormat("MM월 dd일 (EEE)", Locale.KOREA) }

        val categoryTxs = remember(stat.name, transactions, categories, selectedType) {
            transactions
                .filter { tx ->
                    val catName = categories.find { it.id == tx.categoryId }?.name ?: "구분 없음"
                    tx.type == selectedType && catName == stat.name
                }
                .sortedWith(
                    compareByDescending<TransactionEntity> { it.date }.thenBy { it.time }
                )
        }
        val grouped = remember(categoryTxs) {
            categoryTxs.groupBy { dateFormat.format(it.date) }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedStat = null },
            sheetState = sheetState
        ) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(stat.color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stat.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${fmt.format(stat.amount)}원",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selectedType == "income") Color(0xFFF44336) else Color(0xFF2196F3)
                )
            }
            HorizontalDivider()

            if (categoryTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "내역이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    grouped.forEach { (dateStr, txList) ->
                        stickyHeader(key = "detail_header_$dateStr") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(txList, key = { it.id }) { tx ->
                            TransactionItem(
                                transaction = tx,
                                category = categoryMap[tx.categoryId],
                                onClick = {}
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
