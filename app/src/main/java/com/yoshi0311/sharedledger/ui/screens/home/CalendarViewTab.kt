package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.CalendarComposable
import com.yoshi0311.sharedledger.ui.components.TransactionItem
import com.yoshi0311.sharedledger.util.AppYearMonth
import java.util.Calendar

@Composable
fun CalendarViewTab(
    yearMonth: AppYearMonth,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    isLoading: Boolean,
    selectedDay: Int?,
    todayDay: Int,
    onDaySelected: (Int) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val selectedDayTx = remember(transactions, selectedDay) {
        if (selectedDay == null) return@remember emptyList()
        val cal = Calendar.getInstance()
        transactions.filter { tx ->
            cal.time = tx.date
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CalendarComposable(
            yearMonth = yearMonth,
            transactions = transactions,
            selectedDay = selectedDay,
            todayDay = todayDay,
            onDaySelected = onDaySelected,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        // 선택된 날짜 헤더
        Text(
            text = if (selectedDay != null) "${yearMonth.month}월 ${selectedDay}일" else "날짜를 선택하세요",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (selectedDay == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "날짜를 선택하면 거래 목록이 표시됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else if (selectedDayTx.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "이 날의 거래가 없습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(selectedDayTx, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        category = categoryMap[tx.categoryId],
                        onClick = { onTransactionClick(tx) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
