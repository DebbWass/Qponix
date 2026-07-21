package com.cupons.sms.data.sms

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for SmsParser.
 * Tests coupon detection, blacklist rejection, confidence scoring, and edge cases.
 */
class SmsParserTest {

    private lateinit var parser: SmsParser

    @Before
    fun setup() {
        parser = SmsParser()
    }

    /** חותמת זמן ליום/חודש/שנה מקומיים בצהריים (יציב מול חלון החסד) */
    private fun timestampOf(year: Int, month1to12: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month1to12 - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun yearOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)

    // ─── Blacklist tests ───

    @Test
    fun `blacklist OTP - should return null`() {
        val sms = "קוד האימות שלך הוא: 123456. אל תשתף אותו עם אחרים."
        assertNull(parser.parse(sms, "Bank", System.currentTimeMillis()))
    }

    @Test
    fun `blacklist unsubscribe - should return null`() {
        val sms = "הצעה מיוחדת! להסרה מהרשימה שלח STOP"
        assertNull(parser.parse(sms, "Promo", System.currentTimeMillis()))
    }

    @Test
    fun `blacklist order confirmation - should return null`() {
        val sms = "אישור הזמנה #12345. הסכום: 250 ש\"ח. תודה שקנית!"
        assertNull(parser.parse(sms, "Shop", System.currentTimeMillis()))
    }

    @Test
    fun `blacklist OTP english - should return null`() {
        val sms = "Your verification code is 4521. Valid for 5 minutes."
        assertNull(parser.parse(sms, "Service", System.currentTimeMillis()))
    }

    // ─── Coupon detection tests ───

