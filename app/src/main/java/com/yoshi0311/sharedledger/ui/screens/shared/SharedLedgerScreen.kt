package com.yoshi0311.sharedledger.ui.screens.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.network.api.SharedLedgerDto
import com.yoshi0311.sharedledger.network.api.SharedUserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLedgerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SharedLedgerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val ledgerName       by viewModel.ledgerName.collectAsStateWithLifecycle()
    val isOwner          by viewModel.isOwner.collectAsStateWithLifecycle()
    val uiState          by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState      by viewModel.actionState.collectAsStateWithLifecycle()
    val sharedWithMe     by viewModel.sharedWithMe.collectAsStateWithLifecycle()
    val inviteCode       by viewModel.inviteCode.collectAsStateWithLifecycle()
    val allLedgers       by viewModel.allLedgers.collectAsStateWithLifecycle()
    val activeLedgerId   by viewModel.activeLedgerId.collectAsStateWithLifecycle()

    val activeLedgerName = allLedgers.find { it.ledgerId == activeLedgerId }?.ledgerName ?: "장부 선택"

    val snackbarHostState = remember { SnackbarHostState() }

    var showLedgerMenu by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by rememberSaveable(ledgerName) { mutableStateOf(ledgerName) }
    var joinCode      by rememberSaveable { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is SharedActionState.Success -> {
                snackbarHostState.showSnackbar("완료")
                viewModel.resetActionState()
                isEditingName = false
                joinCode = ""
                showJoinDialog = false
            }
            is SharedActionState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("장부 관리") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── 기록 장부 선택 ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("기록 장부")
                Spacer(Modifier.height(8.dp))

                Box {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        onClick = { showLedgerMenu = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeLedgerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "새 데이터가 이 장부에 기록됩니다",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "장부 선택",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showLedgerMenu,
                        onDismissRequest = { showLedgerMenu = false }
                    ) {
                        if (allLedgers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("장부 없음", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { showLedgerMenu = false }
                            )
                        } else {
                            allLedgers.forEach { ledger ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(ledger.ledgerName)
                                            if (!ledger.isOwner) {
                                                Text(
                                                    "(공유)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (ledger.ledgerId == activeLedgerId) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchActiveLedger(ledger.ledgerId)
                                        showLedgerMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 내 장부 ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("내 장부")
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    if (isEditingName) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = editNameValue,
                                onValueChange = { editNameValue = it },
                                label = { Text("장부 이름") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.renameLedger(editNameValue) },
                                    enabled = editNameValue.isNotBlank() && editNameValue != ledgerName
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                    Text("저장", modifier = Modifier.padding(start = 4.dp))
                                }
                                OutlinedButton(onClick = {
                                    isEditingName = false
                                    editNameValue = ledgerName
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                    Text("취소", modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ledgerName.ifBlank { "내 장부" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "내가 소유한 장부",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            if (isOwner) {
                                IconButton(onClick = {
                                    editNameValue = ledgerName
                                    isEditingName = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "이름 수정")
                                }
                            }
                        }
                    }
                }
            }

            // ── 공유 관리 (소유자만) ──────────────────────────────────────────
            if (isOwner) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("공유 중인 사용자")
                    Spacer(Modifier.height(8.dp))

                    // ── 초대 코드 ────────────────────────────────────────────
                    Text(
                        "초대 코드",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (inviteCode != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = inviteCode!!,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(
                                        6f, androidx.compose.ui.unit.TextUnitType.Sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "24시간 동안 유효",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val clip = ClipData.newPlainText("초대 코드", inviteCode)
                                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                            .setPrimaryClip(clip)
                                    }) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                        Text("복사", modifier = Modifier.padding(start = 4.dp))
                                    }
                                    OutlinedButton(onClick = { viewModel.generateInviteCode() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null)
                                        Text("갱신", modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = { viewModel.generateInviteCode() }) {
                                    Text("초대 코드 생성")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 공유 중인 사용자 목록
                if (uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.sharedUsers.isEmpty()) {
                    item {
                        Text(
                            "공유된 사용자가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(uiState.sharedUsers) { user ->
                        SharedUserItem(
                            user = user,
                            onPermissionChange = { viewModel.updatePermission(user.sharedLedgerId, it) },
                            onRevoke = { viewModel.revokeAccess(user.sharedLedgerId) }
                        )
                    }
                }
            }

            // ── 공유받은 장부 ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader("공유받은 장부")
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("코드로 참가")
                    }
                }
                Spacer(Modifier.height(4.dp))

                if (sharedWithMe.isEmpty()) {
                    Text(
                        "공유받은 장부가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            items(sharedWithMe) { item ->
                SharedWithMeItem(
                    item = item,
                    onLeave = { viewModel.leaveSharedLedger(item.sharedLedgerId) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // 코드로 참가 다이얼로그
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; joinCode = "" },
            title = { Text("초대 코드로 참가") },
            text = {
                Column {
                    Text(
                        "상대방이 공유한 6자리 코드를 입력하세요.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().take(6) },
                        label = { Text("초대 코드") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.joinLedger(joinCode) },
                    enabled = joinCode.length == 6
                ) { Text("참가") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false; joinCode = "" }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SharedWithMeItem(
    item: SharedLedgerDto,
    onLeave: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.ledgerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "소유자: ${item.ownerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.ownerEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Badge(
                containerColor = if (item.permission == "edit")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = if (item.permission == "edit") "편집" else "조회",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "나가기",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("장부 나가기") },
            text = { Text("'${item.ledgerName}' 장부에서 나가시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { onLeave(); showConfirm = false }) { Text("나가기") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SharedUserItem(
    user: SharedUserDto,
    onPermissionChange: (String) -> Unit,
    onRevoke: () -> Unit
) {
    var showPermMenu by remember { mutableStateOf(false) }
    var showConfirm  by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
            }

            Box {
                OutlinedButton(onClick = { showPermMenu = true }) {
                    Text(if (user.permission == "edit") "편집" else "조회")
                }
                DropdownMenu(expanded = showPermMenu, onDismissRequest = { showPermMenu = false }) {
                    DropdownMenuItem(text = { Text("조회") },
                        onClick = { onPermissionChange("view"); showPermMenu = false })
                    DropdownMenuItem(text = { Text("편집") },
                        onClick = { onPermissionChange("edit"); showPermMenu = false })
                }
            }

            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "공유 해제",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("공유 해제") },
            text = { Text("${user.name}의 접근을 해제할까요?") },
            confirmButton = {
                TextButton(onClick = { onRevoke(); showConfirm = false }) { Text("해제") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("취소") }
            }
        )
    }
}
