package com.codecraft.contactvault.domain.model

data class DeletedSnapshot(
    val id: Long,
    val originalContactId: Long,
    val displayName: String,
    val contactJson: String,
    val reason: String,
    val deletedAt: Long
)

data class BackupHeader(
    val app: String = "ContactVault",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val totalContacts: Int = 0,
    val totalTags: Int = 0,
    val totalNotes: Int = 0
)
