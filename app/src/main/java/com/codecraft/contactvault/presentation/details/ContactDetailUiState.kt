package com.codecraft.contactvault.presentation.details

import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.model.Tag

data class ContactDetailUiState(
    val isLoading: Boolean = false,
    val contact: Contact? = null,
    val assignedTags: List<Tag> = emptyList(),
    val allAvailableTags: List<Tag> = emptyList(),
    val appNotes: List<ContactAppNote> = emptyList(),
    val isDeleted: Boolean = false,
    val error: String? = null
)
