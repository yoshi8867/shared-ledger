package com.yoshi0311.sharedledger.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.yoshi0311.sharedledger.ui.screens.home.CategoryStat

/**
 * 도넛(링) 형태의 파이 차트.
 * stats 각 항목의 percentage 합이 100이 되어야 올바르게 렌더링된다.
 */
@Composable
fun PieChart(
    stats: List<CategoryStat>,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.26f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val gapDegrees = if (stats.size > 1) 1.5f else 0f

            var startAngle = -90f
            stats.forEach { stat ->
                val totalSweep = stat.percentage / 100f * 360f
                val sweepAngle = totalSweep - gapDegrees
                if (sweepAngle > 0f) {
                    drawArc(
                        color = stat.color,
                        startAngle = startAngle + gapDegrees / 2f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }
                startAngle += totalSweep
            }
        }

        // 중앙 레이블
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
