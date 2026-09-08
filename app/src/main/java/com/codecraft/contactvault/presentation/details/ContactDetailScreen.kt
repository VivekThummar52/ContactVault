package com.codecraft.contactvault.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.Organization
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.domain.model.PostalAddress
import com.codecraft.contactvault.domain.model.Tag
import com.codecraft.contactvault.domain.model.Website
import com.codecraft.contactvault.presentation.common.ContactAvatar
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import com.codecraft.contactvault.util.ContactUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactDetailScreen(
    contactId: Long,
    viewModel: ContactDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    ContactDetailScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onToggleFavorite = { viewModel.toggleFavorite() },
        onDeleteContact = { viewModel.deleteContact(onSuccess = onBackClick) },
        onTagToggle = { tag, isAssigned ->
            if (isAssigned) viewModel.assignTag(tag) else viewModel.removeTag(tag)
        },
        onAddAppNote = { noteText -> viewModel.addAppNote(noteText) },
        onDeleteAppNote = { note -> viewModel.deleteAppNote(note) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreenContent(
    uiState: ContactDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDeleteContact: () -> Unit = {},
    onTagToggle: (tag: Tag, isAssigned: Boolean) -> Unit = { _, _ -> },
    onAddAppNote: (noteText: String) -> Unit = {},
    onDeleteAppNote: (note: ContactAppNote) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val contact = uiState.contact

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Contact Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (contact != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (contact.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (contact.isStarred) "Remove Favorite" else "Favorite",
                                tint = if (contact.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { ContactUtils.shareContact(context, contact) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading && contact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (contact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Contact not found.",
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
                // Header: Avatar, Name, Org
                ContactAvatar(
                    displayName = contact.displayName,
                    photoUri = contact.photoUri,
                    size = 110.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (contact.organization != null) {
                    val orgText = listOfNotNull(contact.organization.company, contact.organization.title)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (orgText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = orgText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (!contact.accountName.isNullOrBlank() || !contact.accountType.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val accountText = listOfNotNull(contact.accountName, contact.accountType)
                        .filter { it.isNotBlank() }
                        .joinToString(" (") + if (!contact.accountType.isNullOrBlank()) ")" else ""
                    AssistChip(
                        onClick = { },
                        label = { Text("Source: $accountText", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tags Section
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.assignedTags.forEach { tag ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(tag.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        AssistChip(
                            onClick = { showTagDialog = true },
                            label = { Text(tag.name, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        )
                    }

                    AssistChip(
                        onClick = { showTagDialog = true },
                        label = { Text("Add Tag") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Tag", modifier = Modifier.size(16.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val primaryPhone = contact.phoneNumbers.firstOrNull()?.number
                    val primaryEmail = contact.emailAddresses.firstOrNull()?.address

                    QuickActionButton(
                        icon = Icons.Default.Call,
                        label = "Call",
                        enabled = !primaryPhone.isNullOrBlank(),
                        onClick = { primaryPhone?.let { ContactUtils.dialPhoneNumber(context, it) } }
                    )
                    QuickActionButton(
                        icon = Icons.AutoMirrored.Filled.Message,
                        label = "SMS",
                        enabled = !primaryPhone.isNullOrBlank(),
                        onClick = { primaryPhone?.let { ContactUtils.sendSms(context, it) } }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Email,
                        label = "Email",
                        enabled = !primaryEmail.isNullOrBlank(),
                        onClick = { primaryEmail?.let { ContactUtils.sendEmail(context, it) } }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        enabled = true,
                        onClick = { ContactUtils.shareContact(context, contact) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ContactVault Local Application Notes Card
                DetailSectionCard(
                    title = "ContactVault Notes (Local)",
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    action = {
                        IconButton(onClick = { showAddNoteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Local Note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    if (uiState.appNotes.isEmpty()) {
                        Text(
                            text = "No application notes added. Tap '+' to record offline notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val dateFormat = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
                        uiState.appNotes.forEach { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.noteText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Updated: ${dateFormat.format(Date(note.updatedAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteAppNote(note) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Note",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Numbers Card
                if (contact.phoneNumbers.isNotEmpty()) {
                    DetailSectionCard(
                        title = "Phone Numbers",
                        icon = Icons.Default.Phone
                    ) {
                        contact.phoneNumbers.forEach { phone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = phone.number,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val label = ContactUtils.getPhoneTypeLabel(phone.type, phone.label)
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { ContactUtils.dialPhoneNumber(context, phone.number) }) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call ${phone.number}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { ContactUtils.sendSms(context, phone.number) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Message,
                                        contentDescription = "SMS ${phone.number}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Emails Card
                if (contact.emailAddresses.isNotEmpty()) {
                    DetailSectionCard(
                        title = "Email Addresses",
                        icon = Icons.Default.Email
                    ) {
                        contact.emailAddresses.forEach { email ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = email.address,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val label = ContactUtils.getEmailTypeLabel(email.type, email.label)
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { ContactUtils.sendEmail(context, email.address) }) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Send Email to ${email.address}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Organization Card
                if (contact.organization != null) {
                    val company = contact.organization.company
                    val title = contact.organization.title
                    if (!company.isNullOrBlank() || !title.isNullOrBlank()) {
                        DetailSectionCard(
                            title = "Organization",
                            icon = Icons.Default.Business
                        ) {
                            if (!company.isNullOrBlank()) {
                                Text(
                                    text = company,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Postal Addresses Card
                if (contact.postalAddresses.isNotEmpty()) {
                    DetailSectionCard(
                        title = "Addresses",
                        icon = Icons.Default.LocationOn
                    ) {
                        contact.postalAddresses.forEach { postal ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = postal.formattedAddress,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                val label = ContactUtils.getPostalTypeLabel(postal.type, postal.label)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Websites Card
                if (contact.websites.isNotEmpty()) {
                    DetailSectionCard(
                        title = "Websites",
                        icon = Icons.Default.Language
                    ) {
                        contact.websites.forEach { website ->
                            Text(
                                text = website.url,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // System Notes Card
                if (contact.notes.isNotEmpty()) {
                    DetailSectionCard(
                        title = "System Provider Notes",
                        icon = Icons.AutoMirrored.Filled.Notes
                    ) {
                        contact.notes.forEach { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete Contact?") },
            text = {
                Text(
                    text = "Are you sure you want to delete ${contact?.displayName ?: "this contact"} from your device contacts?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteContact()
                    }
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showTagDialog) {
        TagSelectionDialog(
            assignedTags = uiState.assignedTags,
            allTags = uiState.allAvailableTags,
            onDismiss = { showTagDialog = false },
            onTagToggle = onTagToggle
        )
    }

    if (showAddNoteDialog) {
        AddAppNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { noteText ->
                showAddNoteDialog = false
                onAddAppNote(noteText)
            }
        )
    }
}

@Composable
fun TagSelectionDialog(
    assignedTags: List<Tag>,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onTagToggle: (tag: Tag, isAssigned: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Contact Tags") },
        text = {
            if (allTags.isEmpty()) {
                Text("No custom tags exist. Go to Tags menu to create custom tags first.")
            } else {
                Column {
                    allTags.forEach { tag ->
                        val isAssigned = assignedTags.any { it.id == tag.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAssigned,
                                onCheckedChange = { checked ->
                                    onTagToggle(tag, checked)
                                }
                            )
                            val parseColor = try {
                                Color(android.graphics.Color.parseColor(tag.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(parseColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tag.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun AddAppNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (noteText: String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Local Application Note") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note content...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (action != null) {
                    action()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactDetailScreenPreview() {
    ContactVaultTheme {
        ContactDetailScreenContent(
            uiState = ContactDetailUiState(
                isLoading = false,
                contact = Contact(
                    id = 1,
                    lookupKey = "key1",
                    displayName = "Alice Smith",
                    isStarred = true,
                    phoneNumbers = listOf(
                        PhoneNumber(id = 101, number = "+1 (555) 019-2834", type = 2, label = "Mobile"),
                        PhoneNumber(id = 102, number = "+1 (555) 012-3456", type = 3, label = "Work")
                    ),
                    emailAddresses = listOf(
                        EmailAddress(id = 201, address = "alice.smith@example.com", type = 1, label = "Home")
                    ),
                    organization = Organization(company = "Acme Corp", title = "Lead Software Engineer"),
                    postalAddresses = listOf(
                        PostalAddress(id = 301, formattedAddress = "123 Innovation Way, San Francisco, CA", type = 1)
                    ),
                    websites = listOf(Website("https://alicesmith.dev")),
                    notes = listOf("Met at Tech Summit 2025.")
                ),
                assignedTags = listOf(
                    Tag(id = 1, name = "Work", colorHex = "#1E88E5"),
                    Tag(id = 2, name = "Important", colorHex = "#E53935")
                ),
                allAvailableTags = listOf(
                    Tag(id = 1, name = "Work", colorHex = "#1E88E5"),
                    Tag(id = 2, name = "Important", colorHex = "#E53935"),
                    Tag(id = 3, name = "Family", colorHex = "#43A047")
                ),
                appNotes = listOf(
                    ContactAppNote(id = 10, contactId = 1, noteText = "Follow up regarding Q3 roadmap next Tuesday.", updatedAt = System.currentTimeMillis())
                )
            ),
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TagSelectionDialogPreview() {
    ContactVaultTheme {
        TagSelectionDialog(
            assignedTags = listOf(Tag(id = 1, name = "Work", colorHex = "#1E88E5")),
            allTags = listOf(
                Tag(id = 1, name = "Work", colorHex = "#1E88E5"),
                Tag(id = 2, name = "Family", colorHex = "#43A047"),
                Tag(id = 3, name = "VIP", colorHex = "#E53935")
            ),
            onDismiss = {},
            onTagToggle = { _, _ -> }
        )
    }
}
