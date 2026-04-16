package com.cupons.sms.data.repository

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

    override suspend fun insertCoupon(coupon: Coupon): Long =
        couponDao.insert(coupon.toEntity())

    override suspend fun updateCoupon(coupon: Coupon) =
        couponDao.update(coupon.toEntity())

    /**
     * עדכון יתרה — מעדכן את שדה ה-balance ומוסיף רשומה ל-usage_log.
     */
    override suspend fun updateBalance(
        couponId: Long,
        newBalance: Double,
        amountUsed: Double,
        note: String?
    ) {
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

    override suspend fun markAsUsed(couponId: Long) =
        couponDao.markAsUsed(couponId)

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
