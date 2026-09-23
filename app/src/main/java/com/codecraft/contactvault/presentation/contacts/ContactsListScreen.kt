package com.codecraft.contactvault.presentation.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codecraft.contactvault.domain.ads.AdPlacement
import com.codecraft.contactvault.domain.ads.NativeAdManager
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.presentation.ads.AdaptiveBannerAd
import com.codecraft.contactvault.presentation.ads.NativeContactAd
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.presentation.common.SleekScrollBar
import com.codecraft.contactvault.presentation.common.ContactsPermissionRequestCard
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun ContactsListScreen(
    viewModel: ContactsViewModel,
    onContactClick: (Long) -> Unit,
    onGroupsClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onHealthClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val nativeAd by NativeAdManager.nativeAdState.collectAsState()

    LaunchedEffect(Unit) {
        NativeAdManager.loadNativeAd(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            NativeAdManager.destroy()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    ContactsListScreenContent(
        uiState = uiState,
        nativeAd = nativeAd,
        onContactClick = onContactClick,
        onFavoriteToggle = { summary -> viewModel.toggleFavorite(summary) },
        onSearchQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
        onSetFavoritesFilter = { favoritesOnly -> viewModel.setFavoritesFilter(favoritesOnly) },
        onRequestPermission = { viewModel.checkPermissionAndLoad() },
        onRefresh = { viewModel.refresh() },
        onGroupsClick = onGroupsClick,
        onTagsClick = onTagsClick,
        onHealthClick = onHealthClick,
        onBackupClick = onBackupClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsListScreenContent(
    uiState: ContactsUiState,
    nativeAd: NativeAd? = null,
    onContactClick: (Long) -> Unit,
    onFavoriteToggle: (ContactSummary) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSetFavoritesFilter: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onGroupsClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onHealthClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ContactVault",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onHealthClick) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = "Contact Health"
                        )
                    }
                    IconButton(onClick = onGroupsClick) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Contact Groups"
                        )
                    }
                    IconButton(onClick = onTagsClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = "Manage Tags"
                        )
                    }
                    IconButton(onClick = onBackupClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Backup & Settings"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh contacts"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AdaptiveBannerAd(
                placement = AdPlacement.CONTACTS_BANNER,
                modifier = Modifier.fillMaxWidth()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.hasPermission) {
                ContactsPermissionRequestCard(
                    onRequestPermission = onRequestPermission,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                // Search Bar & Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, phone, email...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filteredCount = if (uiState.filterFavoritesOnly) {
                            uiState.contacts.count { it.isStarred }
                        } else {
                            uiState.contacts.size
                        }

                        Text(
                            text = if (uiState.isLoading) "Loading..." else "$filteredCount Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilterChip(
                            selected = uiState.filterFavoritesOnly,
                            onClick = { onSetFavoritesFilter(!uiState.filterFavoritesOnly) },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.filterFavoritesOnly) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                if (uiState.isLoading && uiState.contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val displayedContacts = if (uiState.filterFavoritesOnly) {
                        uiState.contacts.filter { it.isStarred }
                    } else {
                        uiState.contacts
                    }

                    val sortedContacts = displayedContacts.sortedBy { contact ->
                        contact.displayName.trim().lowercase()
                    }

                    if (sortedContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty()) "No contacts matching \"${uiState.searchQuery}\"" else "No contacts found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Group by initial letter and sort initial keys strictly alphabetically ('A'..'Z', '#')
                        val grouped: Map<Char, List<ContactSummary>> = sortedContacts.groupBy { contact ->
                            val char = contact.displayName.trim().firstOrNull()?.uppercaseChar() ?: '#'
                            if (char in 'A'..'Z') char else '#'
                        }.toSortedMap(comparator = compareBy { key ->
                            if (key == '#') 'Z' + 1 else key
                        })

                        val listState = rememberLazyListState()

                        val currentSectionLetter = remember(listState.firstVisibleItemIndex, grouped) {
                            var runningCount = 0
                            var foundLetter = ""
                            for ((initial, list) in grouped) {
                                val sectionItemCount = list.size + 1
                                if (listState.firstVisibleItemIndex < runningCount + sectionItemCount) {
                                    foundLetter = initial.toString()
                                    break
                                }
                                runningCount += sectionItemCount
                            }
                            foundLetter
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            var overallContactIndex = 0

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                grouped.forEach { (initial, contactsInGroup) ->
                                    stickyHeader {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = initial.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    contactsInGroup.distinctBy { it.id }.forEach { summary ->
                                        overallContactIndex++
                                        val currentIndex = overallContactIndex

                                        item(key = "${initial}_${summary.id}") {
                                            ContactItemRow(
                                                summary = summary,
                                                onContactClick = { onContactClick(summary.id) },
                                                onFavoriteToggle = { onFavoriteToggle(summary) }
                                            )
                                        }

                                        if (currentIndex % 20 == 0) {
                                            item(key = "native_ad_$currentIndex") {
                                                NativeContactAd(nativeAd = nativeAd)
                                            }
                                        }
                                    }
                                }
                            }

                            SleekScrollBar(
                                listState = listState,
                                sectionLetter = currentSectionLetter,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemRow(
    summary: ContactSummary,
    onContactClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onContactClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            displayName = summary.displayName,
            photoUri = summary.photoUri,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = summary.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleText = summary.organization ?: summary.primaryPhone ?: summary.primaryEmail
            if (!subtitleText.isNullOrBlank()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (summary.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (summary.isStarred) "Remove from favorites" else "Add to favorites",
                tint = if (summary.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsListScreenPreview() {
    ContactVaultTheme {
        ContactsListScreenContent(
            uiState = ContactsUiState(
                isLoading = false,
                contacts = listOf(
                    ContactSummary(id = 1, lookupKey = "k1", displayName = "Alice Smith", primaryPhone = "+1 234 567 8900", isStarred = true),
                    ContactSummary(id = 2, lookupKey = "k2", displayName = "Andrew Miller", primaryPhone = "+1 987 654 3210", organization = "TechCorp"),
                    ContactSummary(id = 3, lookupKey = "k3", displayName = "Bob Jones", primaryEmail = "bob@example.com")
                ),
                searchQuery = "",
                filterFavoritesOnly = false,
                hasPermission = true
            ),
            onContactClick = {},
            onFavoriteToggle = {},
            onSearchQueryChanged = {},
            onSetFavoritesFilter = {},
            onRequestPermission = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactItemRowPreview() {
    ContactVaultTheme {
        ContactItemRow(
            summary = ContactSummary(
                id = 1,
                lookupKey = "k1",
                displayName = "Alice Smith",
                primaryPhone = "+1 234 567 8900",
                organization = "Acme Inc.",
                isStarred = true
            ),
            onContactClick = {},
            onFavoriteToggle = {}
        )
    }
}
