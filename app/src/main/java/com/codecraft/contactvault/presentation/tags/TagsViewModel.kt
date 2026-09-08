package com.codecraft.contactvault.presentation.tags

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.data.database.repository.RoomTagsRepository
import com.codecraft.contactvault.domain.model.Tag
import com.codecraft.contactvault.domain.repository.TagsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagsUiState(
    val isLoading: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val selectedTagFilter: Tag? = null,
    val error: String? = null
)

class TagsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val contactsRepo = AndroidContactsRepository(application)
    private val tagsRepository: TagsRepository = RoomTagsRepository(db.tagDao(), contactsRepo)

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    fun loadTags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                tagsRepository.getAllTags().collect { tagList ->
                    _uiState.update { it.copy(isLoading = false, tags = tagList) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load tags") }
            }
        }
    }

    fun createTag(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                tagsRepository.createTag(name, colorHex)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to create tag") }
            }
        }
    }

    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagsRepository.updateTag(tag)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to update tag") }
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            try {
                tagsRepository.deleteTag(tag)
                if (_uiState.value.selectedTagFilter?.id == tag.id) {
                    _uiState.update { it.copy(selectedTagFilter = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to delete tag") }
            }
        }
    }

    fun setSelectedTagFilter(tag: Tag?) {
        _uiState.update { it.copy(selectedTagFilter = tag) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
