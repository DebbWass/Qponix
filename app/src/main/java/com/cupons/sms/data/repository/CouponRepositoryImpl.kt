package com.cupons.sms.data.repository

import androidx.room.withTransaction
import com.cupons.sms.data.db.AppDatabase
import com.cupons.sms.data.db.dao.CouponDao
import com.cupons.sms.data.db.dao.UsageLogDao
import com.cupons.sms.data.db.entity.UsageLogEntity
import com.cupons.sms.data.db.entity.toEntity
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.model.UsageLog
import com.cupons.sms.domain.repository.CouponRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * מימוש ה-Repository — מגשר בין שכבת ה-Domain לשכבת ה-DB.
 *
 * ממיר entities ל-domain models ולהיפך.
 * כל פעולה היא suspending / Flow — מאפשרת עבודה ב-coroutines.
 */
@Singleton
class CouponRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val couponDao: CouponDao,
    private val usageLogDao: UsageLogDao
) : CouponRepository {

    // ─── Queries ───

    override fun getNonArchivedCoupons(): Flow<List<Coupon>> =
        couponDao.observeNonArchivedCoupons().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getArchivedCoupons(): Flow<List<Coupon>> =
        couponDao.observeArchivedCoupons().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDeletedCoupons(): Flow<List<Coupon>> =
        couponDao.observeDeletedCoupons().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getPendingCoupons(): Flow<List<Coupon>> =
        couponDao.observePendingCoupons().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getCouponById(id: Long): Coupon? =
        couponDao.getById(id)?.toDomain()

    override suspend fun getCouponBySmsId(smsId: String): Coupon? =
        couponDao.getBySmsId(smsId)?.toDomain()

    override suspend fun getImportedSmsIds(): Set<String> =
        couponDao.getAllSmsIds().toSet()

    // ─── Mutations ───

    override suspend fun insertCoupon(coupon: Coupon): Long {
        // הגנת כפילויות תוכן לקופונים ממקור SMS/WhatsApp (smsId != null):
        // אותה הודעה יכולה להגיע גם בזמן אמת (sms_id סינתטי) וגם בסריקת Inbox
        // (sms_id של ה-Provider) — sms_id שונה, אז האינדקס הייחודי לא תופס.
        // קופונים ידניים (smsId == null) פטורים כדי לא לחסום הזנה חוזרת מכוונת.
        if (coupon.smsId != null &&
            couponDao.countByCodeAndSender(coupon.couponCode, coupon.sender) > 0
        ) {
            return -1L
        }
        return couponDao.insert(coupon.toEntity())
    }

    override suspend fun updateCoupon(coupon: Coupon) =
        couponDao.update(coupon.toEntity())

    /**
     * עדכון יתרה — מעדכן את שדה ה-balance ומוסיף רשומה ל-usage_log.
     * שתי הפעולות בטרנזקציה אחת: או ששתיהן מצליחות או ששתיהן מתבטלות
     * (מונע חוסר-סנכרון בין היתרה ליומן השימוש בקריסה).
     */
    override suspend fun updateBalance(
        couponId: Long,
        newBalance: Double,
        amountUsed: Double,
        note: String?
    ) {
        db.withTransaction {
            couponDao.updateBalance(couponId, newBalance)
            usageLogDao.insert(
                UsageLogEntity(
                    couponId     = couponId,
                    amountUsed   = amountUsed,
                    balanceAfter = newBalance,
                    note         = note
                )
            )
        }
    }

    /**
     * סימון קופון כממומש — מאפס יתרה ורושם רשומת usage_log על היתרה שנוצלה,
     * כדי שסטטיסטיקת ה"נחסך" לא תחסיר קופונים שנסגרו כאן.
     * אידמפוטנטי: קריאה שנייה (יתרה כבר 0) לא תוסיף רשומה נוספת.
     */
    override suspend fun markAsUsed(couponId: Long) {
        db.withTransaction {
            val coupon = couponDao.getById(couponId)
            val remaining = coupon?.remainingBalance
            if (remaining != null && remaining > 0.0) {
                usageLogDao.insert(
                    UsageLogEntity(
                        couponId     = couponId,
                        amountUsed   = remaining,
                        balanceAfter = 0.0,
                        note         = null
                    )
                )
            }
            couponDao.markAsUsed(couponId)
        }
    }

    /** מחיקה רכה — הקופון יעבור לטאב "נמחקו" */
    override suspend fun deleteCoupon(couponId: Long) =
        couponDao.softDelete(couponId)

    /** שחזור קופון שנמחק */
    override suspend fun restoreCoupon(couponId: Long) =
        couponDao.restoreCoupon(couponId)

    /** אישור קופון ממתין */
    override suspend fun approvePendingCoupon(couponId: Long) =
        couponDao.approvePending(couponId)

    // ─── Usage Log ───

    override fun getUsageLog(couponId: Long): Flow<List<UsageLog>> =
        usageLogDao.observeLogForCoupon(couponId).map { entities ->
            entities.map { it.toDomain() }
        }
}
