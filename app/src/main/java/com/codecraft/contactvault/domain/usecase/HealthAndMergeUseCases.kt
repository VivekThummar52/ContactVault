package com.codecraft.contactvault.domain.usecase

import com.codecraft.contactvault.data.database.dao.DeletedContactSnapshotDao
import com.codecraft.contactvault.data.database.dao.TagDao
import com.codecraft.contactvault.data.database.dao.ContactNoteDao
import com.codecraft.contactvault.data.database.entity.ContactTagCrossRef
import com.codecraft.contactvault.data.database.entity.DeletedContactSnapshotEntity
import com.codecraft.contactvault.domain.model.ContactHealthReport
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.model.DuplicatePair
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.MergePreview
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class AnalyzeContactHealthUseCase(
    private val contactsRepository: ContactsRepository
) {
    suspend operator fun invoke(): ContactHealthReport {
        val contacts = contactsRepository.getContacts("").first()
        if (contacts.isEmpty()) {
            return ContactHealthReport()
        }

        val noPhone = contacts.filter { it.primaryPhone.isNullOrBlank() }
        val noEmail = contacts.filter { it.primaryEmail.isNullOrBlank() }
        val incompleteName = contacts.filter {
            val name = it.displayName.trim()
            name.isBlank() || name.length <= 2 || name.all { char -> !char.isLetter() }
        }

        val duplicates = DetectDuplicatesUseCase(contactsRepository).invoke()

        val total = contacts.size
        val issuesCount = noPhone.size + noEmail.size + incompleteName.size + duplicates.size
        val maxIssuesPossible = total * 3
        val rawScore = if (maxIssuesPossible > 0) {
            100 - ((issuesCount.toDouble() / maxIssuesPossible.toDouble()) * 100).toInt()
        } else {
            100
        }
        val healthScore = rawScore.coerceIn(10, 100)

        return ContactHealthReport(
            totalContacts = total,
            healthScore = healthScore,
            noPhoneCount = noPhone.size,
            noEmailCount = noEmail.size,
            incompleteNameCount = incompleteName.size,
            possibleDuplicateCount = duplicates.size,
            contactsNoPhone = noPhone,
            contactsNoEmail = noEmail,
            contactsIncompleteName = incompleteName
        )
    }
}

class DetectDuplicatesUseCase(
    private val contactsRepository: ContactsRepository
) {
    suspend operator fun invoke(): List<DuplicatePair> {
        val contacts = contactsRepository.getContacts("").first()
        val duplicatePairs = mutableListOf<DuplicatePair>()
        val processedPairKeys = mutableSetOf<String>()

        // 1. Same normalized phone number
        val phoneMap = mutableMapOf<String, MutableList<ContactSummary>>()
        contacts.forEach { contact ->
            val rawPhone = contact.primaryPhone
            if (!rawPhone.isNullOrBlank()) {
                val normalized = normalizePhoneNumber(rawPhone)
                if (normalized.length >= 7) {
                    phoneMap.getOrPut(normalized) { mutableListOf() }.add(contact)
                }
            }
        }

        phoneMap.values.filter { it.size > 1 }.forEach { list ->
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val c1 = list[i]
                    val c2 = list[j]
                    val pairKey = getPairKey(c1.id, c2.id)
                    if (pairKey !in processedPairKeys) {
                        processedPairKeys.add(pairKey)
                        duplicatePairs.add(
                            DuplicatePair(
                                id = pairKey,
                                contactA = c1,
                                contactB = c2,
                                matchReason = "Phone Number (${c1.primaryPhone})",
                                confidenceScore = 95,
                                isConfirmed = true
                            )
                        )
                    }
                }
            }
        }

        // 2. Same normalized email
        val emailMap = mutableMapOf<String, MutableList<ContactSummary>>()
        contacts.forEach { contact ->
            val email = contact.primaryEmail
            if (!email.isNullOrBlank()) {
                val normalized = email.trim().lowercase()
                emailMap.getOrPut(normalized) { mutableListOf() }.add(contact)
            }
        }

        emailMap.values.filter { it.size > 1 }.forEach { list ->
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val c1 = list[i]
                    val c2 = list[j]
                    val pairKey = getPairKey(c1.id, c2.id)
                    if (pairKey !in processedPairKeys) {
                        processedPairKeys.add(pairKey)
                        duplicatePairs.add(
                            DuplicatePair(
                                id = pairKey,
                                contactA = c1,
                                contactB = c2,
                                matchReason = "Same Email Address (${c1.primaryEmail})",
                                confidenceScore = 90,
                                isConfirmed = true
                            )
                        )
                    }
                }
            }
        }

        // 3. Exact Display Name match
        val nameMap = mutableMapOf<String, MutableList<ContactSummary>>()
        contacts.forEach { contact ->
            val name = contact.displayName.trim().lowercase()
            if (name.length > 2) {
                nameMap.getOrPut(name) { mutableListOf() }.add(contact)
            }
        }

        nameMap.values.filter { it.size > 1 }.forEach { list ->
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val c1 = list[i]
                    val c2 = list[j]
                    val pairKey = getPairKey(c1.id, c2.id)
                    if (pairKey !in processedPairKeys) {
                        processedPairKeys.add(pairKey)
                        duplicatePairs.add(
                            DuplicatePair(
                                id = pairKey,
                                contactA = c1,
                                contactB = c2,
                                matchReason = "Exact Name Match (\"${c1.displayName}\")",
                                confidenceScore = 80,
                                isConfirmed = false
                            )
                        )
                    }
                }
            }
        }

        return duplicatePairs.sortedByDescending { it.confidenceScore }
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "").takeLast(10)
    }

    private fun getPairKey(id1: Long, id2: Long): String {
        val min = minOf(id1, id2)
        val max = maxOf(id1, id2)
        return "${min}_$max"
    }
}

