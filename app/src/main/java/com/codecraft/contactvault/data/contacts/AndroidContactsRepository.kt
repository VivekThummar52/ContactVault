package com.codecraft.contactvault.data.contacts

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.Organization
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.domain.model.PostalAddress
import com.codecraft.contactvault.domain.model.Website
import com.codecraft.contactvault.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AndroidContactsRepository(
    private val context: Context
) : ContactsRepository {

    private val contentResolver: ContentResolver get() = context.contentResolver

    override fun getContacts(query: String): Flow<List<ContactSummary>> = flow {
        val contacts = fetchContactSummaries(query)
        emit(contacts)
    }.flowOn(Dispatchers.IO)

    private fun fetchContactSummaries(query: String): List<ContactSummary> {
        val trimmedQuery = query.trim()
        val matchingContactIds = if (trimmedQuery.isNotEmpty()) {
            findContactIdsMatchingQuery(trimmedQuery)
        } else {
            null
        }

        if (matchingContactIds != null && matchingContactIds.isEmpty()) {
            return emptyList()
        }

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        val selection = if (matchingContactIds != null) {
            "${ContactsContract.Contacts._ID} IN (${matchingContactIds.joinToString(",")})"
        } else {
            null
        }
        val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"

        val contactMap = LinkedHashMap<Long, ContactSummaryBuilder>()

        try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val lookupKey = if (lookupIdx >= 0) cursor.getString(lookupIdx) ?: "" else ""
                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    val isStarred = if (starredIdx >= 0) cursor.getInt(starredIdx) == 1 else false

                    contactMap[id] = ContactSummaryBuilder(
                        id = id,
                        lookupKey = lookupKey,
                        displayName = displayName,
                        photoUri = photoUri,
                        isStarred = isStarred
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        if (contactMap.isEmpty()) {
            return emptyList()
        }

        // Fetch primary phones in batch
        val contactIds = contactMap.keys.toList()
        populatePrimaryPhones(contactIds, contactMap)
        populatePrimaryEmails(contactIds, contactMap)
        populateOrganizations(contactIds, contactMap)

        return contactMap.values.map { builder ->
            ContactSummary(
                id = builder.id,
                lookupKey = builder.lookupKey,
                displayName = builder.displayName,
                photoUri = builder.photoUri,
                isStarred = builder.isStarred,
                primaryPhone = builder.primaryPhone,
                primaryEmail = builder.primaryEmail,
                organization = builder.organization
            )
        }
    }

    private fun findContactIdsMatchingQuery(query: String): Set<Long> {
        val ids = mutableSetOf<Long>()
        val wildQuery = "%$query%"

        // 1. Search names
        try {
            val filterUri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_FILTER_URI,
                Uri.encode(query)
            )
            contentResolver.query(
                filterUri,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idIdx))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Search Phone numbers
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
                arrayOf(wildQuery),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idIdx))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Search Emails
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID),
                "${ContactsContract.CommonDataKinds.Email.ADDRESS} LIKE ?",
                arrayOf(wildQuery),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idIdx))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Search Organizations
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data.CONTACT_ID),
                "${ContactsContract.Data.MIMETYPE} = ? AND (${ContactsContract.CommonDataKinds.Organization.COMPANY} LIKE ? OR ${ContactsContract.CommonDataKinds.Organization.TITLE} LIKE ?)",
                arrayOf(
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                    wildQuery,
                    wildQuery
                ),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idIdx))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ids
    }

    private fun populatePrimaryPhones(contactIds: List<Long>, map: Map<Long, ContactSummaryBuilder>) {
        contactIds.chunked(200).forEach { chunk ->
            val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN (${chunk.joinToString(",")})"
            try {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                    ),
                    selection,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
                )?.use { cursor ->
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(contactIdIdx)
                        val number = cursor.getString(numberIdx)
                        val builder = map[contactId]
                        if (builder != null && builder.primaryPhone == null && !number.isNullOrBlank()) {
                            builder.primaryPhone = number
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun populatePrimaryEmails(contactIds: List<Long>, map: Map<Long, ContactSummaryBuilder>) {
        contactIds.chunked(200).forEach { chunk ->
            val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} IN (${chunk.joinToString(",")})"
            try {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        ContactsContract.CommonDataKinds.Email.IS_PRIMARY
                    ),
                    selection,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.IS_PRIMARY} DESC"
                )?.use { cursor ->
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(contactIdIdx)
                        val email = cursor.getString(emailIdx)
                        val builder = map[contactId]
                        if (builder != null && builder.primaryEmail == null && !email.isNullOrBlank()) {
                            builder.primaryEmail = email
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun populateOrganizations(contactIds: List<Long>, map: Map<Long, ContactSummaryBuilder>) {
        contactIds.chunked(200).forEach { chunk ->
            val selection = "${ContactsContract.Data.CONTACT_ID} IN (${chunk.joinToString(",")}) AND ${ContactsContract.Data.MIMETYPE} = ?"
            try {
                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.CommonDataKinds.Organization.TITLE
                    ),
                    selection,
                    arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                    null
                )?.use { cursor ->
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                    val companyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                    val titleIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)

                    while (cursor.moveToNext()) {
                        val contactId = cursor.getLong(contactIdIdx)
                        val company = cursor.getString(companyIdx)
                        val title = cursor.getString(titleIdx)
                        val builder = map[contactId]
                        if (builder != null && builder.organization == null) {
                            val orgText = listOfNotNull(company, title).filter { it.isNotBlank() }.joinToString(" • ")
                            if (orgText.isNotBlank()) {
                                builder.organization = orgText
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getContactDetails(contactId: Long): Contact? = withContext(Dispatchers.IO) {
        var displayName = "Unknown"
        var photoUri: String? = null
        var isStarred = false
        var lookupKey = ""

        // Query base contact details
        try {
            val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                    val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
                    val lookupIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: "Unknown"
                    if (photoIdx >= 0) photoUri = cursor.getString(photoIdx)
                    if (starredIdx >= 0) isStarred = cursor.getInt(starredIdx) == 1
                    if (lookupIdx >= 0) lookupKey = cursor.getString(lookupIdx) ?: ""
                } else {
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        val phones = mutableListOf<PhoneNumber>()
        val emails = mutableListOf<EmailAddress>()
        val postalAddresses = mutableListOf<PostalAddress>()
        val websites = mutableListOf<Website>()
        val notes = mutableListOf<String>()
        var organization: Organization? = null
        var accountName: String? = null
        var accountType: String? = null

        // Query Data table for details
        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data._ID,
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    ContactsContract.CommonDataKinds.Email.TYPE,
                    ContactsContract.CommonDataKinds.Email.LABEL,
                    ContactsContract.CommonDataKinds.Organization.COMPANY,
                    ContactsContract.CommonDataKinds.Organization.TITLE,
                    ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                    ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                    ContactsContract.CommonDataKinds.StructuredPostal.LABEL,
                    ContactsContract.CommonDataKinds.Website.URL,
                    ContactsContract.CommonDataKinds.Note.NOTE,
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val phoneNumIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val phoneTypeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val phoneLabelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                val emailAddrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                val emailTypeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
                val emailLabelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)

                val companyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                val titleIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)

                val postalAddrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                val postalTypeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
                val postalLabelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)

                val urlIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL)
                val noteIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)

                val accNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val dataId = cursor.getLong(idIdx)
                    val mimeType = cursor.getString(mimeIdx)

                    if (accountName == null && accNameIdx >= 0) {
                        accountName = cursor.getString(accNameIdx)
                    }
                    if (accountType == null && accTypeIdx >= 0) {
                        accountType = cursor.getString(accTypeIdx)
                    }

                    when (mimeType) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val number = cursor.getString(phoneNumIdx)
                            if (!number.isNullOrBlank()) {
                                val type = cursor.getInt(phoneTypeIdx)
                                val label = cursor.getString(phoneLabelIdx)
                                phones.add(PhoneNumber(id = dataId, number = number, type = type, label = label))
                            }
                        }
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val address = cursor.getString(emailAddrIdx)
                            if (!address.isNullOrBlank()) {
                                val type = cursor.getInt(emailTypeIdx)
                                val label = cursor.getString(emailLabelIdx)
                                emails.add(EmailAddress(id = dataId, address = address, type = type, label = label))
                            }
                        }
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            val company = cursor.getString(companyIdx)
                            val title = cursor.getString(titleIdx)
                            if (!company.isNullOrBlank() || !title.isNullOrBlank()) {
                                organization = Organization(company = company, title = title)
                            }
                        }
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            val address = cursor.getString(postalAddrIdx)
                            if (!address.isNullOrBlank()) {
                                val type = cursor.getInt(postalTypeIdx)
                                val label = cursor.getString(postalLabelIdx)
                                postalAddresses.add(PostalAddress(id = dataId, formattedAddress = address, type = type, label = label))
                            }
                        }
                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                            val url = cursor.getString(urlIdx)
                            if (!url.isNullOrBlank()) {
                                websites.add(Website(url = url))
                            }
                        }
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            val note = cursor.getString(noteIdx)
                            if (!note.isNullOrBlank()) {
                                notes.add(note)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Contact(
            id = contactId,
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri,
            isStarred = isStarred,
            phoneNumbers = phones,
            emailAddresses = emails,
            organization = organization,
            postalAddresses = postalAddresses,
            websites = websites,
            notes = notes,
            accountType = accountType,
            accountName = accountName
        )
    }

    override suspend fun toggleFavorite(contactId: Long, currentStarredState: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (currentStarredState) 0 else 1)
            }
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val updated = contentResolver.update(uri, contentValues, null, null)
            if (updated > 0) {
                Result.success(!currentStarredState)
            } else {
                Result.failure(Exception("Could not update contact favorite status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(contactId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val deleted = contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                Result.success(true)
            } else {
                Result.failure(Exception("Contact not found or could not be deleted"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class ContactSummaryBuilder(
        val id: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?,
        val isStarred: Boolean,
        var primaryPhone: String? = null,
        var primaryEmail: String? = null,
        var organization: String? = null
    )
}
