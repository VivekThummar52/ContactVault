package com.codecraft.contactvault.presentation.backup

import android.app.Application
import android.content.ContentProviderOperation
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.backup.LocalBackupManager
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.util.VcfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class BackupViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val contactsRepo = AndroidContactsRepository(application)
    private val backupManager = LocalBackupManager(application, contactsRepo, db)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackupToJsonUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val jsonText = backupManager.createBackupJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(jsonText.toByteArray(Charsets.UTF_8))
                    }
                }
                _uiState.update { it.copy(isLoading = false, message = "Local backup saved successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to save backup file") }
            }
        }
    }

    fun restoreBackupFromJsonUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: ""
                }
                val result = backupManager.restoreBackupJson(jsonText)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, message = "Restored ${result.getOrDefault(0)} contacts from backup!") }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.localizedMessage ?: "Failed to restore backup") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to read backup file") }
            }
        }
    }

    fun exportVcfToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val summaries = contactsRepo.getContacts("").first()
                val fullContacts = summaries.mapNotNull { contactsRepo.getContactDetails(it.id) }
                val vcfText = VcfManager.generateVcf(fullContacts)

                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(vcfText.toByteArray(Charsets.UTF_8))
                    }
                }
                _uiState.update { it.copy(isLoading = false, message = "Exported ${fullContacts.size} contacts to VCF file!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to export VCF") }
            }
        }
    }

    fun importVcfFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val vcfText = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: ""
                }
                val contacts = VcfManager.parseVcf(vcfText)
                var importedCount = 0

                withContext(Dispatchers.IO) {
                    contacts.forEach { c ->
                        val ops = ArrayList<ContentProviderOperation>()
                        val rawIndex = ops.size
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                .build()
                        )
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, c.displayName)
                                .build()
                        )
                        c.phoneNumbers.forEach { p ->
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, p.number)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, p.type)
                                    .build()
                            )
                        }
                        c.emailAddresses.forEach { e ->
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIndex)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, e.address)
                                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, e.type)
                                    .build()
                            )
                        }

                        try {
                            getApplication<Application>().contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                            importedCount++
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }

                _uiState.update { it.copy(isLoading = false, message = "Imported $importedCount contacts from VCF!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to import VCF file") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }
}
