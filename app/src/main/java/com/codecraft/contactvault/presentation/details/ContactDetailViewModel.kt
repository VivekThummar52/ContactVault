package com.codecraft.contactvault.presentation.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.data.database.repository.RoomAppNotesRepository
import com.codecraft.contactvault.data.database.repository.RoomRecentlyViewedRepository
import com.codecraft.contactvault.data.database.repository.RoomTagsRepository
import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.model.Tag
import com.codecraft.contactvault.domain.repository.AppNotesRepository
import com.codecraft.contactvault.domain.repository.ContactsRepository
import com.codecraft.contactvault.domain.repository.RecentlyViewedRepository
import com.codecraft.contactvault.domain.repository.TagsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactDetailViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val contactsRepo: ContactsRepository = AndroidContactsRepository(application)
    private val tagsRepo: TagsRepository = RoomTagsRepository(db.tagDao(), contactsRepo)
    private val notesRepo: AppNotesRepository = RoomAppNotesRepository(db.contactNoteDao())
    private val recentlyViewedRepo: RecentlyViewedRepository = RoomRecentlyViewedRepository(db.recentlyViewedDao())

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    fun loadContact(contactId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Mark contact viewed
                recentlyViewedRepo.markContactViewed(contactId)

                val contact = contactsRepo.getContactDetails(contactId)
                if (contact != null) {
                    _uiState.update { it.copy(isLoading = false, contact = contact) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Contact not found or has been deleted."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load contact details."
                    )
                }
            }
        }

        // Observe tags
        viewModelScope.launch {
            tagsRepo.getTagsForContact(contactId).collect { assigned ->
                _uiState.update { it.copy(assignedTags = assigned) }
            }
        }

        // Observe all available tags
        viewModelScope.launch {
            tagsRepo.getAllTags().collect { all ->
                _uiState.update { it.copy(allAvailableTags = all) }
            }
        }

        // Observe local app notes
        viewModelScope.launch {
            notesRepo.getNotesForContact(contactId).collect { notes ->
                _uiState.update { it.copy(appNotes = notes) }
            }
        }
    }

    fun toggleFavorite() {
        val currentContact = _uiState.value.contact ?: return
        viewModelScope.launch {
            val result = contactsRepo.toggleFavorite(currentContact.id, currentContact.isStarred)
            if (result.isSuccess) {
                val newStarred = result.getOrDefault(!currentContact.isStarred)
                _uiState.update {
                    it.copy(contact = currentContact.copy(isStarred = newStarred))
                }
            } else {
                _uiState.update { it.copy(error = "Failed to update favorite status.") }
            }
        }
    }

    fun assignTag(tag: Tag) {
        val contactId = _uiState.value.contact?.id ?: return
        viewModelScope.launch {
            tagsRepo.assignTagToContact(contactId, tag.id)
        }
    }

    fun removeTag(tag: Tag) {
        val contactId = _uiState.value.contact?.id ?: return
        viewModelScope.launch {
            tagsRepo.removeTagFromContact(contactId, tag.id)
        }
    }

    fun addAppNote(noteText: String) {
        val contactId = _uiState.value.contact?.id ?: return
        if (noteText.isBlank()) return
        viewModelScope.launch {
            notesRepo.addNote(contactId, noteText)
        }
    }

    fun deleteAppNote(note: ContactAppNote) {
        viewModelScope.launch {
            notesRepo.deleteNote(note)
        }
    }

    fun deleteContact(onSuccess: () -> Unit) {
        val currentContact = _uiState.value.contact ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = contactsRepo.deleteContact(currentContact.id)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.localizedMessage ?: "Failed to delete contact."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
