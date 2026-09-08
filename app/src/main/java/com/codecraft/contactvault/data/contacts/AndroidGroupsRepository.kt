package com.codecraft.contactvault.data.contacts

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.codecraft.contactvault.domain.model.ContactGroup
import com.codecraft.contactvault.domain.model.ContactSummary
import com.codecraft.contactvault.domain.repository.ContactsRepository
import com.codecraft.contactvault.domain.repository.GroupsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AndroidGroupsRepository(
    private val context: Context,
    private val contactsRepository: ContactsRepository = AndroidContactsRepository(context)
) : GroupsRepository {

    private val contentResolver: ContentResolver get() = context.contentResolver

    override fun getContactGroups(): Flow<List<ContactGroup>> = flow {
        val groups = fetchContactGroups()
        emit(groups)
    }.flowOn(Dispatchers.IO)

    override fun getContactsInGroup(groupId: Long): Flow<List<ContactSummary>> = flow {
        val contactIds = fetchContactIdsInGroup(groupId)
        if (contactIds.isEmpty()) {
            emit(emptyList())
        } else {
            val filterQuery = ""
            // We retrieve contacts and filter by group contact IDs
            contactsRepository.getContacts(filterQuery).collect { allContacts ->
                val groupContacts = allContacts.filter { it.id in contactIds }
                emit(groupContacts)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchContactGroups(): List<ContactGroup> {
        val groups = mutableListOf<ContactGroup>()
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.ACCOUNT_NAME,
            ContactsContract.Groups.ACCOUNT_TYPE
        )

        try {
            contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                projection,
                "${ContactsContract.Groups.DELETED} = 0",
                null,
                "${ContactsContract.Groups.TITLE} COLLATE LOCALIZED ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Groups._ID)
                val titleIdx = cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                val accNameIdx = cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val title = cursor.getString(titleIdx) ?: "Unnamed Group"
                    val accName = if (accNameIdx >= 0) cursor.getString(accNameIdx) else null
                    val accType = if (accTypeIdx >= 0) cursor.getString(accTypeIdx) else null

                    if (!title.startsWith("Group:") && !title.startsWith("System Group:")) {
                        groups.add(
                            ContactGroup(
                                id = id,
                                title = title,
                                accountName = accName,
                                accountType = accType,
                                memberCount = 0
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return groups
    }

    private suspend fun fetchContactIdsInGroup(groupId: Long): Set<Long> = withContext(Dispatchers.IO) {
        val contactIds = mutableSetOf<Long>()
        val projection = arrayOf(ContactsContract.Data.CONTACT_ID)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
            groupId.toString()
        )

        try {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                while (cursor.moveToNext()) {
                    contactIds.add(cursor.getLong(idIdx))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contactIds
    }
}
