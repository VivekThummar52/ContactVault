package com.codecraft.contactvault.domain.model

data class ContactGroup(
    val id: Long,
    val title: String,
    val accountName: String? = null,
    val accountType: String? = null,
    val memberCount: Int = 0
)

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#1E88E5",
    val createdAt: Long = System.currentTimeMillis()
)

data class ContactAppNote(
    val id: Long = 0,
    val contactId: Long,
    val noteText: String,
    val updatedAt: Long = System.currentTimeMillis()
)
