package com.cupons.sms.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Coupon computed properties (JVM טהור).
 */
class CouponTest {

    private fun coupon(
        original: Double? = null,
        remaining: Double? = null,
        expires: Long? = null,
        currency: String = "₪"
    ) = Coupon(
        sender = "Test",
        couponCode = "CODE123",
        originalAmount = original,
        remainingBalance = remaining,
        currency = currency,
        receivedAt = 0L,
        expiresAt = expires
    )

    @Test
    fun `displayBalance prefers remaining over original`() {
        assertEquals(30.0, coupon(original = 100.0, remaining = 30.0).displayBalance)
    }

    @Test
    fun `displayBalance falls back to original`() {
        assertEquals(100.0, coupon(original = 100.0, remaining = null).displayBalance)
    }

    @Test
    fun `displayBalance null when neither set`() {
        assertNull(coupon().displayBalance)
    }

    @Test
    fun `hasAmount true when original present`() {
        assertTrue(coupon(original = 50.0).hasAmount)
    }

    @Test
    fun `hasAmount true when only remaining present`() {
        assertTrue(coupon(remaining = 10.0).hasAmount)
    }

    @Test
    fun `hasAmount false when neither present`() {
        assertFalse(coupon().hasAmount)
    }

    @Test
    fun `isExpired true for past expiry`() {
        assertTrue(coupon(expires = System.currentTimeMillis() - 10_000).isExpired)
    }

    @Test
    fun `isExpired false for future expiry`() {
        assertFalse(coupon(expires = System.currentTimeMillis() + 10_000_000).isExpired)
    }

    @Test
    fun `isExpired false when no expiry`() {
        assertFalse(coupon(expires = null).isExpired)
    }

    @Test
    fun `formatAmount prefixes currency and drops decimals`() {
        assertEquals("₪500", coupon().formatAmount(500.0))
        assertEquals("₪500", coupon().formatAmount(499.6))
    }

    @Test
    fun `formatAmount respects currency`() {
        assertEquals("$50", coupon(currency = "$").formatAmount(50.0))
    }

    @Test
    fun `formatAmount null returns empty`() {
        assertEquals("", coupon().formatAmount(null))
    }
}
