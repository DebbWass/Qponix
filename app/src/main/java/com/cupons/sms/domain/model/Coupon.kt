package com.cupons.sms.domain.model

/**
 * מודל ה-Domain של קופון/גיפטקארד.
 *
 * מייצג קופון שחולץ מ-SMS או נוסף ידנית.
 * שכבת ה-Domain אינה תלויה ב-Room או ב-Android API.
 */
data class Coupon(
    val id: Long = 0,

    /** ID ייחודי מ-ContentProvider של SMS. null אם נוסף ידנית. */
    val smsId: String? = null,

    /** שם השולח או מספר הטלפון */
    val sender: String,

    /** גוף ה-SMS המקורי המלא */
    val rawSmsBody: String? = null,

    /** קוד הקופון עצמו */
    val couponCode: String,

    /** הסכום המקורי של הגיפטקארד (null אם לא זוהה) */
    val originalAmount: Double? = null,

    /** יתרה נוכחית — מתעדכנת אחרי כל שימוש חלקי */
    val remainingBalance: Double? = null,

    /** מטבע: ₪, $, € וכו' */
    val currency: String = "₪",

    /** שם בית העסק (מחולץ מהטקסט או מה-URL) */
    val merchantName: String? = null,

    /** קישור לאתר המימוש */
    val websiteUrl: String? = null,

    /** מתי התקבל הקופון (Unix timestamp ms) */
    val receivedAt: Long,

    /** תאריך תפוגה (Unix timestamp ms), null אם לא ידוע */
    val expiresAt: Long? = null,

    /** האם הקופון ממומש לחלוטין */
    val isUsed: Boolean = false,

    /** האם הועבר לארכיון */
    val isArchived: Boolean = false,

    /** מחיקה רכה — הקופון נמחק אך נשמר לצפייה */
    val isDeleted: Boolean = false,

    /** הערה חופשית של המשתמשת */
    val notes: String? = null,

    /** ציון בטחון של ה-parser (0.0–1.0) */
    val confidence: Float = 0.5f,

    /** ממתין לאישור ידני */
    val isPending: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** יתרה לתצוגה: מחזיר remainingBalance אם קיים, אחרת originalAmount */
    val displayBalance: Double?
        get() = remainingBalance ?: originalAmount

    /** האם יש סכום כלשהו מוגדר */
    val hasAmount: Boolean
        get() = originalAmount != null || remainingBalance != null

    /** האם הקופון פג תוקף */
    val isExpired: Boolean
        get() = expiresAt != null && expiresAt < System.currentTimeMillis()

    /** פורמט תצוגה של הסכום */
    fun formatAmount(amount: Double?): String =
        if (amount == null) "" else "$currency${"%.0f".format(amount)}"
}
