package com.codecraft.contactvault.presentation.groups

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codecraft.contactvault.domain.model.ContactGroup
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.presentation.contacts.ContactItemRow
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onContactClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    GroupsScreenContent(
        uiState = uiState,
        onSelectGroup = { group -> viewModel.selectGroup(group) },
        onClearSelectedGroup = { viewModel.clearSelectedGroup() },
        onContactClick = onContactClick,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreenContent(
    uiState: GroupsUiState,
    onSelectGroup: (ContactGroup) -> Unit,
    onClearSelectedGroup: () -> Unit,
    onContactClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.selectedGroup?.title ?: "Contact Groups",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedGroup != null) {
                            onClearSelectedGroup()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.groups.isEmpty() && uiState.groupContacts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.selectedGroup != null) {
                // Group Contacts Detail View
                if (uiState.groupContacts.isEmpty() && !uiState.isLoading) {
                    Text(
                        text = "No contacts in this group.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items = uiState.groupContacts, key = { it.id }) { summary ->
                            ContactItemRow(
                                summary = summary,
                                onContactClick = { onContactClick(summary.id) },
                                onFavoriteToggle = { }
                            )
                        }
                    }
                }
            } else {
                // Groups List
                if (uiState.groups.isEmpty()) {
                    Text(
                        text = "No contact groups found on device.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(items = uiState.groups, key = { it.id }) { group ->
                            GroupCardRow(
                                group = group,
                                onClick = { onSelectGroup(group) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCardRow(
    group: ContactGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!group.accountName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = group.accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (group.memberCount > 0) {
                AssistChip(
                    onClick = onClick,
                    label = { Text("${group.memberCount} members") }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupsScreenListPreview() {
    ContactVaultTheme {
        GroupsScreenContent(
            uiState = GroupsUiState(
                isLoading = false,
                groups = listOf(
                    ContactGroup(id = 1, title = "Family", accountName = "Google Account", memberCount = 12),
                    ContactGroup(id = 2, title = "Co-workers", accountName = "Work Exchange", memberCount = 28),
                    ContactGroup(id = 3, title = "College Friends", accountName = "Google Account", memberCount = 5)
                )
            ),
            onSelectGroup = {},
            onClearSelectedGroup = {},
            onContactClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupCardRowPreview() {
    ContactVaultTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GroupCardRow(
                group = ContactGroup(id = 1, title = "Close Family", accountName = "Personal Account", memberCount = 8),
                onClick = {}
            )
        }
    }
}
