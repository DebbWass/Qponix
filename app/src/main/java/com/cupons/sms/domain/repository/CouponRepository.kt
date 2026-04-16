package com.cupons.sms.domain.repository

import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.model.UsageLog
import kotlinx.coroutines.flow.Flow

/**
 * ממשק ה-Repository — הגדרת החוזה בין שכבת Domain לשכבת Data.
 * הממשק מאפשר החלפת ה-implementation ללא שינוי ב-ViewModels.
 *
 * שלושה Flows ל-4 הטאבים:
 *  - getNonArchivedCoupons  → טאב "בתוקף" + "פגי תוקף" (מפוצל ב-ViewModel)
 *  - getArchivedCoupons     → טאב "ארכיון"
 *  - getDeletedCoupons      → טאב "נמחקו"
 */
interface CouponRepository {

    // ─── Coupon Queries ───

    /**
     * קופונים שאינם ממומשים, אינם בארכיון, ואינם נמחקו, ואינם ממתינים לאישור.
     * ה-ViewModel מפצל ל"בתוקף" ו"פגי תוקף" לפי expiresAt.
     */
    fun getNonArchivedCoupons(): Flow<List<Coupon>>

    /** קופונים הממתינים לאישור משתמש */
    fun getPendingCoupons(): Flow<List<Coupon>>

    /** קופונים ממומשים / בארכיון, שאינם נמחקו */
    fun getArchivedCoupons(): Flow<List<Coupon>>

    /** קופונים שנמחקו באופן רך */
    fun getDeletedCoupons(): Flow<List<Coupon>>

    /** מחזיר קופון יחיד לפי ID */
    suspend fun getCouponById(id: Long): Coupon?

    /** מחזיר קופון לפי SMS ID (למניעת כפילויות) */
    suspend fun getCouponBySmsId(smsId: String): Coupon?

    // ─── Coupon Mutations ───

    /** שמירת קופון חדש. מחזיר את ה-ID שנוצר. */
    suspend fun insertCoupon(coupon: Coupon): Long

    /** עדכון קופון קיים */
    suspend fun updateCoupon(coupon: Coupon)

    /** עדכון יתרה בלבד — שמור גם רשומת UsageLog */
    suspend fun updateBalance(couponId: Long, newBalance: Double, amountUsed: Double, note: String? = null)

    /** סימון קופון כממומש והעברתו לארכיון */
    suspend fun markAsUsed(couponId: Long)

    /** מחיקה רכה — מסמן is_deleted=1, הקופון יועבר לטאב "נמחקו" */
    suspend fun deleteCoupon(couponId: Long)

    /** שחזור קופון שנמחק — מבטל is_deleted */
    suspend fun restoreCoupon(couponId: Long)

    /** אישור קופון ממתין — מסמן is_pending=0 */
    suspend fun approvePendingCoupon(couponId: Long)

    // ─── Usage Log ───

    /** מחזיר Flow של היסטוריית שימושים לקופון מסוים */
    fun getUsageLog(couponId: Long): Flow<List<UsageLog>>

    // ─── SMS ───

    /** מחזיר IDs של כל ה-SMS שכבר יובאו (למניעת כפילויות) */
    suspend fun getImportedSmsIds(): Set<String>
}