    @Test
    fun `hebrew coupon code format - should detect`() {
        val sms = "קוד קופון: SAVE50. ממש את הקופון עד 31/12/2025. שווי ₪100"
        val result = parser.parse(sms, "Store", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("SAVE50", result!!.couponCode)
    }

    @Test
    fun `gift card with amount - should detect`() {
        val sms = "גיפטקארד שלך: GIFT2024. שווי ₪200. בתוקף עד 01/06/2025"
        val result = parser.parse(sms, "GiftStore", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("GIFT2024", result!!.couponCode)
        assertEquals(200.0, result.originalAmount)
    }

    @Test
    fun `english promo code - should detect`() {
        val sms = "Your promo code: PROMO25. Redeem now at checkout. Valid until 2025-12-31"
        val result = parser.parse(sms, "Brand", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("PROMO25", result!!.couponCode)
    }

    @Test
    fun `use code pattern - should detect`() {
        val sms = "השתמש בקוד WINTER20 וקבל 20% הנחה. לממש עד 31/01/2025"
        val result = parser.parse(sms, "Fashion", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("WINTER20", result!!.couponCode)
    }

    @Test
    fun `matana4u URL - should detect with high confidence`() {
        val sms = "כרטיס מתנה שלך: https://matana4u.co.il/redeem/ABC123XY. שווי ₪150"
        val result = parser.parse(sms, "matana4u", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("ABC123XY", result!!.couponCode)
        assertTrue(result.confidence >= SmsParser.CONFIDENCE_AUTO_IMPORT)
    }

    @Test
    fun `url query param code - should detect`() {
        val sms = "ממש קופון ב https://shop.co.il/checkout?code=DEAL30 חסוך ₪50"
        val result = parser.parse(sms, "Shop", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("DEAL30", result!!.couponCode)
    }

    // ─── Amount extraction tests ───

    @Test
    fun `shekel symbol before amount`() {
        val sms = "גיפטקארד: GIFT100. ₪250 לשימוש בחנות"
        val result = parser.parse(sms, "Store", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(250.0, result!!.originalAmount)
        assertEquals("₪", result.currency)
    }

    @Test
    fun `shekel word after amount`() {
        val sms = "קופון: SAVE100. שווי 300 שקל"
        val result = parser.parse(sms, "Store", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(300.0, result!!.originalAmount)
    }

    @Test
    fun `dollar amount`() {
        val sms = "Gift card code: USDGIFT. Worth \$50"
        val result = parser.parse(sms, "Global", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals(50.0, result!!.originalAmount)
        assertEquals("$", result.currency)
    }

    // ─── Confidence tests ───

    @Test
    fun `high confidence - amount + expiry + url`() {
        val sms = "קופון: BEST2024. ₪200 בתוקף עד 31/12/2025. מימוש: https://shop.co.il"
        val result = parser.parse(sms, "Shop", System.currentTimeMillis())
        assertNotNull(result)
        assertTrue(result!!.confidence >= SmsParser.CONFIDENCE_AUTO_IMPORT)
        assertFalse(result.requiresUserConfirmation)
    }

    @Test
    fun `low confidence - keyword only no amount no expiry`() {
        val sms = "קוד קופון שלך: BASIC5"
        val result = parser.parse(sms, "Unknown", System.currentTimeMillis())
        assertNotNull(result)
        // base = 0.5 (keyword + code), no amount, no expiry, no URL → still >= 0.5
        assertTrue(result!!.confidence >= SmsParser.CONFIDENCE_SUGGEST)
    }

    // ─── Edge cases ───

    @Test
    fun `empty body - should return null`() {
        assertNull(parser.parse("", "Sender", System.currentTimeMillis()))
    }

    @Test
    fun `only numbers - should return null`() {
        assertNull(parser.parse("12345 67890", "Sender", System.currentTimeMillis()))
    }

    @Test
    fun `code too short - should return null`() {
        val sms = "קופון: AB1 תקף עד סוף החודש"
        assertNull(parser.parse(sms, "Store", System.currentTimeMillis()))
    }

    @Test
    fun `voucher keyword - should detect`() {
        val sms = "Your voucher code: VCH-2025-SAVE. Redeem at checkout."
        val result = parser.parse(sms, "Brand", System.currentTimeMillis())
        assertNotNull(result)
        assertNotNull(result!!.couponCode)
    }

    @Test
    fun `coupon code is uppercase`() {
        val sms = "קוד קופון: save50"
        val result = parser.parse(sms, "Store", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("SAVE50", result!!.couponCode)
    }

    @Test
    fun `hashtag code format - should detect`() {
        val sms = "גיפטקארד! השתמש בקוד #HASH2024 לקנייה הבאה"
        val result = parser.parse(sms, "Shop", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("HASH2024", result!!.couponCode)
    }

    @Test
    fun `multi-keyword sms - higher confidence`() {
        val sms = "כרטיס מתנה שלך: CARD500. ₪500 שקל בתוקף עד 01/03/2025. לממש ב https://redeem.co.il?code=CARD500"
        val result = parser.parse(sms, "GiftCo", System.currentTimeMillis())
        assertNotNull(result)
        assertTrue(result!!.confidence > 0.7f)
    }

    @Test
    fun `invoice - should be blacklisted`() {
        val sms = "חשבונית עבור הזמנתך מספר 8921. סכום: ₪450"
        assertNull(parser.parse(sms, "Shop", System.currentTimeMillis()))
    }

    // ─── parseDate: year rollover (fix 1.5) ───

    @Test
    fun `two-part date received in december rolls to next year`() {
        // התקבל 20/12/2025, תפוגה "עד 5.1" → צריך להיות 5 בינואר 2026
        val received = timestampOf(2025, 12, 20)
        val sms = "קופון: NEWYEAR. בתוקף עד 5.1"
        val result = parser.parse(sms, "Store", received)
        assertNotNull(result)
        assertNotNull(result!!.expiresAt)
        assertEquals(2026, yearOf(result.expiresAt!!))
        assertTrue("expiry must be in the future", result.expiresAt!! > received)
    }

    @Test
    fun `two-part date in same year does not roll over`() {
        // התקבל 01/03/2025, תפוגה "עד 11.4" → 11 באפריל 2025 (אותה שנה)
        val received = timestampOf(2025, 3, 1)
        val sms = "קופון: SPRING. בתוקף עד 11.4"
        val result = parser.parse(sms, "Store", received)
        assertNotNull(result)
        assertEquals(2025, yearOf(result!!.expiresAt!!))
    }

    @Test
    fun `two-part date within grace window keeps same year`() {
        // התקבל 27/02, תפוגה "28.2" — אתמול/היום בטווח החסד → לא לגלגל
        val received = timestampOf(2025, 2, 27)
        val sms = "קופון: GRACEDAY. בתוקף עד 28.2"
        val result = parser.parse(sms, "Store", received)
        assertNotNull(result)
        assertEquals(2025, yearOf(result!!.expiresAt!!))
    }

    @Test
    fun `three-part date with explicit year is authoritative`() {
        val received = timestampOf(2025, 12, 1)
        val sms = "קופון: EXPLICIT. בתוקף עד 10.3.26"
        val result = parser.parse(sms, "Store", received)
        assertNotNull(result)
        assertEquals(2026, yearOf(result!!.expiresAt!!))
    }

    @Test
    fun `invalid date is rejected but coupon still parses`() {
        // "32.13" אינו תאריך תקין → expiresAt null, אך הקופון עצמו תקף
        val sms = "קופון: BADDATE. בתוקף עד 32.13"
        val result = parser.parse(sms, "Store", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("BADDATE", result!!.couponCode)
        assertNull(result.expiresAt)
    }

    // ─── Confidence clamp (fix 1.6) ───

    @Test
    fun `confidence never exceeds one even with matana4u boost`() {
        // matana4u + amount + expiry + url → 0.5+0.25+0.15+0.10+0.20 = 1.20 → clamp 1.0
        val sms = "כרטיס מתנה: MEGA1234. שווי ₪500 בתוקף עד 31/12/2025. " +
            "מימוש: https://matana4u.co.il/redeem/MEGA1234"
        val result = parser.parse(sms, "matana4u", System.currentTimeMillis())
        assertNotNull(result)
        assertTrue("confidence must be clamped to <= 1.0", result!!.confidence <= 1.0f)
        assertEquals(1.0f, result.confidence)
    }

    // ─── Custom keywords / blacklist (fixes 2.1 / H1 / H2) ───

    @Test
    fun `custom keyword enables otherwise-skipped sms`() {
        // "מבצע" אינו מילת מפתח חזקה כברירת מחדל, וקוד ה-hashtag נחלץ ללא מילת קוד.
        // בלי custom keyword אין מילת מפתח חזקה → נדחה.
        val sms = "מבצע ענק! #DEAL2024 ממתין לך"
        assertNull(parser.parse(sms, "Store", System.currentTimeMillis()))

        val result = parser.parse(
            sms, "Store", System.currentTimeMillis(),
            customKeywords = listOf("מבצע")
        )
        assertNotNull(result)
        assertEquals("DEAL2024", result!!.couponCode)
    }

    @Test
    fun `custom blacklist word blocks an otherwise-valid coupon`() {
        val sms = "קוד קופון: SAVE50 — פנימי בלבד, אל תשלח"
        // ללא רשימה שחורה מותאמת — נקלט
        assertNotNull(parser.parse(sms, "Store", System.currentTimeMillis()))
        // עם מילה ברשימה השחורה — נדחה
        assertNull(
            parser.parse(
                sms, "Store", System.currentTimeMillis(),
                customBlacklist = listOf("פנימי בלבד")
            )
        )
    }

    @Test
    fun `extractExpiryOnly reflects year rollover`() {
        val received = timestampOf(2025, 12, 15)
        val expiry = parser.extractExpiryOnly("בתוקף עד 3.1", received)
        assertNotNull(expiry)
        assertEquals(2026, yearOf(expiry!!))
    }

    // ─── gogift gift-card links ───

    @Test
    fun `gogift url code is extracted from path`() {
        val sms = "מתנה בשבילך! למימוש: http://giftcard.gogift.co.il/redeem/GIFT7788"
        val result = parser.parse(sms, "GoGift", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("GIFT7788", result!!.couponCode)
    }

    @Test
    fun `gogift code directly after domain`() {
        val sms = "כרטיס מתנה: https://giftcard.gogift.co.il/ABC12345"
        val result = parser.parse(sms, "GoGift", System.currentTimeMillis())
        assertNotNull(result)
        assertEquals("ABC12345", result!!.couponCode)
    }

    @Test
    fun `gogift link is a certain coupon - high confidence`() {
        val sms = "מתנה: http://giftcard.gogift.co.il/redeem/GIFT7788"
        val result = parser.parse(sms, "GoGift", System.currentTimeMillis())
        assertNotNull(result)
        // base 0.5 + url 0.10 + certain-domain boost 0.20 = 0.80 → auto-import
        assertTrue(result!!.confidence >= SmsParser.CONFIDENCE_AUTO_IMPORT)
        assertFalse(result.requiresUserConfirmation)
    }

    @Test
    fun `gogift path word without digits is not mistaken for a code`() {
        // "redeem" בלבד (ללא קוד עם ספרה אחריו) לא ייחשב כקוד
        val sms = "מתנה בדרך: http://giftcard.gogift.co.il/redeem"
        val result = parser.parse(sms, "GoGift", System.currentTimeMillis())
        assertNull(result)
    }
}
