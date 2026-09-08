package com.codecraft.contactvault.data.database.repository

import com.codecraft.contactvault.data.database.dao.ContactNoteDao
import com.codecraft.contactvault.data.database.entity.ContactNoteEntity
import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.repository.AppNotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAppNotesRepository(
    private val noteDao: ContactNoteDao
) : AppNotesRepository {

    override fun getNotesForContact(contactId: Long): Flow<List<ContactAppNote>> {
        return noteDao.getNotesForContact(contactId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addNote(contactId: Long, noteText: String): Long {
        val entity = ContactNoteEntity(
            contactId = contactId,
            noteText = noteText.trim(),
            updatedAt = System.currentTimeMillis()
        )
        return noteDao.insertNote(entity)
    }

    override suspend fun updateNote(note: ContactAppNote) {
        val entity = note.toEntity().copy(updatedAt = System.currentTimeMillis())
        noteDao.updateNote(entity)
    }

    override suspend fun deleteNote(note: ContactAppNote) {
        noteDao.deleteNote(note.toEntity())
    }

    private fun ContactNoteEntity.toDomain() = ContactAppNote(
        id = id,
        contactId = contactId,
        noteText = noteText,
        updatedAt = updatedAt
    )

    private fun ContactAppNote.toEntity() = ContactNoteEntity(
        id = id,
        contactId = contactId,
        noteText = noteText,
        updatedAt = updatedAt
    )
}
