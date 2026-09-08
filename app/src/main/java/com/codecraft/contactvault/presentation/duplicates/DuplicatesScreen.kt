package com.codecraft.contactvault.presentation.duplicates

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.model.DuplicatePair
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    onCompareClick: (contactIdA: Long, contactIdB: Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDuplicates()
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

    DuplicatesScreenContent(
        uiState = uiState,
        onCompareClick = onCompareClick,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreenContent(
    uiState: DuplicatesUiState,
    onCompareClick: (contactIdA: Long, contactIdB: Long) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Detection", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.duplicatePairs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.duplicatePairs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Duplicates Found!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All contacts have distinct phone numbers, emails, and names.",
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
                            text = "${uiState.duplicatePairs.size} Duplicate Candidate Pair(s) Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(items = uiState.duplicatePairs, key = { it.id }) { pair ->
                        DuplicatePairCard(
                            pair = pair,
                            onCompareClick = {
                                onCompareClick(pair.contactA.id, pair.contactB.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicatePairCard(
    pair: DuplicatePair,
    onCompareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(pair.matchReason, style = MaterialTheme.typography.labelSmall) }
                )

                Text(
                    text = if (pair.isConfirmed) "Confirmed (${pair.confidenceScore}%)" else "Possible (${pair.confidenceScore}%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pair.isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contact A
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(displayName = pair.contactA.displayName, photoUri = pair.contactA.photoUri, size = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pair.contactA.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    pair.contactA.primaryPhone?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Contact B
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ContactAvatar(displayName = pair.contactB.displayName, photoUri = pair.contactB.photoUri, size = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pair.contactB.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    pair.contactB.primaryPhone?.let {
                        Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCompareClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compare & Merge")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DuplicatesScreenPreview() {
    ContactVaultTheme {
        DuplicatesScreenContent(
            uiState = DuplicatesUiState(
                isLoading = false,
                duplicatePairs = listOf(
                    DuplicatePair(
                        id = "1_2",
                        contactA = ContactSummary(id = 1, lookupKey = "k1", displayName = "John Doe", primaryPhone = "+1 555 123 4567"),
                        contactB = ContactSummary(id = 2, lookupKey = "k2", displayName = "Jon Doe", primaryPhone = "+1 555 123 4567"),
                        matchReason = "Identical Phone Number",
                        confidenceScore = 95,
                        isConfirmed = true
                    )
                )
            ),
            onCompareClick = { _, _ -> },
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DuplicatePairCardPreview() {
    ContactVaultTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DuplicatePairCard(
                pair = DuplicatePair(
                    id = "1_2",
                    contactA = ContactSummary(id = 1, lookupKey = "k1", displayName = "Alice Smith", primaryPhone = "+1 234 567 8900"),
                    contactB = ContactSummary(id = 2, lookupKey = "k2", displayName = "Alice M. Smith", primaryPhone = "+1 234 567 8900"),
                    matchReason = "Identical Phone Number",
                    confidenceScore = 90,
                    isConfirmed = true
                ),
                onCompareClick = {}
            )
        }
    }
}
