package com.cupons.sms.data.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.util.NotificationHelper
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for ExpiryNotificationWorker (Robolectric + TestListenableWorkerBuilder).
 */
@RunWith(RobolectricTestRunner::class)
class ExpiryNotificationWorkerTest {

    private val dao: CouponDao = mockk()
    private val prefs: AppPreferences = mockk()
    private val notifHelper: NotificationHelper = mockk(relaxed = true)
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        every { prefs.notificationDaysBefore } returns flowOf(3)
    }

    private fun buildWorker(): ExpiryNotificationWorker =
        TestListenableWorkerBuilder<ExpiryNotificationWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker =
                    ExpiryNotificationWorker(appContext, workerParameters, dao, prefs, notifHelper)
            })
            .build()

    @Test
    fun `disabled notifications short-circuits to success without notifying`() = runTest {
        every { prefs.notificationsEnabled } returns flowOf(false)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifHelper.notifyExpiringSoon(any(), any(), any(), any()) }
    }

    @Test
    fun `empty expiring set returns success without notifying`() = runTest {
        every { prefs.notificationsEnabled } returns flowOf(true)
        coEvery { dao.getCouponsExpiringBefore(any(), any()) } returns emptyList()

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifHelper.notifyExpiringSoon(any(), any(), any(), any()) }
    }

    @Test
    fun `expiring coupon triggers a stable-id notification`() = runTest {
        every { prefs.notificationsEnabled } returns flowOf(true)
        val coupon = CouponEntity(
            id = 42, smsId = "s", sender = "S", couponCode = "EXP1",
            merchantName = "Shop", receivedAt = 0L,
            expiresAt = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L
        )
        coEvery { dao.getCouponsExpiringBefore(any(), any()) } returns listOf(coupon)

        val expectedId = (100_000L + 42L % 100_000).toInt()
        every { notifHelper.notifyExpiringSoon(any(), any(), any(), expectedId) } just Runs

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) {
            notifHelper.notifyExpiringSoon("EXP1", "Shop", any(), expectedId)
        }
    }
}
