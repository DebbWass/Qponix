package com.cupons.sms.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cupons.sms.domain.model.Coupon

/**
 * ישות Room עבור טבלת `coupons`.
 *
 * Index ייחודי על sms_id מונע ייבוא כפול של אותה הודעת SMS.
 */
@Entity(
    tableName = "coupons",
    indices = [Index(value = ["sms_id"], unique = true)]
)
data class CouponEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ID מ-ContentProvider של SMS (null = נוסף ידנית) */
    @ColumnInfo(name = "sms_id")
    val smsId: String? = null,

    /** שם/מספר השולח */
    @ColumnInfo(name = "sender")
    val sender: String,

    /** גוף ה-SMS המקורי */
    @ColumnInfo(name = "raw_sms_body")
    val rawSmsBody: String? = null,

    /** קוד הקופון */
    @ColumnInfo(name = "coupon_code")
    val couponCode: String,

    /** הסכום המקורי */
    @ColumnInfo(name = "original_amount")
    val originalAmount: Double? = null,

    /** יתרה נוכחית */
    @ColumnInfo(name = "remaining_balance")
    val remainingBalance: Double? = null,

    /** מטבע */
    @ColumnInfo(name = "currency")
    val currency: String = "₪",

    /** שם בית העסק */
    @ColumnInfo(name = "merchant_name")
    val merchantName: String? = null,

    /** קישור לאתר */
    @ColumnInfo(name = "website_url")
    val websiteUrl: String? = null,

    /** זמן קבלה (Unix ms) */
    @ColumnInfo(name = "received_at")
    val receivedAt: Long,

    /** תפוגה (Unix ms), null = לא ידוע */
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    /** 0 = פעיל, 1 = ממומש */
    @ColumnInfo(name = "is_used")
    val isUsed: Boolean = false,

    /** 0 = רגיל, 1 = בארכיון */
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    /** מחיקה רכה — 1 = נמחק (גלוי בטאב נמחקו) */
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    /** הערה חופשית */
    @ColumnInfo(name = "notes")
    val notes: String? = null,

    /** ציון בטחון של ה-parser (0.0–1.0) */
    @ColumnInfo(name = "confidence")
    val confidence: Float = 0.5f,

    /** ממתין לאישור משתמש (confidence < CONFIDENCE_AUTO_IMPORT) */
    @ColumnInfo(name = "is_pending")
    val isPending: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** המרה מ-Entity ל-Domain Model */
    fun toDomain(): Coupon = Coupon(
        id = id,
        smsId = smsId,
        sender = sender,
        rawSmsBody = rawSmsBody,
        couponCode = couponCode,
        originalAmount = originalAmount,
        remainingBalance = remainingBalance,
        currency = currency,
        merchantName = merchantName,
        websiteUrl = websiteUrl,
        receivedAt = receivedAt,
        expiresAt = expiresAt,
        isUsed = isUsed,
        isArchived = isArchived,
        isDeleted = isDeleted,
        notes = notes,
        confidence = confidence,
        isPending = isPending,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/** המרה מ-Domain Model ל-Entity */
fun Coupon.toEntity(): CouponEntity = CouponEntity(
    id = id,
    smsId = smsId,
    sender = sender,
    rawSmsBody = rawSmsBody,
    couponCode = couponCode,
    originalAmount = originalAmount,
    remainingBalance = remainingBalance,
    currency = currency,
    merchantName = merchantName,
    websiteUrl = websiteUrl,
    receivedAt = receivedAt,
    expiresAt = expiresAt,
    isUsed = isUsed,
    isArchived = isArchived,
    isDeleted = isDeleted,
    notes = notes,
    confidence = confidence,
    isPending = isPending,
    createdAt = createdAt,
    updatedAt = updatedAt
)
