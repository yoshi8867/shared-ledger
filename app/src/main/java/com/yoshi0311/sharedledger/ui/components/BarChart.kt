package com.yoshi0311.sharedledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yoshi0311.sharedledger.ui.screens.home.CategoryStat
import java.text.NumberFormat
import java.util.Locale

/**
 * 카테고리별 수평 막대 차트.
 * stats 목록을 amount 내림차순으로 정렬하여 사용한다고 가정한다.
 */
@Composable
fun BarChart(
    stats: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    val maxAmount = stats.maxOf { it.amount }.coerceAtLeast(1L)
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    Column(modifier = modifier) {
        stats.forEachIndexed { index, stat ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                // 윗줄: [● 이름] — 우측 퍼센트
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stat.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stat.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.1f%%".format(stat.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 막대 + 금액
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 막대 (dot 너비 맞춤 들여쓰기)
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = stat.amount.toFloat() / maxAmount.toFloat())
                                .clip(RoundedCornerShape(3.dp))
                                .background(stat.color.copy(alpha = 0.85f))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${fmt.format(stat.amount)}원",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }

            if (index < stats.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
