package com.yoshi0311.sharedledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    modifier: Modifier = Modifier,
    onToggle: ((String) -> Unit)? = null,
    onItemClick: ((CategoryStat) -> Unit)? = null
) {
    if (stats.isEmpty()) return

    val activeStats = stats.filter { !it.excluded }
    val maxAmount = activeStats.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    val fmt = NumberFormat.getNumberInstance(Locale.KOREA)

    Column(modifier = modifier) {
        stats.forEachIndexed { index, stat ->
            val contentAlpha = if (stat.excluded) 0.35f else 1f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 클릭 가능한 콘텐츠 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onItemClick != null) Modifier.clickable { onItemClick(stat) }
                            else Modifier
                        )
                        .padding(vertical = 6.dp)
                ) {
                    // 윗줄: [● 이름] — 우측 퍼센트
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stat.color.copy(alpha = contentAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stat.name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                        )
                        Text(
                            text = if (stat.excluded) "--" else "%.1f%%".format(stat.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (stat.excluded) 0.25f else 0.6f
                            ),
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
                        Spacer(modifier = Modifier.width(14.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = if (stat.excluded) 0.3f else 1f
                                    )
                                )
                        ) {
                            if (!stat.excluded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(
                                            fraction = stat.amount.toFloat() / maxAmount.toFloat()
                                        )
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(stat.color.copy(alpha = 0.85f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (stat.excluded) "" else "${fmt.format(stat.amount)}원",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }

                // 토글 버튼 (눈 아이콘)
                if (onToggle != null) {
                    IconButton(onClick = { onToggle(stat.name) }) {
                        Icon(
                            imageVector = if (stat.excluded) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = if (stat.excluded) "그래프에 포함" else "그래프에서 제외",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (stat.excluded) 0.3f else 0.55f
                            )
                        )
                    }
                }
            }

            if (index < stats.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