class SafeMergeContactsUseCase(
    private val contactsRepository: ContactsRepository,
    private val snapshotDao: DeletedContactSnapshotDao,
    private val tagDao: TagDao,
    private val noteDao: ContactNoteDao
) {
    suspend fun previewMerge(primaryContactId: Long, secondaryContactId: Long): MergePreview? {
        val contactA = contactsRepository.getContactDetails(primaryContactId) ?: return null
        val contactB = contactsRepository.getContactDetails(secondaryContactId) ?: return null

        val combinedDisplayName = if (contactA.displayName.isNotBlank()) contactA.displayName else contactB.displayName
        val combinedPhotoUri = contactA.photoUri ?: contactB.photoUri

        // Combine Phone Numbers without duplicates
        val phoneSet = mutableSetOf<String>()
        val combinedPhones = mutableListOf<PhoneNumber>()
        (contactA.phoneNumbers + contactB.phoneNumbers).forEach { phone ->
            val norm = phone.number.replace(Regex("[^0-9]"), "").takeLast(10)
            if (norm.isNotBlank() && norm !in phoneSet) {
                phoneSet.add(norm)
                combinedPhones.add(phone)
            }
        }

        // Combine Email Addresses without duplicates
        val emailSet = mutableSetOf<String>()
        val combinedEmails = mutableListOf<EmailAddress>()
        (contactA.emailAddresses + contactB.emailAddresses).forEach { email ->
            val norm = email.address.trim().lowercase()
            if (norm !in emailSet) {
                emailSet.add(norm)
                combinedEmails.add(email)
            }
        }

        val combinedOrganization = contactA.organization ?: contactB.organization
        val combinedAddresses = (contactA.postalAddresses + contactB.postalAddresses).distinctBy { it.formattedAddress.trim() }
        val combinedWebsites = (contactA.websites + contactB.websites).distinctBy { it.url.trim().lowercase() }
        val combinedNotes = (contactA.notes + contactB.notes).distinct()

        return MergePreview(
            primaryContactId = primaryContactId,
            secondaryContactId = secondaryContactId,
            combinedDisplayName = combinedDisplayName,
            combinedPhotoUri = combinedPhotoUri,
            combinedPhones = combinedPhones,
            combinedEmails = combinedEmails,
            combinedOrganization = combinedOrganization,
            combinedAddresses = combinedAddresses,
            combinedWebsites = combinedWebsites,
            combinedNotes = combinedNotes
        )
    }

    suspend fun executeMerge(primaryContactId: Long, secondaryContactId: Long): Result<Boolean> {
        return try {
            val contactB = contactsRepository.getContactDetails(secondaryContactId)
                ?: return Result.failure(Exception("Secondary contact no longer exists."))

            // 1. Capture original snapshot in Room before deletion
            val json = JSONObject().apply {
                put("id", contactB.id)
                put("displayName", contactB.displayName)
                put("photoUri", contactB.photoUri)
                val phonesArr = JSONArray()
                contactB.phoneNumbers.forEach { p ->
                    phonesArr.put(JSONObject().apply {
                        put("number", p.number)
                        put("type", p.type)
                        put("label", p.label)
                    })
                }
                put("phoneNumbers", phonesArr)

                val emailsArr = JSONArray()
                contactB.emailAddresses.forEach { e ->
                    emailsArr.put(JSONObject().apply {
                        put("address", e.address)
                        put("type", e.type)
                        put("label", e.label)
                    })
                }
                put("emailAddresses", emailsArr)
            }.toString()

            snapshotDao.insertSnapshot(
                DeletedContactSnapshotEntity(
                    originalContactId = secondaryContactId,
                    displayName = contactB.displayName,
                    contactJson = json,
                    reason = "MERGED_INTO_$primaryContactId"
                )
            )

            // 2. Re-link local tags from Contact B to Contact A
            val tagsForB = tagDao.getTagsForContact(secondaryContactId).first()
            tagsForB.forEach { tag ->
                tagDao.insertContactTagCrossRef(ContactTagCrossRef(contactId = primaryContactId, tagId = tag.id))
            }

            // 3. Delete secondary contact from Contacts Provider
            val deleteResult = contactsRepository.deleteContact(secondaryContactId)
            if (deleteResult.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(deleteResult.exceptionOrNull() ?: Exception("Failed to delete duplicate record."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
