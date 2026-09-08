package com.codecraft.contactvault.presentation.duplicates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.MergePreview
import com.codecraft.contactvault.domain.usecase.SafeMergeContactsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicateComparisonUiState(
    val isLoading: Boolean = false,
    val contactA: Contact? = null,
    val contactB: Contact? = null,
    val preview: MergePreview? = null,
    val isMerged: Boolean = false,
    val error: String? = null
)

class DuplicateComparisonViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val contactsRepo = AndroidContactsRepository(application)
    private val safeMergeUseCase = SafeMergeContactsUseCase(
        contactsRepository = contactsRepo,
        snapshotDao = db.deletedContactSnapshotDao(),
        tagDao = db.tagDao(),
        noteDao = db.contactNoteDao()
    )

    private val _uiState = MutableStateFlow(DuplicateComparisonUiState())
    val uiState: StateFlow<DuplicateComparisonUiState> = _uiState.asStateFlow()

    fun loadComparisonPair(contactIdA: Long, contactIdB: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val cA = contactsRepo.getContactDetails(contactIdA)
                val cB = contactsRepo.getContactDetails(contactIdB)
                val preview = safeMergeUseCase.previewMerge(contactIdA, contactIdB)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contactA = cA,
                        contactB = cB,
                        preview = preview
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load comparison data") }
            }
        }
    }

    fun confirmMerge(onSuccess: () -> Unit) {
        val preview = _uiState.value.preview ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = safeMergeUseCase.executeMerge(
                primaryContactId = preview.primaryContactId,
                secondaryContactId = preview.secondaryContactId
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isMerged = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Merge operation failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
