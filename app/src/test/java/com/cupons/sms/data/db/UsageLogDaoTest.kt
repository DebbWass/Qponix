package com.cupons.sms.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.db.entity.UsageLogEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for UsageLogDao + FK CASCADE (Robolectric + in-memory Room).
 */
@RunWith(RobolectricTestRunner::class)
class UsageLogDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() = db.close()

    private suspend fun insertCoupon(code: String, merchant: String?): Long =
        db.couponDao().insert(
            CouponEntity(
                smsId = "sms_$code", sender = "S", couponCode = code,
                merchantName = merchant, receivedAt = 1L
            )
        )

    @Test
    fun `getTotalAmountUsed sums all logs`() = runTest {
        val id = insertCoupon("C1", "Shop")
        db.usageLogDao().insert(UsageLogEntity(couponId = id, amountUsed = 30.0, balanceAfter = 70.0))
        db.usageLogDao().insert(UsageLogEntity(couponId = id, amountUsed = 20.0, balanceAfter = 50.0))
        assertEquals(50.0, db.usageLogDao().getTotalAmountUsed(), 0.001)
    }

    @Test
    fun `getTotalAmountUsed is zero when empty`() = runTest {
        assertEquals(0.0, db.usageLogDao().getTotalAmountUsed(), 0.001)
    }

    @Test
    fun `getTopMerchants counts usages per merchant`() = runTest {
        val shopA = insertCoupon("A", "ShopA")
        val shopB = insertCoupon("B", "ShopB")
        db.usageLogDao().insert(UsageLogEntity(couponId = shopA, amountUsed = 10.0, balanceAfter = 0.0))
        db.usageLogDao().insert(UsageLogEntity(couponId = shopA, amountUsed = 10.0, balanceAfter = 0.0))
        db.usageLogDao().insert(UsageLogEntity(couponId = shopB, amountUsed = 5.0, balanceAfter = 0.0))

        val top = db.usageLogDao().getTopMerchants()
        assertEquals("ShopA", top.first().merchant)
        assertEquals(2, top.first().usageCount)
    }

    @Test
    fun `getAll returns every log ordered`() = runTest {
        val id = insertCoupon("C2", "Shop")
        db.usageLogDao().insert(UsageLogEntity(couponId = id, amountUsed = 1.0, balanceAfter = 9.0, usedAt = 100L))
        db.usageLogDao().insert(UsageLogEntity(couponId = id, amountUsed = 2.0, balanceAfter = 7.0, usedAt = 200L))
        val all = db.usageLogDao().getAll()
        assertEquals(2, all.size)
        assertTrue(all[0].usedAt <= all[1].usedAt)
    }

    @Test
    fun `deleting coupon cascades to usage logs`() = runTest {
        val id = insertCoupon("C3", "Shop")
        db.usageLogDao().insert(UsageLogEntity(couponId = id, amountUsed = 10.0, balanceAfter = 0.0))
        assertEquals(1, db.usageLogDao().getAll().size)

        db.couponDao().deleteById(id)
        assertTrue(db.usageLogDao().getAll().isEmpty())
    }
}
