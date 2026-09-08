package com.codecraft.contactvault.presentation.recovery

import android.app.Application
import android.content.ContentProviderOperation
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.data.database.entity.DeletedContactSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class RecoveryUiState(
    val isLoading: Boolean = false,
    val snapshots: List<DeletedContactSnapshotEntity> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class RecoveryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val snapshotDao = db.deletedContactSnapshotDao()

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    init {
        loadSnapshots()
    }

    fun loadSnapshots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                snapshotDao.getAllSnapshots().collect { list ->
                    _uiState.update { it.copy(isLoading = false, snapshots = list) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load recovery snapshots") }
            }
        }
    }

    fun restoreSnapshot(snapshot: DeletedContactSnapshotEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = restoreContactFromSnapshot(snapshot)
            if (result.isSuccess) {
                snapshotDao.deleteSnapshot(snapshot)
                _uiState.update { it.copy(isLoading = false, message = "Restored ${snapshot.displayName} successfully!") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to restore contact"
                    )
                }
            }
        }
    }

    fun deleteSnapshotPermanently(snapshot: DeletedContactSnapshotEntity) {
        viewModelScope.launch {
            try {
                snapshotDao.deleteSnapshot(snapshot)
                _uiState.update { it.copy(message = "Permanently deleted snapshot.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to delete snapshot") }
            }
        }
    }

    fun clearAllSnapshots() {
        viewModelScope.launch {
            try {
                snapshotDao.deleteAllSnapshots()
                _uiState.update { it.copy(message = "All recovery snapshots cleared.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to clear snapshots") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    private suspend fun restoreContactFromSnapshot(snapshot: DeletedContactSnapshotEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(snapshot.contactJson)
            val displayName = json.optString("displayName", snapshot.displayName)

            val ops = ArrayList<ContentProviderOperation>()
            val rawContactInsertIndex = ops.size

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )

            // Phones
            val phonesArr = json.optJSONArray("phoneNumbers") ?: json.optJSONArray("phones")
            if (phonesArr != null) {
                for (i in 0 until phonesArr.length()) {
                    val pObj = phonesArr.getJSONObject(i)
                    val num = pObj.optString("number")
                    if (num.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, pObj.optInt("type", ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))
                                .build()
                        )
                    }
                }
            }

            // Emails
            val emailsArr = json.optJSONArray("emailAddresses") ?: json.optJSONArray("emails")
            if (emailsArr != null) {
                for (i in 0 until emailsArr.length()) {
                    val eObj = emailsArr.getJSONObject(i)
                    val addr = eObj.optString("address")
                    if (addr.isNotBlank()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, addr)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eObj.optInt("type", ContactsContract.CommonDataKinds.Email.TYPE_OTHER))
                                .build()
                        )
                    }
                }
            }

            getApplication<Application>().contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
