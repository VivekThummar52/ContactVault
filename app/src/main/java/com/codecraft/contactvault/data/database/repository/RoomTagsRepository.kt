package com.codecraft.contactvault.data.database.repository

import com.codecraft.contactvault.data.database.dao.TagDao
import com.codecraft.contactvault.data.database.entity.ContactTagCrossRef
import com.codecraft.contactvault.data.database.entity.TagEntity
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.model.Tag
import com.codecraft.contactvault.domain.repository.ContactsRepository
import com.codecraft.contactvault.domain.repository.TagsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class RoomTagsRepository(
    private val tagDao: TagDao,
    private val contactsRepository: ContactsRepository
) : TagsRepository {

    override fun getAllTags(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createTag(name: String, colorHex: String): Long {
        val entity = TagEntity(name = name.trim(), colorHex = colorHex)
        return tagDao.insertTag(entity)
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteContactTagCrossRefsByTag(tag.id)
        tagDao.deleteTag(tag.toEntity())
    }

    override fun getTagsForContact(contactId: Long): Flow<List<Tag>> {
        return tagDao.getTagsForContact(contactId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun assignTagToContact(contactId: Long, tagId: Long) {
        tagDao.insertContactTagCrossRef(ContactTagCrossRef(contactId = contactId, tagId = tagId))
    }

    override suspend fun removeTagFromContact(contactId: Long, tagId: Long) {
        tagDao.deleteContactTagCrossRef(ContactTagCrossRef(contactId = contactId, tagId = tagId))
    }

    override fun getContactsForTag(tagId: Long): Flow<List<ContactSummary>> {
        return tagDao.getContactIdsForTag(tagId).flatMapLatest { contactIds ->
            if (contactIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                contactsRepository.getContacts("").map { summaries ->
                    summaries.filter { it.id in contactIds }
                }
            }
        }
    }

    private fun TagEntity.toDomain() = Tag(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt
    )

    private fun Tag.toEntity() = TagEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt
    )
}
