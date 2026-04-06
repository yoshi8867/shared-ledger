package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.BarChart
import com.yoshi0311.sharedledger.ui.components.PieChart
import com.yoshi0311.sharedledger.ui.components.parseHexColor
import java.text.NumberFormat
import java.util.Locale

/** 카테고리별 통계 데이터 */
data class CategoryStat(
    val name: String,
    val amount: Long,
    val percentage: Float,
    val color: Color
)

private fun computeCategoryStats(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    type: String
): List<CategoryStat> {
    val categoryMap = categories.associateBy { it.id }
    val filtered = transactions.filter { it.type == type }
    val total = filtered.sumOf { it.amount }
    if (total == 0L) return emptyList()

    return filtered
        .groupBy { it.categoryId }
        .map { (categoryId, txs) ->
            val amount = txs.sumOf { it.amount }
            val category = categoryId?.let { categoryMap[it] }
            CategoryStat(
                name = category?.name ?: "구분 없음",
                amount = amount,
                percentage = amount.toFloat() / total.toFloat() * 100f,
                color = category?.let { parseHexColor(it.color) } ?: Color(0xFF9E9E9E)
            )
        }
        .sortedByDescending { it.amount }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val stats = remember(transactions, categories, selectedType) {
        computeCategoryStats(transactions, categories, selectedType)
    }
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
                    onClick = { selectedType = value },
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

        if (stats.isEmpty()) {
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
            // 도넛 차트
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    stats = stats,
                    centerLabel = typeLabel,
                    modifier = Modifier.size(180.dp)
                )
            }

            // 카테고리별 바 차트
            Text(
                text = "카테고리별 내역",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            BarChart(
                stats = stats,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
