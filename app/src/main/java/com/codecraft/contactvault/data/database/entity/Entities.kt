package com.codecraft.contactvault.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#1E88E5",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contact_tags",
    primaryKeys = ["contactId", "tagId"],
    indices = [Index("tagId")]
)
data class ContactTagCrossRef(
    val contactId: Long,
    val tagId: Long
)

@Entity(
    tableName = "contact_notes",
    indices = [Index("contactId")]
)
data class ContactNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey
    val contactId: Long,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "deleted_contact_snapshots")
data class DeletedContactSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalContactId: Long,
    val displayName: String,
    val contactJson: String,
    val reason: String = "MERGED", // "DELETED" or "MERGED"
    val deletedAt: Long = System.currentTimeMillis()
)
