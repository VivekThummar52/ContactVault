package com.codecraft.contactvault.domain.repository

import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.model.ContactGroup
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun getContactGroups(): Flow<List<ContactGroup>>
    fun getContactsInGroup(groupId: Long): Flow<List<ContactSummary>>
}

interface TagsRepository {
    fun getAllTags(): Flow<List<Tag>>
    suspend fun createTag(name: String, colorHex: String = "#1E88E5"): Long
    suspend fun updateTag(tag: Tag)
    suspend fun deleteTag(tag: Tag)
    fun getTagsForContact(contactId: Long): Flow<List<Tag>>
    suspend fun assignTagToContact(contactId: Long, tagId: Long)
    suspend fun removeTagFromContact(contactId: Long, tagId: Long)
    fun getContactsForTag(tagId: Long): Flow<List<ContactSummary>>
}

interface AppNotesRepository {
    fun getNotesForContact(contactId: Long): Flow<List<ContactAppNote>>
    suspend fun addNote(contactId: Long, noteText: String): Long
    suspend fun updateNote(note: ContactAppNote)
    suspend fun deleteNote(note: ContactAppNote)
}

interface RecentlyViewedRepository {
    fun getRecentlyViewedContactIds(limit: Int = 10): Flow<List<Long>>
    suspend fun markContactViewed(contactId: Long)
}
