package com.codecraft.contactvault.presentation.recovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codecraft.contactvault.data.database.entity.DeletedContactSnapshotEntity
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentlyDeletedScreen(
    viewModel: RecoveryViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearMessage()
        }
    }

    RecentlyDeletedScreenContent(
        uiState = uiState,
        onRestoreSnapshot = { snapshot -> viewModel.restoreSnapshot(snapshot) },
        onDeleteSnapshotPermanently = { snapshot -> viewModel.deleteSnapshotPermanently(snapshot) },
        onClearAllSnapshots = { viewModel.clearAllSnapshots() },
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreenContent(
    uiState: RecoveryUiState,
    onRestoreSnapshot: (DeletedContactSnapshotEntity) -> Unit,
    onDeleteSnapshotPermanently: (DeletedContactSnapshotEntity) -> Unit,
    onClearAllSnapshots: () -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var snapshotToDelete by remember { mutableStateOf<DeletedContactSnapshotEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted / Recovery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.snapshots.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Clear All Snapshots",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.snapshots.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.snapshots.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No Recovery Snapshots Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When you delete or merge contacts in ContactVault, safety recovery snapshots are created here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = "${uiState.snapshots.size} Recovery Snapshot(s) Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(items = uiState.snapshots, key = { it.id }) { snapshot ->
                        SnapshotCardRow(
                            snapshot = snapshot,
                            onRestore = { onRestoreSnapshot(snapshot) },
                            onDeletePermanently = { snapshotToDelete = snapshot }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (snapshotToDelete != null) {
        val target = snapshotToDelete!!
        AlertDialog(
            onDismissRequest = { snapshotToDelete = null },
            title = { Text("Delete Permanently?") },
            text = { Text("Are you sure you want to permanently delete the recovery snapshot for \"${target.displayName}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = target
                    snapshotToDelete = null
                    onDeleteSnapshotPermanently(toDelete)
                }) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Snapshots?") },
            text = { Text("This will permanently remove all recovery snapshots from your local storage. Proceed?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllDialog = false
                    onClearAllSnapshots()
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SnapshotCardRow(
    snapshot: DeletedContactSnapshotEntity,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(displayName = snapshot.displayName, photoUri = null, size = 48.dp)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snapshot.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deleted: ${dateFormat.format(Date(snapshot.deletedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Reason: ${snapshot.reason}", style = MaterialTheme.typography.labelSmall) }
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore Contact",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDeletePermanently) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Permanently",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecentlyDeletedScreenPreview() {
    ContactVaultTheme {
        RecentlyDeletedScreenContent(
            uiState = RecoveryUiState(
                isLoading = false,
                snapshots = listOf(
                    DeletedContactSnapshotEntity(
                        id = 1,
                        originalContactId = 101,
                        displayName = "Bob Johnson",
                        contactJson = "{}",
                        deletedAt = System.currentTimeMillis() - 86400000,
                        reason = "Merged into John Doe"
                    ),
                    DeletedContactSnapshotEntity(
                        id = 2,
                        originalContactId = 102,
                        displayName = "Old Contact",
                        contactJson = "{}",
                        deletedAt = System.currentTimeMillis() - 172800000,
                        reason = "User Delete"
                    )
                )
            ),
            onRestoreSnapshot = {},
            onDeleteSnapshotPermanently = {},
            onClearAllSnapshots = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SnapshotCardRowPreview() {
    ContactVaultTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SnapshotCardRow(
                snapshot = DeletedContactSnapshotEntity(
                    id = 1,
                    originalContactId = 101,
                    displayName = "Bob Johnson",
                    contactJson = "{}",
                    deletedAt = System.currentTimeMillis(),
                    reason = "Merged into John Doe"
                ),
                onRestore = {},
                onDeletePermanently = {}
            )
        }
    }
}
