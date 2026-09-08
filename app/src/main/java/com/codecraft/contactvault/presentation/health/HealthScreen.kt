package com.codecraft.contactvault.presentation.health

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codecraft.contactvault.domain.model.ContactHealthReport
import com.codecraft.contactvault.presentation.contacts.ContactItemRow
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    onReviewDuplicatesClick: () -> Unit,
    onContactClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.analyzeHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    HealthScreenContent(
        uiState = uiState,
        onSelectCategory = { category -> viewModel.selectCategory(category) },
        onReviewDuplicatesClick = onReviewDuplicatesClick,
        onContactClick = onContactClick,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreenContent(
    uiState: HealthUiState,
    onSelectCategory: (HealthFilterCategory?) -> Unit,
    onReviewDuplicatesClick: () -> Unit,
    onContactClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val report = uiState.report

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Contact Health Analyzer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedCategory != null) {
                            onSelectCategory(null)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && report.totalContacts == 0) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.selectedCategory != null) {
                // Category Filtered List
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = when (uiState.selectedCategory) {
                            HealthFilterCategory.NO_PHONE -> "Contacts Without Phone Numbers (${report.noPhoneCount})"
                            HealthFilterCategory.NO_EMAIL -> "Contacts Without Email Addresses (${report.noEmailCount})"
                            HealthFilterCategory.INCOMPLETE_NAME -> "Incomplete Names (${report.incompleteNameCount})"
                            else -> "Filtered Issues"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    if (uiState.filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No contacts in this issue category.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(items = uiState.filteredContacts, key = { it.id }) { summary ->
                                ContactItemRow(
                                    summary = summary,
                                    onContactClick = { onContactClick(summary.id) },
                                    onFavoriteToggle = { }
                                )
                            }
                        }
                    }
                }
            } else {
                // Main Health Overview
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Health Score Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = "${report.healthScore}% Healthy",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${report.totalContacts} Total Contacts Analyzed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Quality Metrics & Issue Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Duplicates Card
                    item {
                        HealthMetricRowCard(
                            icon = Icons.Default.FindInPage,
                            title = "Possible Duplicates",
                            count = report.possibleDuplicateCount,
                            actionLabel = "Review Duplicates",
                            onClick = onReviewDuplicatesClick
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // No Phone
                    item {
                        HealthMetricRowCard(
                            icon = Icons.Default.Phone,
                            title = "No Phone Number",
                            count = report.noPhoneCount,
                            actionLabel = "Inspect List",
                            onClick = { onSelectCategory(HealthFilterCategory.NO_PHONE) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // No Email
                    item {
                        HealthMetricRowCard(
                            icon = Icons.Default.Email,
                            title = "No Email Address",
                            count = report.noEmailCount,
                            actionLabel = "Inspect List",
                            onClick = { onSelectCategory(HealthFilterCategory.NO_EMAIL) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Incomplete Name
                    item {
                        HealthMetricRowCard(
                            icon = Icons.Default.Person,
                            title = "Incomplete Names",
                            count = report.incompleteNameCount,
                            actionLabel = "Inspect List",
                            onClick = { onSelectCategory(HealthFilterCategory.INCOMPLETE_NAME) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Button(
                            onClick = onReviewDuplicatesClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan & Merge Duplicates")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthMetricRowCard(
    icon: ImageVector,
    title: String,
    count: Int,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = if (count > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HealthScreenOverviewPreview() {
    ContactVaultTheme {
        HealthScreenContent(
            uiState = HealthUiState(
                isLoading = false,
                report = ContactHealthReport(
                    totalContacts = 142,
                    healthScore = 88,
                    noPhoneCount = 4,
                    noEmailCount = 12,
                    incompleteNameCount = 2,
                    possibleDuplicateCount = 3
                )
            ),
            onSelectCategory = {},
            onReviewDuplicatesClick = {},
            onContactClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HealthMetricRowCardPreview() {
    ContactVaultTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            HealthMetricRowCard(
                icon = Icons.Default.Phone,
                title = "No Phone Number",
                count = 4,
                actionLabel = "Inspect List",
                onClick = {}
            )
        }
    }
}
