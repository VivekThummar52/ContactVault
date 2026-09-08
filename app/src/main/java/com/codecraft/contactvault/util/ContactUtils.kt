package com.codecraft.contactvault.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.codecraft.contactvault.domain.model.Contact

object ContactUtils {

    fun getPhoneTypeLabel(type: Int, customLabel: String?): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel ?: "Custom"
            else -> "Phone"
        }
    }

    fun getEmailTypeLabel(type: Int, customLabel: String?): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM -> customLabel ?: "Custom"
            else -> "Email"
        }
    }

    fun getPostalTypeLabel(type: Int, customLabel: String?): String {
        return when (type) {
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM -> customLabel ?: "Custom"
            else -> "Address"
        }
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendSms(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber.trim()}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendEmail(context: Context, emailAddress: String) {
        if (emailAddress.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${emailAddress.trim()}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareContact(context: Context, contact: Contact) {
        try {
            val sb = StringBuilder()
            sb.appendLine("Contact Details:")
            sb.appendLine("Name: ${contact.displayName}")
            if (contact.phoneNumbers.isNotEmpty()) {
                sb.appendLine("Phone(s):")
                contact.phoneNumbers.forEach { phone ->
                    val label = getPhoneTypeLabel(phone.type, phone.label)
                    sb.appendLine("  - $label: ${phone.number}")
                }
            }
            if (contact.emailAddresses.isNotEmpty()) {
                sb.appendLine("Email(s):")
                contact.emailAddresses.forEach { email ->
                    val label = getEmailTypeLabel(email.type, email.label)
                    sb.appendLine("  - $label: ${email.address}")
                }
            }
            if (contact.organization != null) {
                val org = listOfNotNull(contact.organization.company, contact.organization.title).joinToString(", ")
                if (org.isNotBlank()) {
                    sb.appendLine("Organization: $org")
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, contact.displayName)
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share contact via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
