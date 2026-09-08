package com.codecraft.contactvault.domain.repository

import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.ContactSummary
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(query: String = ""): Flow<List<ContactSummary>>
    suspend fun getContactDetails(contactId: Long): Contact?
    suspend fun toggleFavorite(contactId: Long, currentStarredState: Boolean): Result<Boolean>
    suspend fun deleteContact(contactId: Long): Result<Boolean>
}
