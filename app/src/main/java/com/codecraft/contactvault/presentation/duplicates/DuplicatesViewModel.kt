package com.codecraft.contactvault.presentation.duplicates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codecraft.contactvault.data.contacts.AndroidContactsRepository
import com.codecraft.contactvault.domain.model.DuplicatePair
import com.codecraft.contactvault.domain.usecase.DetectDuplicatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicatesUiState(
    val isLoading: Boolean = false,
    val duplicatePairs: List<DuplicatePair> = emptyList(),
    val error: String? = null
)

class DuplicatesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val contactsRepo = AndroidContactsRepository(application)
    private val detectDuplicatesUseCase = DetectDuplicatesUseCase(contactsRepo)

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        loadDuplicates()
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val pairs = detectDuplicatesUseCase()
                _uiState.update { it.copy(isLoading = false, duplicatePairs = pairs) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to detect duplicates") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
