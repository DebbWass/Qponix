package com.cupons.sms.data.db.dao

import androidx.room.*
import com.cupons.sms.data.db.entity.CouponEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO לטבלת `coupons`.
 *
 * כל שאילתות ה-Flow יפעילו emit חדש בכל שינוי בטבלה —
 * הUI יתעדכן אוטומטית ללא polling.
 *
 * Flows לטאבים:
 *  - observeNonArchivedCoupons  → "בתוקף" + "פג בקרוב" + "פגי תוקף" (מפוצל בVM לפי expires_at)
 *  - observeArchivedCoupons     → טאב "ארכיון"
 *  - observeDeletedCoupons      → טאב "נמחקו"
 *  - observePendingCoupons      → קופונים ממתינים לאישור (מוצגים בראש "בתוקף")
 */
@Dao
interface CouponDao {

    // ─── Read / Observe ───

    /**
     * כל הקופונים שאינם ממומשים, אינם בארכיון, ואינם נמחקו.
     * ה-ViewModel מפצל אותם ל"בתוקף" ו"פגי תוקף" לפי expires_at.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE is_used = 0
          AND is_archived = 0
          AND is_deleted = 0
          AND is_pending = 0
        ORDER BY received_at DESC
    """)
    fun observeNonArchivedCoupons(): Flow<List<CouponEntity>>

    /**
     * קופונים ממומשים / בארכיון שאינם נמחקו.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE (is_used = 1 OR is_archived = 1)
          AND is_deleted = 0
        ORDER BY updated_at DESC
    """)
    fun observeArchivedCoupons(): Flow<List<CouponEntity>>

    /**
     * קופונים שנמחקו באופן רך — is_deleted = 1.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE is_deleted = 1
        ORDER BY updated_at DESC
    """)
    fun observeDeletedCoupons(): Flow<List<CouponEntity>>

    /**
     * קופון יחיד לפי ID.
     */
    @Query("SELECT * FROM coupons WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CouponEntity?

    /**
     * קופון לפי SMS ID — משמש למניעת כפילויות בייבוא.
     */
    @Query("SELECT * FROM coupons WHERE sms_id = :smsId LIMIT 1")
    suspend fun getBySmsId(smsId: String): CouponEntity?

    /**
     * קופונים הממתינים לאישור משתמש — is_pending = 1.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE is_pending = 1
          AND is_deleted = 0
        ORDER BY received_at DESC
    """)
    fun observePendingCoupons(): Flow<List<CouponEntity>>

    /**
     * אישור קופון ממתין — מסמן is_pending=0.
     */
    @Query("UPDATE coupons SET is_pending = 0, updated_at = :updatedAt WHERE id = :couponId")
    suspend fun approvePending(couponId: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * מחזיר רשימת כל ה-sms_id שכבר יובאו — לסינון כפילויות.
     */
    @Query("SELECT sms_id FROM coupons WHERE sms_id IS NOT NULL")
    suspend fun getAllSmsIds(): List<String>

    /**
     * ספירת קופונים קיימים (לא-מחוקים) עם אותו קוד ואותו שולח —
     * הגנה מפני כפילויות תוכן: אותה הודעה יכולה להגיע גם דרך
     * SmsReceiver (sms_id סינתטי) וגם דרך סריקת ה-Inbox (sms_id של ה-Provider).
     */
    @Query("""
        SELECT COUNT(*) FROM coupons
        WHERE coupon_code = :code
          AND sender = :sender
          AND is_deleted = 0
    """)
    suspend fun countByCodeAndSender(code: String, sender: String): Int

    // ─── Write ───

    /**
     * הוספת קופון חדש.
     * OnConflict = IGNORE: אם ה-sms_id כבר קיים, מדלג (ללא שגיאה).
     * מחזיר את ה-rowId שנוצר, או -1 אם היה conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(coupon: CouponEntity): Long

    /**
     * עדכון קופון קיים (לפי ID).
     */
    @Update
    suspend fun update(coupon: CouponEntity)

    /**
     * עדכון יתרה ותאריך עדכון בלבד — שאילתה ממוקדת לביצועים טובים יותר.
     */
    @Query("""
        UPDATE coupons
        SET remaining_balance = :newBalance,
            updated_at = :updatedAt
        WHERE id = :couponId
    """)
    suspend fun updateBalance(couponId: Long, newBalance: Double, updatedAt: Long = System.currentTimeMillis())

    /**
     * סימון קופון כממומש ובארכיון.
     */
    @Query("""
        UPDATE coupons
        SET is_used = 1,
            is_archived = 1,
            remaining_balance = 0,
            updated_at = :updatedAt
        WHERE id = :couponId
    """)
    suspend fun markAsUsed(couponId: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * מחיקה רכה — מסמן is_deleted=1 במקום למחוק מה-DB.
     * הקופון יופיע בטאב "נמחקו" ולא ייעלם לחלוטין.
     */
    @Query("""
        UPDATE coupons
        SET is_deleted = 1,
            updated_at = :updatedAt
        WHERE id = :couponId
    """)
    suspend fun softDelete(couponId: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * שחזור קופון שנמחק — מבטל is_deleted.
     */
    @Query("""
        UPDATE coupons
        SET is_deleted = 0,
            updated_at = :updatedAt
        WHERE id = :couponId
    """)
    suspend fun restoreCoupon(couponId: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * מחיקה פיזית לצמיתות — לשימוש עתידי מטאב "נמחקו".
     * ה-ForeignKey CASCADE ימחק גם את ה-usage_log המשויך.
     */
    @Query("DELETE FROM coupons WHERE id = :couponId")
    suspend fun deleteById(couponId: Long)

    /**
     * מחזיר קופונים הפוגים בין [now] ל-[deadline] —
     * לשימוש ב-ExpiryNotificationWorker.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE expires_at IS NOT NULL
          AND expires_at BETWEEN :now AND :deadline
          AND is_used = 0
          AND is_archived = 0
          AND is_deleted = 0
    """)
    suspend fun getCouponsExpiringBefore(now: Long, deadline: Long): List<CouponEntity>

    /**
     * מחזיר את כל הקופונים הפעילים (לגיבוי).
     */
    @Query("SELECT * FROM coupons WHERE is_deleted = 0 ORDER BY received_at DESC")
    suspend fun getAllActive(): List<CouponEntity>

    /**
     * קופונים ללא תאריך תפוגה שיש להם גוף SMS — לסריקה מחדש.
     */
    @Query("""
        SELECT * FROM coupons
        WHERE expires_at IS NULL
          AND raw_sms_body IS NOT NULL
          AND is_deleted = 0
    """)
    suspend fun getCouponsWithNoExpiry(): List<CouponEntity>

    /**
     * עדכון תאריך תפוגה בלבד — לשימוש בסריקה מחדש.
     */
    @Query("UPDATE coupons SET expires_at = :expiresAt, updated_at = :updatedAt WHERE id = :couponId")
    suspend fun updateExpiresAt(
        couponId  : Long,
        expiresAt : Long,
        updatedAt : Long = System.currentTimeMillis()
    )

    /** סה"כ ערך קופונים שפגו תוקפם לפני מימוש (ערך שנאבד) */
    @Query("""
        SELECT COALESCE(SUM(remaining_balance), 0)
        FROM coupons
        WHERE expires_at IS NOT NULL
          AND expires_at < :now
          AND is_used = 0
          AND is_deleted = 0
    """)
    suspend fun getTotalExpiredValue(now: Long = System.currentTimeMillis()): Double

    /** מספר קופונים פעילים */
    @Query("SELECT COUNT(*) FROM coupons WHERE is_used = 0 AND is_archived = 0 AND is_deleted = 0 AND is_pending = 0")
    suspend fun getActiveCouponsCount(): Int

    /** מספר קופונים שמומשו */
    @Query("SELECT COUNT(*) FROM coupons WHERE (is_used = 1 OR is_archived = 1) AND is_deleted = 0")
    suspend fun getUsedCouponsCount(): Int
}
