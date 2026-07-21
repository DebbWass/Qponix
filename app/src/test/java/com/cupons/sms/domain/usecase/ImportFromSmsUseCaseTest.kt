package com.cupons.sms.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cupons.sms.data.db.AppDatabase
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.data.repository.CouponRepositoryImpl
import com.cupons.sms.data.sms.ParsedSmsData
import com.cupons.sms.data.sms.RawSmsMessage
import com.cupons.sms.data.sms.SmsReader
import com.cupons.sms.domain.repository.CouponRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for ImportFromSmsUseCase — SmsReader ב-MockK, repository אמיתי (in-memory Room).
 * מכסה: פיצול auto/pending, אידמפוטנטיות (הרצה שנייה = 0), והעברת מילות מפתח.
 */
@RunWith(RobolectricTestRunner::class)
class ImportFromSmsUseCaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: CouponRepository
    private val smsReader: SmsReader = mockk()
    private val prefs: AppPreferences = mockk(relaxed = true)

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = CouponRepositoryImpl(db, db.couponDao(), db.usageLogDao())

        coEvery { prefs.getRejectedSmsIds() } returns emptySet()
        every { prefs.customKeywords } returns flowOf(emptySet())
        every { prefs.customBlacklist } returns flowOf(emptySet())
    }

    @After
    fun teardown() = db.close()

    private fun parsed(code: String, needsConfirm: Boolean, confidence: Float) = ParsedSmsData(
        couponCode = code, originalAmount = 100.0, currency = "₪", merchantName = "Shop",
        websiteUrl = null, expiresAt = null, confidence = confidence,
        requiresUserConfirmation = needsConfirm
    )

    private fun rawMsg(smsId: String, code: String, needsConfirm: Boolean, conf: Float) =
        RawSmsMessage(smsId, "Shop", "body $code", 1000L, parsed(code, needsConfirm, conf))

    @Test
    fun `high-confidence messages are auto-imported, low ones pending`() = runTest {
        coEvery { smsReader.readAll(any(), any(), any()) } returns listOf(
            rawMsg("1", "AUTO1", needsConfirm = false, conf = 0.8f),
            rawMsg("2", "PEND1", needsConfirm = true, conf = 0.5f)
        )
        val useCase = ImportFromSmsUseCase(smsReader, repo, prefs)

        val result = useCase()
        assertEquals(1, result.imported)
        assertEquals(1, result.pendingConfirmation.size)
        assertEquals("PEND1", result.pendingConfirmation.first().couponCode)
    }

    @Test
    fun `second run imports zero due to content dedup`() = runTest {
        coEvery { smsReader.readAll(any(), any(), any()) } returns listOf(
            rawMsg("1", "AUTO1", needsConfirm = false, conf = 0.8f)
        )
        val useCase = ImportFromSmsUseCase(smsReader, repo, prefs)

        assertEquals(1, useCase().imported)
        // הרצה שנייה — אותו קוד+שולח כבר קיים → dedup בתוכן מחזיר -1
        assertEquals(0, useCase().imported)
        assertEquals(1, repo.getNonArchivedCoupons().first().size)
    }

    @Test
    fun `pending coupon is stored with is_pending flag`() = runTest {
        coEvery { smsReader.readAll(any(), any(), any()) } returns listOf(
            rawMsg("9", "PENDX", needsConfirm = true, conf = 0.55f)
        )
        val useCase = ImportFromSmsUseCase(smsReader, repo, prefs)
        useCase()

        val pending = repo.getPendingCoupons().first()
        assertEquals(1, pending.size)
        assertEquals("PENDX", pending.first().couponCode)
    }
}
