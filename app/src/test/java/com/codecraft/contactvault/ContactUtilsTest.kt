package com.codecraft.contactvault

import android.provider.ContactsContract
import com.codecraft.contactvault.util.ContactUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactUtilsTest {

    @Test
    fun testGetPhoneTypeLabel_mobile() {
        val label = ContactUtils.getPhoneTypeLabel(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, null)
        assertEquals("Mobile", label)
    }

    @Test
    fun testGetPhoneTypeLabel_home() {
        val label = ContactUtils.getPhoneTypeLabel(ContactsContract.CommonDataKinds.Phone.TYPE_HOME, null)
        assertEquals("Home", label)
    }

    @Test
    fun testGetPhoneTypeLabel_work() {
        val label = ContactUtils.getPhoneTypeLabel(ContactsContract.CommonDataKinds.Phone.TYPE_WORK, null)
        assertEquals("Work", label)
    }

    @Test
    fun testGetPhoneTypeLabel_custom() {
        val label = ContactUtils.getPhoneTypeLabel(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM, "Secondary Mobile")
        assertEquals("Secondary Mobile", label)
    }

    @Test
    fun testGetEmailTypeLabel_work() {
        val label = ContactUtils.getEmailTypeLabel(ContactsContract.CommonDataKinds.Email.TYPE_WORK, null)
        assertEquals("Work", label)
    }

    @Test
    fun testGetPostalTypeLabel_home() {
        val label = ContactUtils.getPostalTypeLabel(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME, null)
        assertEquals("Home", label)
    }
}
