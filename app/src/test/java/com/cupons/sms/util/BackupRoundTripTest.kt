package com.cupons.sms.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cupons.sms.data.db.AppDatabase
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.db.entity.UsageLogEntity
import com.cupons.sms.data.prefs.AppPreferences
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
 * Round-trip tests for backup v2 (export JSON → import into fresh DB) plus v1 compat
 * and malformed-input handling. Robolectric + in-memory Room.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {

    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase
    private lateinit var exportManager: ExportManager
    private lateinit var importManager: ImportManager

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        sourceDb = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        targetDb = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val prefs = AppPreferences(ctx)
        exportManager = ExportManager(ctx, sourceDb.couponDao(), sourceDb.usageLogDao(), prefs)
        importManager = ImportManager(ctx, targetDb.couponDao(), targetDb.usageLogDao())
    }

    @After
    fun teardown() {
        sourceDb.close()
        targetDb.close()
    }

    @Test
    fun `v2 round trip preserves coupons and remaps usage logs`() = runTest {
        val couponId = sourceDb.couponDao().insert(
            CouponEntity(smsId = "sms_1", sender = "Shop", couponCode = "CODE1",
                originalAmount = 100.0, remainingBalance = 40.0, merchantName = "Shop", receivedAt = 1L)
        )
        sourceDb.usageLogDao().insert(
            UsageLogEntity(couponId = couponId, amountUsed = 60.0, balanceAfter = 40.0, note = "n")
        )

        val json = exportManager.buildBackupJson(
            sourceDb.couponDao().getAllActive(),
            sourceDb.usageLogDao().getAll()
        )
        val result = importManager.importFromJson(json.toString())

        assertEquals(1, result.imported)
        val imported = targetDb.couponDao().getAllActive()
        assertEquals(1, imported.size)
        assertEquals("CODE1", imported[0].couponCode)

        // יומן השימוש שויך מחדש ל-id הקופון החדש
        val logs = targetDb.usageLogDao().getAll()
        assertEquals(1, logs.size)
        assertEquals(imported[0].id, logs[0].couponId)
        assertEquals(60.0, logs[0].amountUsed, 0.001)
    }

    @Test
    fun `v2 export declares version 2 and includes usageLogs array`() = runTest {
        val json = exportManager.buildBackupJson(emptyList(), emptyList())
        assertEquals(2, json.getInt("version"))
        assertNotNull(json.getJSONArray("usageLogs"))
    }

    @Test
    fun `v1 format without usageLogs still imports coupons`() = runTest {
        val v1 = """
            {
              "version": 1,
              "count": 1,
              "coupons": [
                {"id": 5, "smsId": "old_1", "sender": "S", "couponCode": "OLD1",
                 "originalAmount": 50.0, "currency": "₪", "receivedAt": 10}
              ]
            }
        """.trimIndent()

        val result = importManager.importFromJson(v1)
        assertEquals(1, result.imported)
        assertEquals("OLD1", targetDb.couponDao().getAllActive()[0].couponCode)
        assertTrue(targetDb.usageLogDao().getAll().isEmpty())
    }

    @Test
    fun `duplicate smsId is skipped on import`() = runTest {
        targetDb.couponDao().insert(
            CouponEntity(smsId = "dup_1", sender = "S", couponCode = "EXISTS", receivedAt = 1L)
        )
        val json = """
            {"version": 2, "coupons": [
              {"id": 1, "smsId": "dup_1", "sender": "S", "couponCode": "EXISTS", "receivedAt": 1}
            ], "usageLogs": []}
        """.trimIndent()

        val result = importManager.importFromJson(json)
        assertEquals(0, result.imported)
        assertEquals(1, result.skipped)
        assertEquals(1, targetDb.couponDao().getAllActive().size)
    }

    @Test
    fun `unsupported version returns error and inserts nothing`() = runTest {
        val result = importManager.importFromJson("""{"version": 99, "coupons": []}""")
        assertNotNull(result.error)
        assertTrue(targetDb.couponDao().getAllActive().isEmpty())
    }

    @Test
    fun `malformed json returns error via importFromUri path`() = runTest {
        // importFromJson מזרוק JSONException; importFromUri עוטף ל-error.
        // כאן בודקים ישירות שהזריקה מתרחשת על JSON לא תקין.
        var threw = false
        try {
            importManager.importFromJson("{ this is not json")
        } catch (e: Exception) {
            threw = true
        }
        assertTrue(threw)
        assertTrue(targetDb.couponDao().getAllActive().isEmpty())
    }

    @Test
    fun `usage log for skipped coupon is not imported`() = runTest {
        // קופון קיים → מדולג; יומן השימוש שלו לא אמור להיכנס (אין מיפוי id)
        targetDb.couponDao().insert(
            CouponEntity(smsId = "keep_1", sender = "S", couponCode = "KEEP", receivedAt = 1L)
        )
        val json = """
            {"version": 2, "coupons": [
              {"id": 7, "smsId": "keep_1", "sender": "S", "couponCode": "KEEP", "receivedAt": 1}
            ], "usageLogs": [
              {"id": 1, "couponId": 7, "amountUsed": 10.0, "balanceAfter": 0.0}
            ]}
        """.trimIndent()

        importManager.importFromJson(json)
        assertTrue(targetDb.usageLogDao().getAll().isEmpty())
    }
}
