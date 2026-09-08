package com.codecraft.contactvault.domain.model

data class ContactSummary(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val isStarred: Boolean = false,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val organization: String? = null
)

data class Contact(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val isStarred: Boolean = false,
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    val emailAddresses: List<EmailAddress> = emptyList(),
    val organization: Organization? = null,
    val postalAddresses: List<PostalAddress> = emptyList(),
    val websites: List<Website> = emptyList(),
    val notes: List<String> = emptyList(),
    val accountType: String? = null,
    val accountName: String? = null,
    val lastUpdatedTimestamp: Long? = null
)

data class PhoneNumber(
    val id: Long,
    val number: String,
    val type: Int,
    val label: String? = null
)

data class EmailAddress(
    val id: Long,
    val address: String,
    val type: Int,
    val label: String? = null
)

data class Organization(
    val company: String? = null,
    val title: String? = null
)

data class PostalAddress(
    val id: Long,
    val formattedAddress: String,
    val type: Int,
    val label: String? = null
)

data class Website(
    val url: String
)
