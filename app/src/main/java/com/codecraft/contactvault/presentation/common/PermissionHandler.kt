package com.codecraft.contactvault.presentation.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

object PermissionUtils {
    private const val PREFS_NAME = "contact_vault_permissions"
    private const val KEY_REQUESTED = "requested_contacts_permission"

    val CONTACT_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS
    )

    fun hasContactsPermission(context: Context): Boolean {
        return CONTACT_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasRequestedPermissionBefore(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REQUESTED, false)
    }

    fun setRequestedPermissionBefore(context: Context, value: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REQUESTED, value).apply()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shouldShowRationale(context: Context): Boolean {
        val activity = context as? Activity ?: return false
        return CONTACT_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun isPermanentlyDenied(context: Context): Boolean {
        if (hasContactsPermission(context)) return false
        val requestedBefore = hasRequestedPermissionBefore(context)
        val shouldShowRat = shouldShowRationale(context)
        return requestedBefore && !shouldShowRat
    }
}

@Composable
fun ContactsPermissionScreen(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPermanentlyDenied by remember {
        mutableStateOf(PermissionUtils.isPermanentlyDenied(context))
    }

    // Re-check permission automatically when app resumes (e.g. returning from System Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionUtils.hasContactsPermission(context)) {
                    onPermissionGranted()
                } else {
                    isPermanentlyDenied = PermissionUtils.isPermanentlyDenied(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        PermissionUtils.setRequestedPermissionBefore(context, true)
        if (PermissionUtils.hasContactsPermission(context)) {
            onPermissionGranted()
        } else {
            isPermanentlyDenied = PermissionUtils.isPermanentlyDenied(context)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Contacts Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isPermanentlyDenied) {
                        "Contacts permission was permanently denied or disabled. ContactVault requires Contacts access to function. Please tap 'Open App Settings' below to enable Contacts access in System Settings."
                    } else {
                        "ContactVault is a privacy-first, offline contacts manager. To display, organize, search, and back up your address book, ContactVault requires permission to access your contacts."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Privacy callout banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "100% Offline & Private: No internet permission required. Your contacts never leave your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (isPermanentlyDenied) {
                            PermissionUtils.openAppSettings(context)
                        } else {
                            PermissionUtils.setRequestedPermissionBefore(context, true)
                            permissionLauncher.launch(PermissionUtils.CONTACT_PERMISSIONS)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.Contacts,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPermanentlyDenied) "Open App Settings" else "Grant Contacts Access",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ContactsPermissionRequestCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPermanentlyDenied by remember {
        mutableStateOf(PermissionUtils.isPermanentlyDenied(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        PermissionUtils.setRequestedPermissionBefore(context, true)
        if (PermissionUtils.hasContactsPermission(context)) {
            onRequestPermission()
        } else {
            isPermanentlyDenied = PermissionUtils.isPermanentlyDenied(context)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Contacts Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isPermanentlyDenied) {
                    "Contacts permission was permanently denied. Please enable Contacts permission in System Settings."
                } else {
                    "ContactVault is an offline contacts manager. Grant contacts permission so you can view, search, and manage your contacts safely on device."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        PermissionUtils.openAppSettings(context)
                    } else {
                        PermissionUtils.setRequestedPermissionBefore(context, true)
                        permissionLauncher.launch(PermissionUtils.CONTACT_PERMISSIONS)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.Contacts,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isPermanentlyDenied) "Open App Settings" else "Grant Permission")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsPermissionRequestCardPreview() {
    ContactVaultTheme {
        ContactsPermissionRequestCard(
            onRequestPermission = {}
        )
    }
}
