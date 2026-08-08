package com.yoshi0311.sharedledger.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yoshi0311.sharedledger.data.db.entity.CategoryEntity
import com.yoshi0311.sharedledger.data.db.entity.TransactionEntity
import com.yoshi0311.sharedledger.ui.components.TransactionItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 전 기간 거래 검색 모달. 조건은 서로 AND —
 * 검색어(내역) · 수입/지출(OR) · 기간 · 금액 · 카테고리(복수 OR).
 * 결과는 모달 안에서 목록으로 표시.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDialog(
    filter: SearchFilter,
    categories: List<CategoryEntity>,
    results: List<TransactionEntity>,
    onChange: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
    onResultClick: (TransactionEntity) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var amountExpanded by remember { mutableStateOf(filter.minAmount != null || filter.maxAmount != null) }
    var categoryExpanded by remember { mutableStateOf(filter.categoryIds.isNotEmpty()) }
    // 모달 열릴 때의 기본 기간(현재 월) — '전체' 해제 시 복원용
    val defaultRange = remember { filter.startDate to filter.endDate }
    val allPeriod = filter.startDate == null && filter.endDate == null
    var minText by remember { mutableStateOf(filter.minAmount?.toString() ?: "") }
    var maxText by remember { mutableStateOf(filter.maxAmount?.toString() ?: "") }
    // null=닫힘, true=시작일 피커, false=종료일 피커
    var pickerFor by remember { mutableStateOf<Boolean?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("검색") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "닫기")
                        }
                    }
                )

                // ── 필터 입력부 ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { onChange(filter.copy(query = it)) },
                        singleLine = true,
                        placeholder = { Text("내역 검색") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckItem("수입", filter.income) { onChange(filter.copy(income = it)) }
                        CheckItem("지출", filter.expense) { onChange(filter.copy(expense = it)) }
                    }

                    // 기간
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("기간", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        CheckItem("전체", allPeriod) { checked ->
                            onChange(
                                if (checked) filter.copy(startDate = null, endDate = null)
                                else filter.copy(startDate = defaultRange.first, endDate = defaultRange.second)
                            )
                        }
                    }
                    if (!allPeriod) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(onClick = { pickerFor = true }, modifier = Modifier.weight(1f)) {
                                Text(filter.startDate?.let { dateFmt.format(it) } ?: "시작일")
                            }
                            Text("~")
                            OutlinedButton(onClick = { pickerFor = false }, modifier = Modifier.weight(1f)) {
                                Text(filter.endDate?.let { dateFmt.format(it) } ?: "종료일")
                            }
                        }
                    }

                    // 금액 (접힘)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { amountExpanded = !amountExpanded }
                            .padding(vertical = 4.dp)
                    ) {
                        Text("금액", style = MaterialTheme.typography.labelLarge)
                        Icon(
                            imageVector = if (amountExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (amountExpanded) "접기" else "펼치기"
                        )
                    }
                    if (amountExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = minText,
                                onValueChange = {
                                    minText = it.filter { c -> c.isDigit() }
                                    onChange(filter.copy(minAmount = minText.toLongOrNull()))
                                },
                                singleLine = true,
                                placeholder = { Text("최소") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Text("~")
                            OutlinedTextField(
                                value = maxText,
                                onValueChange = {
                                    maxText = it.filter { c -> c.isDigit() }
                                    onChange(filter.copy(maxAmount = maxText.toLongOrNull()))
                                },
                                singleLine = true,
                                placeholder = { Text("최대") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 카테고리 (접힘, 복수 선택 = OR)
                    if (categories.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryExpanded = !categoryExpanded }
                                .padding(vertical = 4.dp)
                        ) {
                            Text("카테고리", style = MaterialTheme.typography.labelLarge)
                            Icon(
                                imageVector = if (categoryExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (categoryExpanded) "접기" else "펼치기"
                            )
                        }
                    }
                    if (categories.isNotEmpty() && categoryExpanded) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { c ->
                                val selected = c.id in filter.categoryIds
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val next = if (selected) filter.categoryIds - c.id
                                                   else filter.categoryIds + c.id
                                        onChange(filter.copy(categoryIds = next))
                                    },
                                    label = { Text(c.name) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "결과 ${results.size}건",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "검색 결과가 없습니다",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.id }) { tx ->
                            TransactionItem(
                                transaction = tx,
                                category = categoryMap[tx.categoryId],
                                onClick = { onResultClick(tx) },
                                showDate = true
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    // 날짜 선택기
    pickerFor?.let { isStart ->
        val initial = (if (isStart) filter.startDate else filter.endDate)?.let { localToPickerUtc(it) }
        DatePickerModal(
            initialUtcMillis = initial,
            onDismiss = { pickerFor = null },
            onConfirm = { picked ->
                if (picked != null) {
                    onChange(
                        if (isStart) filter.copy(startDate = utcToLocalDayStart(picked))
                        else filter.copy(endDate = utcToLocalDayEnd(picked))
                    )
                }
                pickerFor = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialUtcMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.selectedDateMillis) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun CheckItem(label: String, checked: Boolean, onCheck: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onCheck(!checked) }
            .padding(end = 12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheck)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// DatePicker는 UTC 자정 기준 millis를 반환/기대 → 로컬 캘린더 일자와 맞춰 변환한다.
private fun utcToLocalDayStart(utc: Long): Long = localDayFromUtc(utc, 0, 0, 0, 0)
private fun utcToLocalDayEnd(utc: Long): Long = localDayFromUtc(utc, 23, 59, 59, 999)

private fun localDayFromUtc(utc: Long, h: Int, m: Int, s: Int, ms: Int): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH), h, m, s)
        set(Calendar.MILLISECOND, ms)
    }.timeInMillis
}

private fun localToPickerUtc(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
