package com.codecraft.contactvault

import com.codecraft.contactvault.domain.model.ContactAppNote
import com.codecraft.contactvault.domain.model.ContactGroup
import com.codecraft.contactvault.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTagsAndNotesTest {

    @Test
    fun testTagDomainModel() {
        val tag = Tag(id = 1, name = "Work", colorHex = "#1E88E5")
        assertEquals(1L, tag.id)
        assertEquals("Work", tag.name)
        assertEquals("#1E88E5", tag.colorHex)
    }

    @Test
    fun testContactAppNoteDomainModel() {
        val note = ContactAppNote(id = 10, contactId = 100, noteText = "Met at Kotlin conference")
        assertEquals(10L, note.id)
        assertEquals(100L, note.contactId)
        assertEquals("Met at Kotlin conference", note.noteText)
    }

    @Test
    fun testContactGroupDomainModel() {
        val group = ContactGroup(id = 5, title = "Colleagues", accountName = "user@gmail.com", memberCount = 12)
        assertEquals(5L, group.id)
        assertEquals("Colleagues", group.title)
        assertEquals(12, group.memberCount)
    }

    @Test
    fun testTagFilterMatching() {
        val tags = listOf(
            Tag(1, "Work", "#1E88E5"),
            Tag(2, "Family", "#43A047"),
            Tag(3, "Important", "#E53935")
        )
        val selectedTagId = 2L
        val matchedTag = tags.find { it.id == selectedTagId }
        assertEquals("Family", matchedTag?.name)
    }
}
