package com.cupons.sms.data.db.dao

import androidx.room.*
import com.cupons.sms.data.db.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

data class MonthlyUsage(val month: String, val total: Double)
data class MerchantUsage(val merchant: String, val usageCount: Int)

/**
 * DAO לטבלת `usage_log`.
 */
@Dao
interface UsageLogDao {

    /**
     * מחזיר Flow של היסטוריית שימושים לקופון, מהחדש לישן.
     */
    @Query("SELECT * FROM usage_log WHERE coupon_id = :couponId ORDER BY used_at DESC")
    fun observeLogForCoupon(couponId: Long): Flow<List<UsageLogEntity>>

    /**
     * הוספת רשומת שימוש חדשה.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UsageLogEntity): Long

    /** סה"כ סכום שנוצל בכל הזמנים */
    @Query("SELECT COALESCE(SUM(amount_used), 0) FROM usage_log")
    suspend fun getTotalAmountUsed(): Double

    /** סה"כ שימושים לפי חודש (לגרף) */
    @Query("""
        SELECT strftime('%Y-%m', used_at / 1000, 'unixepoch') as month,
               SUM(amount_used) as total
        FROM usage_log
        GROUP BY month
        ORDER BY month DESC
        LIMIT 12
    """)
    suspend fun getMonthlyUsage(): List<MonthlyUsage>

    /** מספר שימושים ייחודיים לפי merchant */
    @Query("""
        SELECT c.merchant_name as merchant, COUNT(ul.id) as usageCount
        FROM usage_log ul
        JOIN coupons c ON ul.coupon_id = c.id
        WHERE c.merchant_name IS NOT NULL
        GROUP BY c.merchant_name
        ORDER BY usageCount DESC
        LIMIT 5
    """)
    suspend fun getTopMerchants(): List<MerchantUsage>
}
