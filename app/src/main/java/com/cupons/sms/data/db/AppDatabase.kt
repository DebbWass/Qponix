package com.cupons.sms.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.data.db.entity.CouponEntity
import com.cupons.sms.data.db.entity.UsageLogEntity

/**
 * Room Database — גרסה 3.
 *
 * שינויים מגרסה 1→2: הוספת עמודה `is_deleted`
 * שינויים מגרסה 2→3: הוספת עמודות `confidence` ו-`is_pending`
 */
@Database(
    entities = [CouponEntity::class, UsageLogEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun couponDao(): CouponDao
    abstract fun usageLogDao(): UsageLogDao

    companion object {
        const val DATABASE_NAME = "cupons_sms.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE coupons ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coupons ADD COLUMN confidence REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE coupons ADD COLUMN is_pending INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
