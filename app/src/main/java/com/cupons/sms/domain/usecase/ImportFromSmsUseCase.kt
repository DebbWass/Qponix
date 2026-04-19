package com.cupons.sms.domain.usecase

import android.util.Log
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.data.sms.SmsReader
import kotlinx.coroutines.flow.first
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import javax.inject.Inject

/**
 * ImportFromSmsUseCase — ייבוא קופונים מ-SMS.
 *
 * כל סריקה: קורא את כל ה-inbox, מדלג על IDs שכבר טופלו.
 * IDs שטופלו = שכבר ב-DB + שנדחו על ידי המשתמשת.
 */
class ImportFromSmsUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val repository: CouponRepository,
    private val prefs: AppPreferences
) {
    companion object {
        private const val TAG = "ImportFromSmsUseCase"
    }

    suspend operator fun invoke(): ImportResult {
        // IDs שיש לדלג עליהם — כבר יובאו לDB או נדחו על ידי המשתמשת
        val existingIds    = repository.getImportedSmsIds()
        val rejectedIds    = prefs.getRejectedSmsIds()
        val idsToSkip      = existingIds + rejectedIds
        val customKeywords = prefs.customKeywords.first().toList()
        Log.d(TAG, "idsToSkip: ${idsToSkip.size} (existing=${existingIds.size}, rejected=${rejectedIds.size})")
        Log.d(TAG, "customKeywords: $customKeywords")

        // סריקה — SmsReader מדלג אוטומטית על idsToSkip
        val rawMessages = smsReader.readAll(idsToSkip, customKeywords)
        Log.d(TAG, "Candidates after skip: ${rawMessages.size}")

        var imported = 0
        val pending  = mutableListOf<Coupon>()

        for (msg in rawMessages) {
            val coupon = Coupon(
                smsId            = msg.smsId,
                sender           = msg.sender,
                rawSmsBody       = msg.body,
                couponCode       = msg.parsed.couponCode,
                originalAmount   = msg.parsed.originalAmount,
                remainingBalance = msg.parsed.originalAmount,
                currency         = msg.parsed.currency,
                merchantName     = msg.parsed.merchantName,
                websiteUrl       = msg.parsed.websiteUrl,
                receivedAt       = msg.receivedAt,
                expiresAt        = msg.parsed.expiresAt
            )

            if (msg.parsed.requiresUserConfirmation) {
                val pendingCoupon = coupon.copy(isPending = true, confidence = msg.parsed.confidence)
                val id = repository.insertCoupon(pendingCoupon)
                if (id > 0) pending.add(pendingCoupon.copy(id = id))
                Log.d(TAG, "Pending: ${coupon.couponCode} (conf=${msg.parsed.confidence})")
            } else {
                val id = repository.insertCoupon(coupon.copy(confidence = msg.parsed.confidence))
                if (id > 0) { imported++; Log.d(TAG, "Imported: ${coupon.couponCode}") }
            }
        }

        Log.d(TAG, "Done: imported=$imported, pending=${pending.size}")
        return ImportResult(imported = imported, pendingConfirmation = pending)
    }
}

data class ImportResult(
    val imported: Int,
    val pendingConfirmation: List<Coupon>
)
