package com.cupons.sms.data.prefs

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for AppPreferences custom keyword/blacklist storage (Robolectric + real DataStore).
 * מכסה: אטומיות ההוספה/הסרה, ניקוי פסיקים, ורשומות SMS שנדחו.
 */
@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {

    private lateinit var prefs: AppPreferences

    @Before
    fun setup() {
        prefs = AppPreferences(ApplicationProvider.getApplicationContext())
        // Robolectric משתף את קובץ ה-DataStore בין טסטים באותה מחלקה —
        // מאפסים מפורשות מצב כדי שכל טסט יתחיל נקי.
        runBlocking {
            prefs.setCustomKeywords(emptySet())
            prefs.setCustomBlacklist(emptySet())
        }
    }

    @Test
    fun `add and read custom keyword`() = runTest {
        prefs.addCustomKeyword("מבצע")
        assertTrue(prefs.customKeywords.first().contains("מבצע"))
    }

    @Test
    fun `remove custom keyword`() = runTest {
        prefs.addCustomKeyword("alpha")
        prefs.addCustomKeyword("beta")
        prefs.removeCustomKeyword("alpha")
        val kws = prefs.customKeywords.first()
        assertFalse(kws.contains("alpha"))
        assertTrue(kws.contains("beta"))
    }

    @Test
    fun `sequential adds accumulate without lost update`() = runTest {
        prefs.addCustomKeyword("one")
        prefs.addCustomKeyword("two")
        prefs.addCustomKeyword("three")
        assertEquals(setOf("one", "two", "three"), prefs.customKeywords.first())
    }

    @Test
    fun `comma is stripped from keyword to protect csv encoding`() = runTest {
        prefs.addCustomKeyword("a,b,c")
        assertEquals(setOf("abc"), prefs.customKeywords.first())
    }

    @Test
    fun `blank keyword is ignored`() = runTest {
        prefs.addCustomKeyword("   ")
        assertTrue(prefs.customKeywords.first().isEmpty())
    }

    @Test
    fun `blacklist add and remove`() = runTest {
        prefs.addBlacklistWord("ספאם")
        assertTrue(prefs.customBlacklist.first().contains("ספאם"))
        prefs.removeBlacklistWord("ספאם")
        assertFalse(prefs.customBlacklist.first().contains("ספאם"))
    }

    @Test
    fun `rejected sms ids round trip`() = runTest {
        prefs.addRejectedSmsId("Store_1000")
        prefs.addRejectedSmsId("Store_2000")
        val ids = prefs.getRejectedSmsIds()
        assertTrue(ids.contains("Store_1000"))
        assertTrue(ids.contains("Store_2000"))
    }

    @Test
    fun `notification prefs defaults and update`() = runTest {
        assertTrue(prefs.notificationsEnabled.first())
        assertEquals(3, prefs.notificationDaysBefore.first())
        prefs.setNotificationDaysBefore(7)
        assertEquals(7, prefs.notificationDaysBefore.first())
    }
}
