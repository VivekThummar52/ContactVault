package com.codecraft.contactvault

import com.codecraft.contactvault.domain.model.Contact
import com.codecraft.contactvault.domain.model.EmailAddress
import com.codecraft.contactvault.domain.model.Organization
import com.codecraft.contactvault.domain.model.PhoneNumber
import com.codecraft.contactvault.util.VcfManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VcfAndBackupTest {

    @Test
    fun testVcfGenerationAndParsing() {
        val sampleContacts = listOf(
            Contact(
                id = 1,
                lookupKey = "key1",
                displayName = "Rahul Sharma",
                phoneNumbers = listOf(PhoneNumber(101, "+91 98765 12345", 2, "Mobile")),
                emailAddresses = listOf(EmailAddress(201, "rahul@gmail.com", 1, "Personal")),
                organization = Organization("Acme Corp", "Tech Lead"),
                notes = listOf("Met at Android summit")
            )
        )

        val vcfText = VcfManager.generateVcf(sampleContacts)
        assertTrue(vcfText.contains("BEGIN:VCARD"))
        assertTrue(vcfText.contains("FN:Rahul Sharma"))
        assertTrue(vcfText.contains("TEL;TYPE=CELL:+91 98765 12345"))
        assertTrue(vcfText.contains("EMAIL;TYPE=INTERNET:rahul@gmail.com"))
        assertTrue(vcfText.contains("ORG:Acme Corp"))
        assertTrue(vcfText.contains("END:VCARD"))

        val parsed = VcfManager.parseVcf(vcfText)
        assertEquals(1, parsed.size)
        assertEquals("Rahul Sharma", parsed.first().displayName)
        assertEquals("+91 98765 12345", parsed.first().phoneNumbers.first().number)
        assertEquals("rahul@gmail.com", parsed.first().emailAddresses.first().address)
    }

    @Test
    fun testBackupJsonSchemaValidation() {
        val jsonText = """
            {
              "header": {
                "app": "ContactVault",
                "version": 1,
                "createdAt": 1788448800000,
                "totalContacts": 5
              },
              "contacts": []
            }
        """.trimIndent()

        val root = JSONObject(jsonText)
        val header = root.optJSONObject("header")
        assertEquals("ContactVault", header?.optString("app"))
        assertEquals(1, header?.optInt("version"))
        assertEquals(5, header?.optInt("totalContacts"))
    }
}
