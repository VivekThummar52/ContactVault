package com.codecraft.contactvault.presentation.contacts

import com.codecraft.contactvault.domain.model.ContactSummary

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactSummary> = emptyList(),
    val searchQuery: String = "",
    val filterFavoritesOnly: Boolean = false,
    val hasPermission: Boolean = true,
    val error: String? = null
)
