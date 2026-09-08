package com.codecraft.contactvault.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codecraft.contactvault.data.database.entity.ContactNoteEntity
import com.codecraft.contactvault.data.database.entity.ContactTagCrossRef
import com.codecraft.contactvault.data.database.entity.DeletedContactSnapshotEntity
import com.codecraft.contactvault.data.database.entity.RecentlyViewedEntity
import com.codecraft.contactvault.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM contact_tags WHERE tagId = :tagId")
    suspend fun deleteContactTagCrossRefsByTag(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContactTagCrossRef(crossRef: ContactTagCrossRef)

    @Delete
    suspend fun deleteContactTagCrossRef(crossRef: ContactTagCrossRef)

    @Query("SELECT t.* FROM tags t INNER JOIN contact_tags ct ON t.id = ct.tagId WHERE ct.contactId = :contactId ORDER BY t.name ASC")
    fun getTagsForContact(contactId: Long): Flow<List<TagEntity>>

    @Query("SELECT contactId FROM contact_tags WHERE tagId = :tagId")
    fun getContactIdsForTag(tagId: Long): Flow<List<Long>>
}

@Dao
interface ContactNoteDao {
    @Query("SELECT * FROM contact_notes WHERE contactId = :contactId ORDER BY updatedAt DESC")
    fun getNotesForContact(contactId: Long): Flow<List<ContactNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ContactNoteEntity): Long

    @Update
    suspend fun updateNote(note: ContactNoteEntity)

    @Delete
    suspend fun deleteNote(note: ContactNoteEntity)

    @Query("DELETE FROM contact_notes WHERE contactId = :contactId")
    suspend fun deleteAllNotesForContact(contactId: Long)
}

@Dao
interface RecentlyViewedDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecentlyViewed(limit: Int = 10): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyViewed(entity: RecentlyViewedEntity)
}

@Dao
interface DeletedContactSnapshotDao {
    @Query("SELECT * FROM deleted_contact_snapshots ORDER BY deletedAt DESC")
    fun getAllSnapshots(): Flow<List<DeletedContactSnapshotEntity>>

    @Query("SELECT * FROM deleted_contact_snapshots WHERE id = :id")
    suspend fun getSnapshotById(id: Long): DeletedContactSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: DeletedContactSnapshotEntity): Long

    @Delete
    suspend fun deleteSnapshot(snapshot: DeletedContactSnapshotEntity)

    @Query("DELETE FROM deleted_contact_snapshots")
    suspend fun deleteAllSnapshots()
}
