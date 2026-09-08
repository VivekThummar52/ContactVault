package com.codecraft.contactvault.util

import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.Organization
import com.codecraft.contactvault.domain.model.PhoneNumber
import android.provider.ContactsContract

object VcfManager {

    fun generateVcf(contacts: List<Contact>): String {
        val sb = StringBuilder()
        contacts.forEach { contact ->
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            sb.appendLine("FN:${contact.displayName.escapeVcf()}")
            
            // Name parts
            val nameParts = contact.displayName.trim().split(" ")
            val lastName = if (nameParts.size > 1) nameParts.last() else ""
            val firstName = if (nameParts.size > 1) nameParts.dropLast(1).joinToString(" ") else contact.displayName
            sb.appendLine("N:${lastName.escapeVcf()};${firstName.escapeVcf()};;;")

            contact.phoneNumbers.forEach { phone ->
                val typeLabel = when (phone.type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "CELL"
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "HOME"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "WORK"
                    else -> "VOICE"
                }
                sb.appendLine("TEL;TYPE=$typeLabel:${phone.number.trim()}")
            }

            contact.emailAddresses.forEach { email ->
                sb.appendLine("EMAIL;TYPE=INTERNET:${email.address.trim()}")
            }

            contact.organization?.let { org ->
                if (!org.company.isNullOrBlank()) {
                    sb.appendLine("ORG:${org.company.escapeVcf()}")
                }
                if (!org.title.isNullOrBlank()) {
                    sb.appendLine("TITLE:${org.title.escapeVcf()}")
                }
            }

            if (contact.notes.isNotEmpty()) {
                val combinedNotes = contact.notes.joinToString(" \\n ")
                sb.appendLine("NOTE:${combinedNotes.escapeVcf()}")
            }

            sb.appendLine("END:VCARD")
        }
        return sb.toString()
    }

    fun parseVcf(vcfText: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val vcardBlocks = vcfText.split("BEGIN:VCARD").filter { it.contains("END:VCARD") }

        var tempId = 1L
        vcardBlocks.forEach { block ->
            var displayName = ""
            val phones = mutableListOf<PhoneNumber>()
            val emails = mutableListOf<EmailAddress>()
            var company: String? = null
            var title: String? = null
            val notes = mutableListOf<String>()

            block.lines().forEach { rawLine ->
                val line = rawLine.trim()
                when {
                    line.startsWith("FN:", ignoreCase = true) -> {
                        displayName = line.substringAfter(":").trim().unescapeVcf()
                    }
                    line.startsWith("TEL", ignoreCase = true) -> {
                        val num = line.substringAfter(":").trim()
                        if (num.isNotBlank()) {
                            phones.add(PhoneNumber(id = tempId++, number = num, type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))
                        }
                    }
                    line.startsWith("EMAIL", ignoreCase = true) -> {
                        val addr = line.substringAfter(":").trim()
                        if (addr.isNotBlank()) {
                            emails.add(EmailAddress(id = tempId++, address = addr, type = ContactsContract.CommonDataKinds.Email.TYPE_OTHER))
                        }
                    }
                    line.startsWith("ORG:", ignoreCase = true) -> {
                        company = line.substringAfter(":").trim().unescapeVcf()
                    }
                    line.startsWith("TITLE:", ignoreCase = true) -> {
                        title = line.substringAfter(":").trim().unescapeVcf()
                    }
                    line.startsWith("NOTE:", ignoreCase = true) -> {
                        val note = line.substringAfter(":").trim().unescapeVcf()
                        if (note.isNotBlank()) {
                            notes.add(note)
                        }
                    }
                }
            }

            if (displayName.isNotBlank() || phones.isNotEmpty() || emails.isNotEmpty()) {
                val finalName = if (displayName.isNotBlank()) displayName else phones.firstOrNull()?.number ?: "Imported Contact"
                val org = if (company != null || title != null) Organization(company = company, title = title) else null
                contacts.add(
                    Contact(
                        id = tempId++,
                        lookupKey = "vcf_$tempId",
                        displayName = finalName,
                        phoneNumbers = phones,
                        emailAddresses = emails,
                        organization = org,
                        notes = notes
                    )
                )
            }
        }

        return contacts
    }

    private fun String.escapeVcf(): String = this.replace("\n", "\\n").replace(";", "\\;")
    private fun String.unescapeVcf(): String = this.replace("\\n", "\n").replace("\\;", ";")
}
