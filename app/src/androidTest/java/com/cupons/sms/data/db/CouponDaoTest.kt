package com.cupons.sms.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.entity.CouponEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for CouponDao using in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class CouponDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CouponDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.couponDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildCoupon(
        id     : Long    = 0,
        smsId  : String? = null,
        code   : String  = "TEST123",
        expired: Boolean = false,
        used   : Boolean = false,
        pending: Boolean = false,
        deleted: Boolean = false
    ) = CouponEntity(
        id         = id,
        smsId      = smsId,
        sender     = "TestSender",
        couponCode = code,
        receivedAt = System.currentTimeMillis(),
        expiresAt  = if (expired) System.currentTimeMillis() - 1000 else null,
        isUsed     = used,
        isArchived = used,
        isDeleted  = deleted,
        isPending  = pending,
        confidence = if (pending) 0.4f else 0.8f
    )

    // ─── Insert ───

    @Test
    fun insert_returnsPositiveId() = runTest {
        val id = dao.insert(buildCoupon())
        assertTrue(id > 0)
    }

    @Test
    fun insert_duplicateSmsId_returnsMinusOne() = runTest {
        dao.insert(buildCoupon(smsId = "sms_001"))
        val duplicate = dao.insert(buildCoupon(smsId = "sms_001", code = "DIFF123"))
        assertEquals(-1L, duplicate)
    }

    // ─── Observe ───

    @Test
    fun observeNonArchived_excludesPending() = runTest {
        dao.insert(buildCoupon(code = "ACTIVE1"))
        dao.insert(buildCoupon(code = "PEND1", pending = true))

        val result = dao.observeNonArchivedCoupons().first()
        assertEquals(1, result.size)
        assertEquals("ACTIVE1", result[0].couponCode)
    }

    @Test
    fun observeNonArchived_excludesDeleted() = runTest {
        dao.insert(buildCoupon(code = "ALIVE1"))
        dao.insert(buildCoupon(code = "DEAD1", deleted = true))

        val result = dao.observeNonArchivedCoupons().first()
        assertEquals(1, result.size)
    }

    @Test
    fun observePending_returnsPendingOnly() = runTest {
        dao.insert(buildCoupon(code = "ACTIVE"))
        dao.insert(buildCoupon(code = "PEND1", pending = true))
        dao.insert(buildCoupon(code = "PEND2", pending = true))

        val result = dao.observePendingCoupons().first()
        assertEquals(2, result.size)
    }

    @Test
    fun observeDeleted_returnsDeletedOnly() = runTest {
        dao.insert(buildCoupon(code = "ALIVE"))
        dao.insert(buildCoupon(code = "DELETED", deleted = true))

        val result = dao.observeDeletedCoupons().first()
        assertEquals(1, result.size)
        assertEquals("DELETED", result[0].couponCode)
    }

    @Test
    fun observeArchived_returnsUsedCoupons() = runTest {
        dao.insert(buildCoupon(code = "USED1", used = true))
        dao.insert(buildCoupon(code = "ACTIVE"))

        val result = dao.observeArchivedCoupons().first()
        assertEquals(1, result.size)
    }

    // ─── Soft delete / Restore ───

    @Test
    fun softDelete_movestoDeletedTab() = runTest {
        val id = dao.insert(buildCoupon(code = "DEL123"))
        dao.softDelete(id)

        val active  = dao.observeNonArchivedCoupons().first()
        val deleted = dao.observeDeletedCoupons().first()

        assertTrue(active.none { it.couponCode == "DEL123" })
        assertEquals(1, deleted.size)
    }

    @Test
    fun restoreCoupon_movesBackToActive() = runTest {
        val id = dao.insert(buildCoupon(code = "RESTORE"))
        dao.softDelete(id)
        dao.restoreCoupon(id)

        val active = dao.observeNonArchivedCoupons().first()
        assertEquals(1, active.size)
    }

    // ─── Approve pending ───

    @Test
    fun approvePending_removeFromPendingTab() = runTest {
        val id = dao.insert(buildCoupon(code = "APPROV", pending = true))
        dao.approvePending(id)

        val pending = dao.observePendingCoupons().first()
        val active  = dao.observeNonArchivedCoupons().first()

        assertTrue(pending.isEmpty())
        assertEquals(1, active.size)
    }

    // ─── Mark as used ───

    @Test
    fun markAsUsed_movesToArchive() = runTest {
        val id = dao.insert(buildCoupon(code = "USEDIT"))
        dao.markAsUsed(id)

        val active   = dao.observeNonArchivedCoupons().first()
        val archived = dao.observeArchivedCoupons().first()

        assertTrue(active.isEmpty())
        assertEquals(1, archived.size)
    }

    // ─── Expiry query ───

    @Test
    fun getCouponsExpiringBefore_returnsOnlyInRange() = runTest {
        val now      = System.currentTimeMillis()
        val tomorrow = now + 24 * 60 * 60 * 1000L
        val nextWeek = now + 7 * 24 * 60 * 60 * 1000L

        dao.insert(buildCoupon(code = "EXPIRE_TOMORROW").copy(expiresAt = tomorrow - 1000))
        dao.insert(buildCoupon(code = "EXPIRE_NEXTWEEK").copy(expiresAt = nextWeek))
        dao.insert(buildCoupon(code = "NO_EXPIRY"))

        val result = dao.getCouponsExpiringBefore(now, tomorrow + 1000)
        assertEquals(1, result.size)
        assertEquals("EXPIRE_TOMORROW", result[0].couponCode)
    }

    // ─── Backup ───

    @Test
    fun getAllActive_excludesDeleted() = runTest {
        dao.insert(buildCoupon(code = "ACTIVE"))
        dao.insert(buildCoupon(code = "DELETED", deleted = true))

        val all = dao.getAllActive()
        assertEquals(1, all.size)
    }
}
