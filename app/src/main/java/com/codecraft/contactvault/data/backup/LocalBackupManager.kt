package com.codecraft.contactvault.data.backup

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.codecraft.contactvault.data.database.ContactVaultDatabase
import com.codecraft.contactvault.data.database.entity.ContactNoteEntity
import com.codecraft.contactvault.data.database.entity.TagEntity
import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.Organization
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.domain.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalBackupManager(
    private val context: Context,
    private val contactsRepository: ContactsRepository,
    private val database: ContactVaultDatabase
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val summaries = contactsRepository.getContacts("").first()
        val fullContacts = summaries.mapNotNull { summary ->
            contactsRepository.getContactDetails(summary.id)
        }

        val tags = database.tagDao().getAllTags().first()
        val appNotesList = mutableListOf<ContactNoteEntity>()
        summaries.forEach { summary ->
            val notes = database.contactNoteDao().getNotesForContact(summary.id).first()
            appNotesList.addAll(notes)
        }

        val root = JSONObject()
        val header = JSONObject().apply {
            put("app", "ContactVault")
            put("version", 1)
            put("createdAt", System.currentTimeMillis())
            put("totalContacts", fullContacts.size)
            put("totalTags", tags.size)
            put("totalNotes", appNotesList.size)
        }
        root.put("header", header)

        // Contacts Array
        val contactsArray = JSONArray()
        fullContacts.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("displayName", c.displayName)
                put("isStarred", c.isStarred)
                
                val phonesArr = JSONArray()
                c.phoneNumbers.forEach { p ->
                    phonesArr.put(JSONObject().apply {
                        put("number", p.number)
                        put("type", p.type)
                        put("label", p.label)
                    })
                }
                put("phones", phonesArr)

                val emailsArr = JSONArray()
                c.emailAddresses.forEach { e ->
                    emailsArr.put(JSONObject().apply {
                        put("address", e.address)
                        put("type", e.type)
                        put("label", e.label)
                    })
                }
                put("emails", emailsArr)

                c.organization?.let { org ->
                    put("orgCompany", org.company)
                    put("orgTitle", org.title)
                }

                val notesArr = JSONArray()
                c.notes.forEach { n -> notesArr.put(n) }
                put("systemNotes", notesArr)
            }
            contactsArray.put(obj)
        }
        root.put("contacts", contactsArray)

        // Tags Array
        val tagsArray = JSONArray()
        tags.forEach { t ->
            tagsArray.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("colorHex", t.colorHex)
            })
        }
        root.put("tags", tagsArray)

        // Local Notes Array
        val notesArray = JSONArray()
        appNotesList.forEach { n ->
            notesArray.put(JSONObject().apply {
                put("contactId", n.contactId)
                put("noteText", n.noteText)
                put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notesArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText)
            val header = root.optJSONObject("header")
            if (header == null || header.optString("app") != "ContactVault") {
                return@withContext Result.failure(Exception("Invalid ContactVault backup file format."))
            }

            val contactsArray = root.optJSONArray("contacts") ?: JSONArray()
            var restoredCount = 0

            for (i in 0 until contactsArray.length()) {
                val cObj = contactsArray.getJSONObject(i)
                val displayName = cObj.optString("displayName")
                if (displayName.isBlank()) continue

                val ops = ArrayList<ContentProviderOperation>()
                val rawContactInsertIndex = ops.size

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )

                // Name
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        .build()
                )

                // Phones
                val phonesArr = cObj.optJSONArray("phones")
                if (phonesArr != null) {
                    for (pIdx in 0 until phonesArr.length()) {
                        val pObj = phonesArr.getJSONObject(pIdx)
                        val num = pObj.optString("number")
                        if (num.isNotBlank()) {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, num)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, pObj.optInt("type", ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))
                                    .build()
                            )
                        }
                    }
                }

                // Emails
                val emailsArr = cObj.optJSONArray("emails")
                if (emailsArr != null) {
                    for (eIdx in 0 until emailsArr.length()) {
                        val eObj = emailsArr.getJSONObject(eIdx)
                        val addr = eObj.optString("address")
                        if (addr.isNotBlank()) {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, addr)
                                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, eObj.optInt("type", ContactsContract.CommonDataKinds.Email.TYPE_OTHER))
                                    .build()
                            )
                        }
                    }
                }

                try {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    restoredCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Restore Tags
            val tagsArray = root.optJSONArray("tags")
            if (tagsArray != null) {
                for (tIdx in 0 until tagsArray.length()) {
                    val tObj = tagsArray.getJSONObject(tIdx)
                    val tagName = tObj.optString("name")
                    val colorHex = tObj.optString("colorHex", "#1E88E5")
                    if (tagName.isNotBlank()) {
                        database.tagDao().insertTag(TagEntity(name = tagName, colorHex = colorHex))
                    }
                }
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
