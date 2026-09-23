package com.codecraft.contactvault.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.padding
import com.codecraft.contactvault.domain.ads.AdManager
import com.codecraft.contactvault.domain.ads.AdPlacement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codecraft.contactvault.presentation.backup.BackupSettingsScreen
import com.codecraft.contactvault.presentation.backup.BackupViewModel
import com.codecraft.contactvault.presentation.common.ContactsPermissionScreen
import com.codecraft.contactvault.presentation.common.PermissionUtils
import com.codecraft.contactvault.presentation.contacts.ContactsListScreen
import com.codecraft.contactvault.presentation.contacts.ContactsViewModel
import com.codecraft.contactvault.presentation.details.ContactDetailScreen
import com.codecraft.contactvault.presentation.details.ContactDetailViewModel
import com.codecraft.contactvault.presentation.duplicates.DuplicateComparisonScreen
import com.codecraft.contactvault.presentation.duplicates.DuplicateComparisonViewModel
import com.codecraft.contactvault.presentation.duplicates.DuplicatesScreen
import com.codecraft.contactvault.presentation.duplicates.DuplicatesViewModel
import com.codecraft.contactvault.presentation.groups.GroupsScreen
import com.codecraft.contactvault.presentation.groups.GroupsViewModel
import com.codecraft.contactvault.presentation.health.HealthScreen
import com.codecraft.contactvault.presentation.health.HealthViewModel
import com.codecraft.contactvault.presentation.home.HomeDashboardViewModel
import com.codecraft.contactvault.presentation.home.HomeScreen
import com.codecraft.contactvault.presentation.recovery.RecentlyDeletedScreen
import com.codecraft.contactvault.presentation.recovery.RecoveryViewModel
import com.codecraft.contactvault.presentation.tags.TagsScreen
import com.codecraft.contactvault.presentation.tags.TagsViewModel

object ContactVaultDestinations {
    const val HOME = "home"
    const val CONTACTS_LIST = "contacts_list?favorites={favorites}"
    const val CONTACT_DETAIL = "contact_detail/{contactId}"
    const val GROUPS = "groups"
    const val TAGS = "tags"
    const val HEALTH = "health"
    const val DUPLICATES = "duplicates"
    const val DUPLICATE_COMPARISON = "duplicate_comparison/{contactIdA}/{contactIdB}"
    const val BACKUP = "backup"
    const val RECOVERY = "recovery"

