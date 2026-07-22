package com.cupons.sms.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cupons.sms.data.db.AppDatabase
import com.cupons.sms.domain.model.Coupon
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for CouponRepositoryImpl על גבי in-memory Room (Robolectric).
 * מכסה: dedup ב-insert, טרנזקציית updateBalance, markAsUsed + usage_log ואידמפוטנטיות.
 */
@RunWith(RobolectricTestRunner::class)
class CouponRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CouponRepositoryImpl

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = CouponRepositoryImpl(db, db.couponDao(), db.usageLogDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun smsCoupon(code: String, sender: String, smsId: String?) = Coupon(
        smsId = smsId,
        sender = sender,
        couponCode = code,
        originalAmount = 100.0,
        remainingBalance = 100.0,
        receivedAt = 1000L
    )

    // ─── dedup at insert (fix 1.2) ───

    @Test
    fun `duplicate code and sender from different sms sources is rejected`() = runTest {
        val first = repo.insertCoupon(smsCoupon("ABC123", "Store", "Store_1000"))
        assertTrue(first > 0)
        // אותה הודעה נסרקת מ-Inbox עם sms_id שונה (של ה-Provider)
        val second = repo.insertCoupon(smsCoupon("ABC123", "Store", "42"))
        assertEquals(-1L, second)
        assertEquals(1, repo.getNonArchivedCoupons().first().size)
    }

    @Test
    fun `same code different sender both insert`() = runTest {
        assertTrue(repo.insertCoupon(smsCoupon("ABC123", "StoreA", "a_1")) > 0)
        assertTrue(repo.insertCoupon(smsCoupon("ABC123", "StoreB", "b_1")) > 0)
        assertEquals(2, repo.getNonArchivedCoupons().first().size)
    }

    @Test
    fun `manual coupons with null smsId are exempt from dedup`() = runTest {
        val manual1 = Coupon(smsId = null, sender = "ידני", couponCode = "SAME", receivedAt = 1L)
        val manual2 = Coupon(smsId = null, sender = "ידני", couponCode = "SAME", receivedAt = 2L)
        assertTrue(repo.insertCoupon(manual1) > 0)
        assertTrue(repo.insertCoupon(manual2) > 0)
        assertEquals(2, repo.getNonArchivedCoupons().first().size)
    }

    @Test
    fun `same smsId inserted twice is idempotent via unique index`() = runTest {
        assertTrue(repo.insertCoupon(smsCoupon("X1234", "S", "S_1000")) > 0)
        // אותו sms_id בדיוק — נחסם ע"י האינדקס הייחודי (וגם ע"י dedup התוכן)
        assertEquals(-1L, repo.insertCoupon(smsCoupon("X1234", "S", "S_1000")))
    }

    // ─── updateBalance transaction (fix 1.3) ───

    @Test
    fun `updateBalance updates balance and writes usage log`() = runTest {
        val id = repo.insertCoupon(smsCoupon("BAL1", "S", "S_1"))
        repo.updateBalance(id, newBalance = 40.0, amountUsed = 60.0, note = "קניה")

        val coupon = repo.getCouponById(id)
        assertEquals(40.0, coupon!!.remainingBalance!!, 0.001)

        val log = repo.getUsageLog(id).first()
        assertEquals(1, log.size)
        assertEquals(60.0, log[0].amountUsed, 0.001)
        assertEquals(40.0, log[0].balanceAfter, 0.001)
        assertEquals("קניה", log[0].note)
    }

    // ─── markAsUsed + usage log + idempotency (fix 1.4) ───

    @Test
    fun `markAsUsed writes a usage log for remaining balance`() = runTest {
        val id = repo.insertCoupon(smsCoupon("USE1", "S", "S_2"))
        repo.markAsUsed(id)

        val coupon = repo.getCouponById(id)
        assertTrue(coupon!!.isUsed)
        assertEquals(0.0, coupon.remainingBalance!!, 0.001)

        val log = repo.getUsageLog(id).first()
        assertEquals(1, log.size)
        assertEquals(100.0, log[0].amountUsed, 0.001)
        assertEquals(0.0, log[0].balanceAfter, 0.001)
    }

    @Test
    fun `markAsUsed twice does not add a second log`() = runTest {
        val id = repo.insertCoupon(smsCoupon("USE2", "S", "S_3"))
        repo.markAsUsed(id)
        repo.markAsUsed(id) // יתרה כבר 0 → אין רשומה נוספת
        assertEquals(1, repo.getUsageLog(id).first().size)
    }

    @Test
    fun `markAsUsed on zero-balance coupon writes no log`() = runTest {
        val id = repo.insertCoupon(
            Coupon(smsId = "s_z", sender = "S", couponCode = "ZERO",
                originalAmount = 0.0, remainingBalance = 0.0, receivedAt = 1L)
        )
        repo.markAsUsed(id)
        assertTrue(repo.getUsageLog(id).first().isEmpty())
    }

    // ─── mapping / soft delete round trips ───

    @Test
    fun `soft delete moves coupon to deleted flow`() = runTest {
        val id = repo.insertCoupon(smsCoupon("DEL1", "S", "S_4"))
        repo.deleteCoupon(id)
        assertTrue(repo.getNonArchivedCoupons().first().isEmpty())
        assertEquals(1, repo.getDeletedCoupons().first().size)
        repo.restoreCoupon(id)
        assertEquals(1, repo.getNonArchivedCoupons().first().size)
    }

    @Test
    fun `deleteMultipleCoupons soft-deletes all given ids atomically`() = runTest {
        val id1 = repo.insertCoupon(smsCoupon("M1", "S", "S_10"))
        val id2 = repo.insertCoupon(smsCoupon("M2", "S", "S_11"))
        val id3 = repo.insertCoupon(smsCoupon("M3", "S", "S_12"))

        repo.deleteMultipleCoupons(listOf(id1, id2))

        assertEquals(listOf("M3"), repo.getNonArchivedCoupons().first().map { it.couponCode })
        assertEquals(setOf("M1", "M2"), repo.getDeletedCoupons().first().map { it.couponCode }.toSet())
        // id3 לא נמחק
        assertEquals(false, repo.getCouponById(id3)!!.isDeleted)
    }

    @Test
    fun `getCouponById returns null for missing id`() = runTest {
        assertNull(repo.getCouponById(9999L))
    }

    @Test
    fun `entity domain mapping preserves fields`() = runTest {
        val id = repo.insertCoupon(
            Coupon(smsId = "s_m", sender = "MyStore", couponCode = "MAP1",
                originalAmount = 250.0, currency = "$", merchantName = "MyStore",
                websiteUrl = "https://x.co", receivedAt = 5000L, expiresAt = 9000L)
        )
        val c = repo.getCouponById(id)
        assertNotNull(c)
        assertEquals("MAP1", c!!.couponCode)
        assertEquals("$", c.currency)
        assertEquals(250.0, c.originalAmount!!, 0.001)
        assertEquals(9000L, c.expiresAt)
    }
}
