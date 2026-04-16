package com.cupons.sms.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cupons.sms.domain.model.UsageLog

/**
 * ישות Room עבור טבלת `usage_log`.
 *
 * ForeignKey עם CASCADE DELETE: כשנמחק קופון, כל ה-logs שלו נמחקים אוטומטית.
 */
@Entity(
    tableName = "usage_log",
    foreignKeys = [
        ForeignKey(
            entity = CouponEntity::class,
            parentColumns = ["id"],
            childColumns = ["coupon_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["coupon_id"])]
)
data class UsageLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "coupon_id")
    val couponId: Long,

    /** כמה נוצל בשימוש זה */
    @ColumnInfo(name = "amount_used")
    val amountUsed: Double,

    /** יתרה לאחר שימוש זה */
    @ColumnInfo(name = "balance_after")
    val balanceAfter: Double,

    @ColumnInfo(name = "used_at")
    val usedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "note")
    val note: String? = null
) {
    fun toDomain(): UsageLog = UsageLog(
        id = id,
        couponId = couponId,
        amountUsed = amountUsed,
        balanceAfter = balanceAfter,
        usedAt = usedAt,
        note = note
    )
}

fun UsageLog.toEntity(): UsageLogEntity = UsageLogEntity(
    id = id,
    couponId = couponId,
    amountUsed = amountUsed,
    balanceAfter = balanceAfter,
    usedAt = usedAt,
    note = note
)