    fun contactsListRoute(favorites: Boolean = false) = "contacts_list?favorites=$favorites"
    fun contactDetailRoute(contactId: Long) = "contact_detail/$contactId"
    fun duplicateComparisonRoute(contactIdA: Long, contactIdB: Long) = "duplicate_comparison/$contactIdA/$contactIdB"
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(ContactVaultDestinations.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(ContactVaultDestinations.contactsListRoute(false), "Contacts", Icons.Filled.Contacts, Icons.Outlined.Contacts),
    BottomNavItem(ContactVaultDestinations.HEALTH, "Health", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety),
    BottomNavItem(ContactVaultDestinations.BACKUP, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun ContactVaultNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasContactsPermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionUtils.hasContactsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission) {
        ContactsPermissionScreen(
            onPermissionGranted = {
                hasPermission = true
            }
        )
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestinationRoute = navBackStackEntry?.destination?.route

        val showBottomBar = currentDestinationRoute?.substringBefore("?") in listOf(
            ContactVaultDestinations.HOME,
            ContactVaultDestinations.CONTACTS_LIST.substringBefore("?"),
            ContactVaultDestinations.HEALTH,
            ContactVaultDestinations.BACKUP
        )

        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        BOTTOM_NAV_ITEMS.forEach { item ->
                            val selected = currentDestinationRoute?.substringBefore("?") == item.route.substringBefore("?")
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        val activity = context.findActivity()
                                        AdManager.recordAction()
                                        if (item.route.substringBefore("?") == ContactVaultDestinations.HOME) {
                                            AdManager.tryShowInterstitial(activity, AdPlacement.INTERSTITIAL_AFTER_BROWSING) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        } else {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ContactVaultDestinations.HOME,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(ContactVaultDestinations.HOME) {
                    val viewModel: HomeDashboardViewModel = viewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToContacts = { favoritesOnly ->
                            navController.navigate(ContactVaultDestinations.contactsListRoute(favoritesOnly)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToGroups = {
                            navController.navigate(ContactVaultDestinations.GROUPS)
                        },
                        onNavigateToDuplicates = {
                            navController.navigate(ContactVaultDestinations.DUPLICATES)
                        },
                        onNavigateToHealth = {
                            navController.navigate(ContactVaultDestinations.HEALTH) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onContactClick = { contactId ->
                            navController.navigate(ContactVaultDestinations.contactDetailRoute(contactId))
                        }
                    )
                }

                composable(
                    route = ContactVaultDestinations.CONTACTS_LIST,
                    arguments = listOf(
                        navArgument("favorites") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val favoritesOnly = backStackEntry.arguments?.getBoolean("favorites") ?: false
                    val viewModel: ContactsViewModel = viewModel()
                    LaunchedEffect(favoritesOnly) {
                        viewModel.setFavoritesFilter(favoritesOnly)
                    }
                    ContactsListScreen(
                        viewModel = viewModel,
                        onContactClick = { contactId ->
                            navController.navigate(ContactVaultDestinations.contactDetailRoute(contactId))
                        },
                        onGroupsClick = {
                            navController.navigate(ContactVaultDestinations.GROUPS)
                        },
                        onTagsClick = {
                            navController.navigate(ContactVaultDestinations.TAGS)
                        },
                        onHealthClick = {
                            navController.navigate(ContactVaultDestinations.HEALTH) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onBackupClick = {
                            navController.navigate(ContactVaultDestinations.BACKUP) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(
                    route = ContactVaultDestinations.CONTACT_DETAIL,
                    arguments = listOf(
                        navArgument("contactId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getLong("contactId") ?: -1L
                    val viewModel: ContactDetailViewModel = viewModel()
                    ContactDetailScreen(
                        contactId = contactId,
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.GROUPS) {
                    val viewModel: GroupsViewModel = viewModel()
                    GroupsScreen(
                        viewModel = viewModel,
                        onContactClick = { contactId ->
                            navController.navigate(ContactVaultDestinations.contactDetailRoute(contactId))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.TAGS) {
                    val viewModel: TagsViewModel = viewModel()
                    TagsScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.HEALTH) {
                    val viewModel: HealthViewModel = viewModel()
                    HealthScreen(
                        viewModel = viewModel,
                        onReviewDuplicatesClick = {
                            navController.navigate(ContactVaultDestinations.DUPLICATES)
                        },
                        onContactClick = { contactId ->
                            navController.navigate(ContactVaultDestinations.contactDetailRoute(contactId))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.DUPLICATES) {
                    val viewModel: DuplicatesViewModel = viewModel()
                    DuplicatesScreen(
                        viewModel = viewModel,
                        onCompareClick = { contactIdA, contactIdB ->
                            navController.navigate(ContactVaultDestinations.duplicateComparisonRoute(contactIdA, contactIdB))
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = ContactVaultDestinations.DUPLICATE_COMPARISON,
                    arguments = listOf(
                        navArgument("contactIdA") { type = NavType.LongType },
                        navArgument("contactIdB") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val contactIdA = backStackEntry.arguments?.getLong("contactIdA") ?: -1L
                    val contactIdB = backStackEntry.arguments?.getLong("contactIdB") ?: -1L
                    val viewModel: DuplicateComparisonViewModel = viewModel()
                    DuplicateComparisonScreen(
                        contactIdA = contactIdA,
                        contactIdB = contactIdB,
                        viewModel = viewModel,
                        onMergeSuccess = {
                            navController.popBackStack()
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.BACKUP) {
                    val viewModel: BackupViewModel = viewModel()
                    BackupSettingsScreen(
                        viewModel = viewModel,
                        onRecentlyDeletedClick = {
                            navController.navigate(ContactVaultDestinations.RECOVERY)
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(ContactVaultDestinations.RECOVERY) {
                    val viewModel: RecoveryViewModel = viewModel()
                    RecentlyDeletedScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
