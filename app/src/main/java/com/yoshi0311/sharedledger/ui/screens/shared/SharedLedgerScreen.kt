package com.yoshi0311.sharedledger.ui.screens.shared

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yoshi0311.sharedledger.network.api.SharedUserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLedgerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SharedLedgerViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val searchError  by viewModel.searchError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var searchEmail     by rememberSaveable { mutableStateOf("") }
    var invitePermission by rememberSaveable { mutableStateOf("view") }
    var showInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is SharedActionState.Success -> {
                snackbarHostState.showSnackbar("완료")
                viewModel.resetActionState()
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
                title = { Text("공유 관리") },
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
            // ── 초대 섹션 ────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text("사용자 초대", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchEmail,
                        onValueChange = { searchEmail = it; viewModel.clearSearchResult() },
                        label = { Text("이메일") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { if (searchEmail.isNotBlank()) viewModel.searchUser(searchEmail) }
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "검색")
                    }
                }

                // 검색 결과
                if (searchError != null) {
                    Text(searchError!!, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
                searchResult?.let { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(user.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text(user.email, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { showInviteDialog = true }) {
                                Text("초대")
                            }
                        }
                    }
                }
            }

            // ── 공유 사용자 목록 ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("공유 중인 사용자", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.sharedUsers.isEmpty()) {
                item {
                    Text("공유된 사용자가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.sharedUsers) { user ->
                    SharedUserItem(
                        user = user,
                        onPermissionChange = { newPerm ->
                            viewModel.updatePermission(user.sharedLedgerId, newPerm)
                        },
                        onRevoke = { viewModel.revokeAccess(user.sharedLedgerId) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // 초대 권한 선택 다이얼로그
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("권한 설정") },
            text = {
                Column {
                    Text("${searchResult?.name}(${searchResult?.email})에게 어떤 권한을 부여할까요?")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("view" to "조회", "edit" to "편집").forEach { (value, label) ->
                            if (invitePermission == value) {
                                Button(onClick = { invitePermission = value }) { Text(label) }
                            } else {
                                OutlinedButton(onClick = { invitePermission = value }) { Text(label) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    searchResult?.let { viewModel.invite(it.email, invitePermission) }
                    showInviteDialog = false
                }) { Text("초대") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("취소") }
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

            // 권한 뱃지 (클릭 시 변경 메뉴)
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
