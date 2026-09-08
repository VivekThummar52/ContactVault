package com.codecraft.contactvault.domain.model

data class ContactHealthReport(
    val totalContacts: Int = 0,
    val healthScore: Int = 100,
    val noPhoneCount: Int = 0,
    val noEmailCount: Int = 0,
    val incompleteNameCount: Int = 0,
    val possibleDuplicateCount: Int = 0,
    val multiplePhonesCount: Int = 0,
    val multipleEmailsCount: Int = 0,
    val contactsNoPhone: List<ContactSummary> = emptyList(),
    val contactsNoEmail: List<ContactSummary> = emptyList(),
    val contactsIncompleteName: List<ContactSummary> = emptyList()
)

data class DuplicatePair(
    val id: String,
    val contactA: ContactSummary,
    val contactB: ContactSummary,
    val matchReason: String,
    val confidenceScore: Int,
    val isConfirmed: Boolean
)

data class MergePreview(
    val primaryContactId: Long,
    val secondaryContactId: Long,
    val combinedDisplayName: String,
    val combinedPhotoUri: String?,
    val combinedPhones: List<PhoneNumber>,
    val combinedEmails: List<EmailAddress>,
    val combinedOrganization: Organization?,
    val combinedAddresses: List<PostalAddress>,
    val combinedWebsites: List<Website>,
    val combinedNotes: List<String>
)
