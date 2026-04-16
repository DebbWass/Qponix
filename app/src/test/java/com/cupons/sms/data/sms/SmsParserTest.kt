package com.cupons.sms.data.sms

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
}
