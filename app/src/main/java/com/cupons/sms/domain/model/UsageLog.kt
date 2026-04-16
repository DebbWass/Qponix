package com.cupons.sms.domain.model

/**
 * רשומת שימוש בקופון — נשמרת בכל פעם שמעדכנים יתרה.
 */
data class UsageLog(
    val id: Long = 0,
    val couponId: Long,

    /** כמה נוצל בשימוש הזה */
    val amountUsed: Double,

    /** היתרה לאחר שימוש זה */
    val balanceAfter: Double,

    /** זמן השימוש (Unix timestamp ms) */
    val usedAt: Long = System.currentTimeMillis(),

    /** הערה אופציונלית */
    val note: String? = null
)
