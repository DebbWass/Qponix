package com.cupons.sms.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for DateUtils — פרסור/פרמוט תאריכי תפוגה (JVM טהור).
 */
class DateUtilsTest {

    private fun fieldsOf(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    @Test
    fun `parses valid date to end of day`() {
        val millis = DateUtils.parseDateToEndOfDayMillis("15/06/2025")
        requireNotNull(millis)
        val cal = fieldsOf(millis)
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `partial input returns null`() {
        assertNull(DateUtils.parseDateToEndOfDayMillis("15/06"))
        assertNull(DateUtils.parseDateToEndOfDayMillis("1/1/25"))
        assertNull(DateUtils.parseDateToEndOfDayMillis(""))
    }

    @Test
    fun `invalid date returns null`() {
        assertNull(DateUtils.parseDateToEndOfDayMillis("32/13/2025"))
        assertNull(DateUtils.parseDateToEndOfDayMillis("abcdefghij"))
    }

    @Test
    fun `round trip millis to string and back`() {
        val millis = DateUtils.parseDateToEndOfDayMillis("01/03/2026")
        requireNotNull(millis)
        assertEquals("01/03/2026", DateUtils.millisToDateString(millis))
    }

    @Test
    fun `millisToDateString null returns empty`() {
        assertEquals("", DateUtils.millisToDateString(null))
    }

    @Test
    fun `formatDateInput inserts slashes`() {
        assertEquals("1", DateUtils.formatDateInput("1"))
        assertEquals("15/", DateUtils.formatDateInput("15"))
        assertEquals("15/06/", DateUtils.formatDateInput("1506"))
        assertEquals("15/06/2025", DateUtils.formatDateInput("15062025"))
    }

    @Test
    fun `formatDateInput strips non-digits and caps at eight`() {
        assertEquals("15/06/2025", DateUtils.formatDateInput("15/06/2025"))
        assertEquals("15/06/2025", DateUtils.formatDateInput("150620259999"))
    }
}
