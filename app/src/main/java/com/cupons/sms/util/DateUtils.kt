package com.cupons.sms.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * עזרי תאריך משותפים — מקור אמת יחיד לפרסור ופרמוט תאריכי תפוגה.
 *
 * לפני האיחוד היו שתי משמעויות שונות ל"תוקף עד תאריך X":
 * AddCouponViewModel פרסר לחצות (00:00:00) בעוד DateInputField ל-23:59:59,
 * כך שקופון שהוזן ידנית נחשב פג ~24 שעות מוקדם יותר. כאן — סוף היום בכל מקום.
 */
object DateUtils {

    private const val DATE_PATTERN = "dd/MM/yyyy"

    /**
     * פרסור "DD/MM/YYYY" ל-Unix timestamp (ms) בסוף היום (23:59:59.999).
     * מחזיר null אם הפורמט לא תקין (parsing לא-לניענטי).
     */
    fun parseDateToEndOfDayMillis(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        if (trimmed.length < 10) return null
        return try {
            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
            sdf.isLenient = false
            val parsed = sdf.parse(trimmed) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    /** המרת Unix timestamp לפורמט "DD/MM/YYYY" (ריק אם null). */
    fun millisToDateString(millis: Long?): String {
        if (millis == null) return ""
        return try {
            SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date(millis))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * פרמוט אוטומטי של קלט תאריך: מוסיף "/" אחרי היום ואחרי החודש.
     * בטוח לקרוא שוב על מחרוזת מפורמטת.
     */
    fun formatDateInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if (i == 1 || i == 3) append("/")
            }
        }
    }
}
