package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListViewTab(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
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
                Text(
                    "이 달의 거래가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Text(
                    "+ 버튼으로 추가해보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }
        return
    }

    val categoryMap = categories.associateBy { it.id }
    val dateFormat = SimpleDateFormat("MM월 dd일 (EEE)", Locale.KOREA)
    val grouped = transactions.groupBy { dateFormat.format(it.date) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (dateStr, txList) ->
            stickyHeader(key = "header_$dateStr") {
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
                    onClick = { onTransactionClick(tx) }
                )
            }
            item(key = "spacer_$dateStr") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        item(key = "fab_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
