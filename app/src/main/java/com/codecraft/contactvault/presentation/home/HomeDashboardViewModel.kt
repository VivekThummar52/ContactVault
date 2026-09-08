package com.codecraft.contactvault.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.data.contacts.AndroidGroupsRepository
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.data.database.repository.RoomRecentlyViewedRepository
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.usecase.AnalyzeContactHealthUseCase
import com.codecraft.contactvault.domain.usecase.DetectDuplicatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeDashboardUiState(
    val isLoading: Boolean = false,
    val totalContactsCount: Int = 0,
    val favoritesCount: Int = 0,
    val groupsCount: Int = 0,
    val possibleDuplicatesCount: Int = 0,
    val incompleteContactsCount: Int = 0,
    val recentlyViewedContacts: List<ContactSummary> = emptyList(),
    val error: String? = null
)

class HomeDashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = ContactVaultDatabase.getDatabase(application)
    private val contactsRepo = AndroidContactsRepository(application)
    private val groupsRepo = AndroidGroupsRepository(application, contactsRepo)
    private val recentlyViewedRepo = RoomRecentlyViewedRepository(db.recentlyViewedDao())
    private val analyzeHealthUseCase = AnalyzeContactHealthUseCase(contactsRepo)
    private val detectDuplicatesUseCase = DetectDuplicatesUseCase(contactsRepo)

    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val allContacts = contactsRepo.getContacts("").first()
                val favorites = allContacts.filter { it.isStarred }
                val groups = groupsRepo.getContactGroups().first()
                val healthReport = analyzeHealthUseCase()
                val duplicates = detectDuplicatesUseCase()

                val recentlyViewedIds = recentlyViewedRepo.getRecentlyViewedContactIds(10).first()
                val recentSummaries = recentlyViewedIds.mapNotNull { id ->
                    allContacts.find { it.id == id }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalContactsCount = allContacts.size,
                        favoritesCount = favorites.size,
                        groupsCount = groups.size,
                        possibleDuplicatesCount = duplicates.size,
                        incompleteContactsCount = healthReport.noPhoneCount + healthReport.incompleteNameCount,
                        recentlyViewedContacts = recentSummaries
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load dashboard data"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
