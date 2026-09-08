package com.codecraft.contactvault.presentation.groups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidGroupsRepository
import com.codecraft.contactvault.domain.model.ContactGroup
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val isLoading: Boolean = false,
    val groups: List<ContactGroup> = emptyList(),
    val selectedGroup: ContactGroup? = null,
    val groupContacts: List<ContactSummary> = emptyList(),
    val error: String? = null
)

class GroupsViewModel(
    application: Application,
    private val groupsRepository: GroupsRepository = AndroidGroupsRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                groupsRepository.getContactGroups().collect { groupsList ->
                    _uiState.update { it.copy(isLoading = false, groups = groupsList) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load groups") }
            }
        }
    }

    fun selectGroup(group: ContactGroup) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedGroup = group, isLoading = true, groupContacts = emptyList()) }
            try {
                groupsRepository.getContactsInGroup(group.id).collect { contacts ->
                    _uiState.update { it.copy(isLoading = false, groupContacts = contacts) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load group contacts") }
            }
        }
    }

    fun clearSelectedGroup() {
        _uiState.update { it.copy(selectedGroup = null, groupContacts = emptyList()) }
    }
}
