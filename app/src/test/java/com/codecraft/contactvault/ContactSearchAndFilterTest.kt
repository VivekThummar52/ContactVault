package com.codecraft.contactvault

import com.codecraft.contactvault.domain.model.ContactSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchAndFilterTest {

    private val sampleContacts = listOf(
        ContactSummary(1, "lk1", "Rahul Sharma", null, true, "+91 98765 12345", "rahul@gmail.com", "Acme Corp"),
        ContactSummary(2, "lk2", "Priya Patel", null, false, "+91 98765 67890", "priya@gmail.com", "Tech Solutions"),
        ContactSummary(3, "lk3", "Amit Shah", null, true, "+91 11223 34455", "amit@yahoo.com", null),
        ContactSummary(4, "lk4", "Ananya Verma", null, false, "+91 99887 76655", "ananya@company.com", "Acme Corp")
    )

    @Test
    fun testFavoritesFilter() {
        val favorites = sampleContacts.filter { it.isStarred }
        assertEquals(2, favorites.size)
        assertTrue(favorites.all { it.isStarred })
    }

    @Test
    fun testNameGroupInitial() {
        val grouped = sampleContacts.groupBy { it.displayName.trim().first().uppercaseChar() }
        assertEquals(2, grouped['A']?.size)
        assertEquals(1, grouped['P']?.size)
        assertEquals(1, grouped['R']?.size)
    }

    @Test
    fun testSearchFilterByName() {
        val query = "Rahul"
        val results = sampleContacts.filter {
            it.displayName.contains(query, ignoreCase = true)
        }
        assertEquals(1, results.size)
        assertEquals("Rahul Sharma", results.first().displayName)
    }
}
