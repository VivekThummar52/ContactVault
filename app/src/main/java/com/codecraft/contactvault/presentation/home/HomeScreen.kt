package com.codecraft.contactvault.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codecraft.contactvault.domain.ads.AdPlacement
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.presentation.ads.AdaptiveBannerAd
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

@Composable
fun HomeScreen(
    viewModel: HomeDashboardViewModel,
    onNavigateToContacts: (favoritesOnly: Boolean) -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDashboardData()
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

    HomeScreenContent(
        uiState = uiState,
        onRefresh = { viewModel.loadDashboardData() },
        onNavigateToContacts = onNavigateToContacts,
        onNavigateToGroups = onNavigateToGroups,
        onNavigateToDuplicates = onNavigateToDuplicates,
        onNavigateToHealth = onNavigateToHealth,
        onContactClick = onContactClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeDashboardUiState,
    onRefresh: () -> Unit,
    onNavigateToContacts: (favoritesOnly: Boolean) -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ContactVault", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Offline Contacts Management", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AdaptiveBannerAd(
                placement = AdPlacement.HOME_BANNER,
                modifier = Modifier.fillMaxWidth()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.totalContactsCount == 0) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Search Card Bar
                    OutlinedCard(
                        onClick = { onNavigateToContacts(false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search contacts by name, phone, email...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hero Metric Card: Total Contacts
                    Card(
                        onClick = { onNavigateToContacts(false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${uiState.totalContactsCount} Contacts",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Stored locally on device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Dashboard Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2x2 Metric Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            icon = Icons.Default.Star,
                            label = "Favorites",
                            count = uiState.favoritesCount,
                            onClick = { onNavigateToContacts(true) },
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCard(
                            icon = Icons.Default.Group,
                            label = "Groups",
                            count = uiState.groupsCount,
                            onClick = onNavigateToGroups,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardMetricCard(
                            icon = Icons.Default.FindInPage,
                            label = "Duplicates",
                            count = uiState.possibleDuplicatesCount,
                            onClick = onNavigateToDuplicates,
                            highlight = uiState.possibleDuplicatesCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricCard(
                            icon = Icons.Default.HealthAndSafety,
                            label = "Incomplete",
                            count = uiState.incompleteContactsCount,
                            onClick = onNavigateToHealth,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Recently Viewed Contacts Section
                    if (uiState.recentlyViewedContacts.isNotEmpty()) {
                        Text(
                            text = "Recently Viewed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(items = uiState.recentlyViewedContacts, key = { it.id }) { summary ->
                                RecentContactAvatarCard(
                                    summary = summary,
                                    onClick = { onContactClick(summary.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentContactAvatarCard(
    summary: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ContactAvatar(
                displayName = summary.displayName,
                photoUri = summary.photoUri,
                size = 48.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ContactVaultTheme {
        HomeScreenContent(
            uiState = HomeDashboardUiState(
                isLoading = false,
                totalContactsCount = 142,
                favoritesCount = 12,
                groupsCount = 5,
                possibleDuplicatesCount = 3,
                incompleteContactsCount = 8,
                recentlyViewedContacts = listOf(
                    ContactSummary(id = 1, lookupKey = "key1", displayName = "Alice Smith", primaryPhone = "+1234567890", primaryEmail = "alice@example.com"),
                    ContactSummary(id = 2, lookupKey = "key2", displayName = "Bob Johnson", primaryPhone = "+1987654321", primaryEmail = "bob@example.com"),
                    ContactSummary(id = 3, lookupKey = "key3", displayName = "Charlie Brown", primaryPhone = "+1555666777", primaryEmail = "charlie@example.com")
                )
            ),
            onRefresh = {},
            onNavigateToContacts = {},
            onNavigateToGroups = {},
            onNavigateToDuplicates = {},
            onNavigateToHealth = {},
            onContactClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardMetricCardPreview() {
    ContactVaultTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardMetricCard(
                icon = Icons.Default.Star,
                label = "Favorites",
                count = 12,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            DashboardMetricCard(
                icon = Icons.Default.FindInPage,
                label = "Duplicates",
                count = 3,
                onClick = {},
                highlight = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
