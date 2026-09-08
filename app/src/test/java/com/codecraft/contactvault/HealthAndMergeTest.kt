package com.codecraft.contactvault

import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthAndMergeTest {

    @Test
    fun testPhoneNumberNormalization() {
        val rawPhone1 = "+91 (98765) 12345"
        val rawPhone2 = "98765-12345"

        val norm1 = rawPhone1.replace(Regex("[^0-9]"), "").takeLast(10)
        val norm2 = rawPhone2.replace(Regex("[^0-9]"), "").takeLast(10)

        assertEquals("9876512345", norm1)
        assertEquals("9876512345", norm2)
        assertEquals(norm1, norm2)
    }

    @Test
    fun testPhoneMergeDeduplication() {
        val phonesA = listOf(
            PhoneNumber(1, "+91 98765 12345", 2, "Mobile"),
            PhoneNumber(2, "022 123456", 1, "Home")
        )
        val phonesB = listOf(
            PhoneNumber(3, "9876512345", 2, "Mobile"), // Duplicate normalized number
            PhoneNumber(4, "+91 99999 88888", 3, "Work") // Unique number
        )

        val phoneSet = mutableSetOf<String>()
        val combinedPhones = mutableListOf<PhoneNumber>()
        (phonesA + phonesB).forEach { phone ->
            val norm = phone.number.replace(Regex("[^0-9]"), "").takeLast(10)
            if (norm !in phoneSet) {
                phoneSet.add(norm)
                combinedPhones.add(phone)
            }
        }

        assertEquals(3, combinedPhones.size)
        assertTrue(combinedPhones.any { it.number.contains("99999") })
    }

    @Test
    fun testEmailMergeDeduplication() {
        val emailsA = listOf(EmailAddress(1, "rahul@gmail.com", 1, "Personal"))
        val emailsB = listOf(
            EmailAddress(2, "RAHUL@GMAIL.COM", 1, "Personal"), // Duplicate case-insensitive
            EmailAddress(3, "rahul.work@company.com", 2, "Work") // Unique
        )

        val emailSet = mutableSetOf<String>()
        val combinedEmails = mutableListOf<EmailAddress>()
        (emailsA + emailsB).forEach { email ->
            val norm = email.address.trim().lowercase()
            if (norm !in emailSet) {
                emailSet.add(norm)
                combinedEmails.add(email)
            }
        }

        assertEquals(2, combinedEmails.size)
    }
}
