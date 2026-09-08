package com.codecraft.contactvault.presentation.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.MergePreview
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun DuplicateComparisonScreen(
    contactIdA: Long,
    contactIdB: Long,
    viewModel: DuplicateComparisonViewModel,
    onMergeSuccess: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(contactIdA, contactIdB) {
        viewModel.loadComparisonPair(contactIdA, contactIdB)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    DuplicateComparisonScreenContent(
        uiState = uiState,
        onConfirmMerge = { viewModel.confirmMerge(onSuccess = onMergeSuccess) },
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateComparisonScreenContent(
    uiState: DuplicateComparisonUiState,
    onConfirmMerge: () -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showConfirmMergeDialog by remember { mutableStateOf(false) }

    val contactA = uiState.contactA
    val contactB = uiState.contactB
    val preview = uiState.preview

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Compare & Merge", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading && (contactA == null || contactB == null)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (contactA == null || contactB == null || preview == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Unable to load comparison contacts.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Safety Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Data Safety Active: A local recovery snapshot of Secondary Contact will be saved before merging.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Side-by-Side Comparison Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact A (Primary / Target)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PRIMARY RECORD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ContactAvatar(displayName = contactA.displayName, photoUri = contactA.photoUri, size = 48.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = contactA.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Phones: ${contactA.phoneNumbers.size}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Emails: ${contactA.emailAddresses.size}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Contact B (Secondary)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SECONDARY RECORD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ContactAvatar(displayName = contactB.displayName, photoUri = contactB.photoUri, size = 48.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = contactB.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Phones: ${contactB.phoneNumbers.size}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Emails: ${contactB.emailAddresses.size}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Merge Result Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Merge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Combined Outcome Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Name: ${preview.combinedDisplayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (preview.combinedPhones.isNotEmpty()) {
                            Text(
                                text = "Preserved Phone Numbers (${preview.combinedPhones.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            preview.combinedPhones.forEach { p ->
                                Text(
                                    text = "  • ${p.number}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (preview.combinedEmails.isNotEmpty()) {
                            Text(
                                text = "Preserved Emails (${preview.combinedEmails.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            preview.combinedEmails.forEach { e ->
                                Text(
                                    text = "  • ${e.address}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { showConfirmMergeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Safely Merge Records")
                }
            }
        }
    }

    if (showConfirmMergeDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmMergeDialog = false },
            title = { Text("Confirm Contact Merge") },
            text = {
                Text("Are you sure you want to merge these contacts? All unique phone numbers and email addresses will be preserved in ${contactA?.displayName ?: "the primary record"}. Contact B will be safely snapshot and removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmMergeDialog = false
                        onConfirmMerge()
                    }
                ) {
                    Text("Merge Contacts", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DuplicateComparisonScreenPreview() {
    ContactVaultTheme {
        DuplicateComparisonScreenContent(
            uiState = DuplicateComparisonUiState(
                isLoading = false,
                contactA = Contact(
                    id = 1,
                    lookupKey = "k1",
                    displayName = "John Doe",
                    phoneNumbers = listOf(PhoneNumber(id = 1, number = "+1 555 123 4567", type = 2))
                ),
                contactB = Contact(
                    id = 2,
                    lookupKey = "k2",
                    displayName = "Jon Doe",
                    phoneNumbers = listOf(PhoneNumber(id = 2, number = "+1 555 123 4567", type = 2)),
                    emailAddresses = listOf(EmailAddress(id = 3, address = "jondoe@example.com", type = 1))
                ),
                preview = MergePreview(
                    primaryContactId = 1,
                    secondaryContactId = 2,
                    combinedDisplayName = "John Doe",
                    combinedPhotoUri = null,
                    combinedPhones = listOf(PhoneNumber(id = 1, number = "+1 555 123 4567", type = 2)),
                    combinedEmails = listOf(EmailAddress(id = 3, address = "jondoe@example.com", type = 1)),
                    combinedOrganization = null,
                    combinedAddresses = emptyList(),
                    combinedWebsites = emptyList(),
                    combinedNotes = emptyList()
                )
            ),
            onConfirmMerge = {},
            onBackClick = {}
        )
    }
}
