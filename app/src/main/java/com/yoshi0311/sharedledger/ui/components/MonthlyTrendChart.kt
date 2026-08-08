package com.yoshi0311.sharedledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 한 달치 막대 데이터 — segments는 아래에서 위로 쌓이는 순서 */
data class MonthBarData(
    val label: String,                       // "3월"
    val segments: List<Pair<Color, Long>>,   // (색상, 금액)
    val total: Long
)

private fun formatShortAmount(amount: Long): String = when {
    amount >= 100_000_000 -> "${amount / 100_000_000}억"
    amount >= 10_000 -> "${amount / 10_000}만"
    else -> "$amount"
}

/**
 * 월별 스택 막대그래프. 체크된 카테고리들의 합산을 색상별로 쌓아 표시한다.
 */
@Composable
fun MonthlyTrendChart(
    bars: List<MonthBarData>,
    rangeLabel: String,
    canGoNewer: Boolean,
    onShiftWindow: (Int) -> Unit,   // -1 = 최신 쪽, +1 = 과거 쪽
    onBarClick: ((Int) -> Unit)? = null,   // 막대 인덱스 (bars 순서)
    modifier: Modifier = Modifier
) {
    val chartHeight = 140.dp
    val maxTotal = bars.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier) {
        // 기간 이동 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onShiftWindow(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 6개월",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onShiftWindow(-1) }, enabled = canGoNewer) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 6개월",
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (canGoNewer) 0.6f else 0.2f
                    )
                )
            }
        }

        // 막대 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, bar ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onBarClick != null) Modifier.clickable { onBarClick(index) }
                            else Modifier
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 합산 금액 (간략 표기)
                    Text(
                        text = if (bar.total > 0) formatShortAmount(bar.total) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    // 스택 막대 (아래→위로 쌓기: Column은 위→아래로 그리므로 역순 배치)
                    Column(
                        modifier = Modifier
                            .height(chartHeight)
                            .fillMaxWidth(0.55f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        bar.segments.asReversed().forEach { (color, amount) ->
                            val fraction = amount.toFloat() / maxTotal.toFloat()
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(chartHeight * fraction)
                                        .background(color.copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // 월 라벨
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
