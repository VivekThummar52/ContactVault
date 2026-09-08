package com.codecraft.contactvault.presentation.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.domain.model.ContactHealthReport
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.usecase.AnalyzeContactHealthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HealthFilterCategory {
    ALL_ISSUES,
    NO_PHONE,
    NO_EMAIL,
    INCOMPLETE_NAME
}

data class HealthUiState(
    val isLoading: Boolean = false,
    val report: ContactHealthReport = ContactHealthReport(),
    val selectedCategory: HealthFilterCategory? = null,
    val filteredContacts: List<ContactSummary> = emptyList(),
    val error: String? = null
)

class HealthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val contactsRepo = AndroidContactsRepository(application)
    private val analyzeHealthUseCase = AnalyzeContactHealthUseCase(contactsRepo)

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        analyzeHealth()
    }

    fun analyzeHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val report = analyzeHealthUseCase()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        filteredContacts = when (it.selectedCategory) {
                            HealthFilterCategory.NO_PHONE -> report.contactsNoPhone
                            HealthFilterCategory.NO_EMAIL -> report.contactsNoEmail
                            HealthFilterCategory.INCOMPLETE_NAME -> report.contactsIncompleteName
                            else -> emptyList()
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to analyze contact health") }
            }
        }
    }

    fun selectCategory(category: HealthFilterCategory?) {
        _uiState.update { currentState ->
            val report = currentState.report
            val list = when (category) {
                HealthFilterCategory.NO_PHONE -> report.contactsNoPhone
                HealthFilterCategory.NO_EMAIL -> report.contactsNoEmail
                HealthFilterCategory.INCOMPLETE_NAME -> report.contactsIncompleteName
                else -> emptyList()
            }
            currentState.copy(
                selectedCategory = category,
                filteredContacts = list
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
