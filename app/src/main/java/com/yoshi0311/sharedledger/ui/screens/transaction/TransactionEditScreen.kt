package com.yoshi0311.sharedledger.ui.screens.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val isEditMode = viewModel.transactionId != -1L
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    // 현재 시각으로 초기화
    val nowCal = remember { Calendar.getInstance() }

    var type by remember { mutableStateOf("expense") }
    var amountRaw by remember { mutableStateOf("") }          // 실제 숫자 문자열
    var selectedDate by remember {
        mutableStateOf(
            if (viewModel.initialDateMillis > 0) Date(viewModel.initialDateMillis) else Date()
        )
    }
    var selectedHour by remember { mutableStateOf(nowCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(nowCal.get(Calendar.MINUTE)) }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var formInitialized by remember { mutableStateOf(false) }

    // 수정 모드: 기존 데이터 채우기
    LaunchedEffect(uiState.existing) {
        val existing = uiState.existing ?: return@LaunchedEffect
        if (!formInitialized) {
            type = existing.type
            amountRaw = existing.amount.toString()
            selectedDate = existing.date
            existing.time.split(":").let { parts ->
                selectedHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                selectedMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            }
            description = existing.description
            selectedCategoryId = existing.categoryId
            formInitialized = true
        }
    }

    // 저장 완료 시 복귀
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val timeText = "%02d:%02d".format(selectedHour, selectedMinute)
    val selectedCategory = categories.find { it.id == selectedCategoryId }
    val amountDisplay = amountRaw.toLongOrNull()?.let { numberFormat.format(it) } ?: amountRaw

    if (showCategoryDialog) {
        CategoryDialog(
            categories = categories,
            selectedCategoryId = selectedCategoryId ?: -1L,
            onSelectCategory = { cat ->
                selectedCategoryId = cat.id
                showCategoryDialog = false
            },
            onAddCategory = { name, color -> viewModel.addCategory(name, color) },
            onDismiss = { showCategoryDialog = false }
        )
    }

    // DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = Date(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TimePickerDialog (Material3 TimePicker를 AlertDialog로 감싸기)
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "시각 선택",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("취소") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            showTimePicker = false
                        }) { Text("확인") }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "거래 수정" else "거래 추가") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 수입/지출 토글
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("expense" to "지출", "income" to "수입").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = type == value,
                        onClick = { type = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 금액 (천단위 쉼표 표시)
            OutlinedTextField(
                value = amountDisplay,
                onValueChange = { input ->
                    // 숫자만 추출하여 저장
                    amountRaw = input.filter { it.isDigit() }
                },
                label = { Text("금액") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("원") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 날짜 + 시각
            Row(modifier = Modifier.fillMaxWidth()) {
                // 날짜 — 투명 Box로 전체 영역 클릭 처리
                Box(modifier = Modifier.weight(2f)) {
                    OutlinedTextField(
                        value = dateFormat.format(selectedDate),
                        onValueChange = {},
                        label = { Text("날짜") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 시각 — 투명 Box로 전체 영역 클릭 처리
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = {},
                        label = { Text("시각") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Filled.AccessTime, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 내용
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("내용") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 구분(카테고리) 선택
            OutlinedButton(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedCategory != null) "구분: ${selectedCategory.name}"
                    else "구분 선택 (선택사항)"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 저장 버튼
            val amount = amountRaw.toLongOrNull() ?: 0L
            Button(
                onClick = {
                    viewModel.save(
                        type = type,
                        amount = amount,
                        date = selectedDate,
                        time = timeText,
                        categoryId = selectedCategoryId,
                        description = description
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = amount > 0
            ) {
                Text(
                    text = if (isEditMode) "수정 완료" else "저장",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
