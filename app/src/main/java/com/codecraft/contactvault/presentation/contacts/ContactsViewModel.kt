package com.codecraft.contactvault.presentation.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.repository.ContactsRepository
import com.codecraft.contactvault.presentation.common.PermissionUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ContactsViewModel(
    application: Application,
    private val repository: ContactsRepository = AndroidContactsRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    private var loadContactsJob: Job? = null

    init {
        checkPermissionAndLoad()

        _searchQueryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                loadContacts(query)
            }
            .launchIn(viewModelScope)
    }

    fun checkPermissionAndLoad() {
        val hasPermission = PermissionUtils.hasContactsPermission(getApplication())
        _uiState.update { it.copy(hasPermission = hasPermission) }
        if (hasPermission) {
            loadContacts(_uiState.value.searchQuery)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQueryFlow.value = query
    }

    fun setFavoritesFilter(enabled: Boolean) {
        _uiState.update { it.copy(filterFavoritesOnly = enabled) }
    }

    fun toggleFavorite(summary: ContactSummary) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(summary.id, summary.isStarred)
            if (result.isSuccess) {
                val newStarred = result.getOrDefault(!summary.isStarred)
                _uiState.update { currentState ->
                    val updatedList = currentState.contacts.map {
                        if (it.id == summary.id) it.copy(isStarred = newStarred) else it
                    }
                    currentState.copy(contacts = updatedList)
                }
            } else {
                _uiState.update { it.copy(error = "Failed to update favorite status") }
            }
        }
    }

    fun refresh() {
        checkPermissionAndLoad()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadContacts(query: String) {
        if (!_uiState.value.hasPermission) return

        loadContactsJob?.cancel()
        loadContactsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.getContacts(query).collect { contacts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            contacts = contacts
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load contacts"
                    )
                }
            }
        }
    }
}
