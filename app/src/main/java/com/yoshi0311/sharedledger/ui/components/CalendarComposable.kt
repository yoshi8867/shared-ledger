package com.yoshi0311.sharedledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.util.AppYearMonth
import java.util.Calendar

private const val CELL_HEIGHT_DP = 58

@Composable
fun CalendarComposable(
    yearMonth: AppYearMonth,
    transactions: List<TransactionEntity>,
    selectedDay: Int?,
    todayDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val incomeByDay = remember(transactions) {
        mutableMapOf<Int, Long>().also { map ->
            transactions.filter { it.type == "income" }.forEach { tx ->
                val day = Calendar.getInstance().apply { time = tx.date }.get(Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: 0L) + tx.amount
            }
        }
    }
    val expenseByDay = remember(transactions) {
        mutableMapOf<Int, Long>().also { map ->
            transactions.filter { it.type == "expense" }.forEach { tx ->
                val day = Calendar.getInstance().apply { time = tx.date }.get(Calendar.DAY_OF_MONTH)
                map[day] = (map[day] ?: 0L) + tx.amount
            }
        }
    }

    val cal = Calendar.getInstance().apply { set(yearMonth.year, yearMonth.month - 1, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun … 7=Sat
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val offset = firstDayOfWeek - 1

    val dayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEachIndexed { index, header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == 0 || index == 6)
                        Color(0xFFE57373)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val rows = (offset + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - offset + 1
                    if (day < 1 || day > daysInMonth) {
                        // 빈 셀 — 테두리 없음
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(CELL_HEIGHT_DP.dp)
                        )
                    } else {
                        DayCell(
                            day = day,
                            income = incomeByDay[day],
                            expense = expenseByDay[day],
                            isSelected = day == selectedDay,
                            isToday = day == todayDay,
                            isSunday = col == 0,
                            isSaturday = col == 6,
                            onClick = { onDaySelected(day) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    income: Long?,
    expense: Long?,
    isSelected: Boolean,
    isToday: Boolean,
    isSunday: Boolean,
    isSaturday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayTextColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isSunday || isSaturday -> Color(0xFFE57373)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val cellBorderColor = Color(0xFFE0E0E0)
    val selectedBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .height(CELL_HEIGHT_DP.dp)
            .drawBehind {
                val stroke = 0.5.dp.toPx()
                drawLine(cellBorderColor, Offset(0f, 0f), Offset(size.width, 0f), stroke)
                drawLine(cellBorderColor, Offset(0f, 0f), Offset(0f, size.height), stroke)
                drawLine(cellBorderColor, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
                drawLine(cellBorderColor, Offset(0f, size.height), Offset(size.width, size.height), stroke)
            }
            .then(if (isSelected) Modifier.background(selectedBg) else Modifier)
            .clickable(onClick = onClick)
    ) {
        // 날짜 숫자 — 좌상단
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 4.dp)
                .size(20.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else if (isToday) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
            }
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = dayTextColor,
                textAlign = TextAlign.Center
            )
        }

        // 금액 — 우하단 (수입 위, 지출 아래)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 3.dp, bottom = 3.dp),
            horizontalAlignment = Alignment.End
        ) {
            if ((income ?: 0L) > 0) {
                Text(
                    text = formatShortAmount(income!!),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color(0xFFF44336),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            if ((expense ?: 0L) > 0) {
                Text(
                    text = formatShortAmount(expense!!),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color(0xFF2196F3),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

private fun formatShortAmount(amount: Long): String = when {
    amount >= 100_000_000 -> "${amount / 100_000_000}억"
    amount >= 10_000 -> "${amount / 10_000}만"
    amount >= 1_000 -> "${amount / 1_000}천"
    else -> "$amount"
}
