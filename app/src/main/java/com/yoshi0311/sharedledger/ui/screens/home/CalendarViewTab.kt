package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onTransactionClick: (TransactionEntity) -> Unit,
    onAddTransaction: (dateMillis: Long) -> Unit
) {
    val today = remember { Calendar.getInstance() }
    val isCurrentMonth = yearMonth.year == today.get(Calendar.YEAR) &&
            yearMonth.month == today.get(Calendar.MONTH) + 1
    val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    // selectedDay resets whenever the displayed month changes
    var selectedDay by remember(yearMonth) {
        mutableIntStateOf(if (isCurrentMonth) todayDay else 1)
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val selectedDayTx = remember(transactions, selectedDay) {
        val cal = Calendar.getInstance()
        transactions.filter { tx ->
            cal.time = tx.date
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    val selectedDateMillis = remember(yearMonth, selectedDay) {
        Calendar.getInstance().apply {
            set(yearMonth.year, yearMonth.month - 1, selectedDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
            onDaySelected = { selectedDay = it },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        // Selected day header with "+ 추가" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${yearMonth.month}월 ${selectedDay}일",
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(
                onClick = { onAddTransaction(selectedDateMillis) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "추가",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("추가", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (selectedDayTx.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
