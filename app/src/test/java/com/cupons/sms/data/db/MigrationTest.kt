package com.cupons.sms.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration tests — בונה סכימת v1 ידנית, מריץ MIGRATION_1_2 ואז MIGRATION_2_3,
 * ומוודא שהעמודות החדשות קיימות ושנתונים שורדים.
 *
 * (אין קובצי schema היסטוריים, לכן ה-DDL של v1 נבנה כאן ידנית.)
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    // ה-DDL של טבלת coupons בגרסה 1 — ללא is_deleted / confidence / is_pending
    private val v1CouponsDdl = """
        CREATE TABLE IF NOT EXISTS coupons (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            sms_id TEXT,
            sender TEXT NOT NULL,
            raw_sms_body TEXT,
            coupon_code TEXT NOT NULL,
            original_amount REAL,
            remaining_balance REAL,
            currency TEXT NOT NULL,
            merchant_name TEXT,
            website_url TEXT,
            received_at INTEGER NOT NULL,
            expires_at INTEGER,
            is_used INTEGER NOT NULL,
            is_archived INTEGER NOT NULL,
            notes TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    """.trimIndent()

    @Before
    fun setup() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(
            ApplicationProvider.getApplicationContext()
        ).name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(v1CouponsDdl)
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_coupons_sms_id ON coupons(sms_id)"
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) {}
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun teardown() {
        helper.close()
    }

    private fun columns(): Set<String> {
        val cols = mutableSetOf<String>()
        db.query("PRAGMA table_info(coupons)").use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) cols.add(c.getString(nameIdx))
        }
        return cols
    }

    @Test
    fun `v1 schema lacks the columns added by later migrations`() {
        val cols = columns()
        assertTrue(cols.contains("coupon_code"))
        assertTrue(!cols.contains("is_deleted"))
        assertTrue(!cols.contains("confidence"))
        assertTrue(!cols.contains("is_pending"))
    }

    @Test
    fun `migration 1 to 2 adds is_deleted`() {
        AppDatabase.MIGRATION_1_2.migrate(db)
        assertTrue(columns().contains("is_deleted"))
    }

    @Test
    fun `migration 2 to 3 adds confidence and is_pending`() {
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        val cols = columns()
        assertTrue(cols.contains("confidence"))
        assertTrue(cols.contains("is_pending"))
    }

    @Test
    fun `data survives both migrations with correct defaults`() {
        db.execSQL(
            """INSERT INTO coupons
               (sms_id, sender, coupon_code, currency, received_at, is_used, is_archived, created_at, updated_at)
               VALUES ('s1', 'Shop', 'SURVIVE', '₪', 100, 0, 0, 0, 0)"""
        )
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        db.query("SELECT coupon_code, is_deleted, confidence, is_pending FROM coupons WHERE sms_id = 's1'")
            .use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("SURVIVE", c.getString(0))
                assertEquals(0, c.getInt(1))                 // is_deleted default 0
                assertEquals(0.5, c.getDouble(2), 0.001)     // confidence default 0.5
                assertEquals(0, c.getInt(3))                 // is_pending default 0
            }
    }
}
