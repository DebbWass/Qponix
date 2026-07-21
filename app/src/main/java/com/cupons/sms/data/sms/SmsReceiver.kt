package com.cupons.sms.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.util.NotificationHelper
import javax.inject.Inject

/**
 * SmsReceiver — מקבל הודעות SMS חדשות בזמן אמת.
 *
 * מוגדר ב-AndroidManifest עם action SMS_RECEIVED.
 * כל הודעה חדשה עוברת דרך ה-Parser —
 * אם זוהתה כקופון, נשמרת ב-DB.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var parser: SmsParser
    @Inject lateinit var repository: CouponRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var prefs: AppPreferences

    // Scope ל-coroutines — SupervisorJob מבטיח שכישלון אחד לא יפיל אחרים
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmsReceiver"

        // בסיס ל-ID של התראות "קופון חדש" — יציב לפי id הקופון,
        // ומופרד מטווח ההתראות של ExpiryNotificationWorker
        private const val NEW_COUPON_NOTIF_BASE = 200_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // חילוץ הודעות מה-Intent
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // מחבר הודעות רב-חלקיות לטקסט אחד
        val sender     = messages[0].originatingAddress ?: ""
        val body       = messages.joinToString("") { it.messageBody ?: "" }
        val receivedAt = messages[0].timestampMillis

        Log.d(TAG, "Received SMS from '$sender'")

        // goAsync — משאיר את התהליך חי עד finish(), אחרת המערכת עלולה
        // להרוג את התהליך לפני שהשמירה ב-DB מסתיימת (אובדן קופונים)
        val pendingResult = goAsync()

        scope.launch {
            try {
                // פרסור עם מילות מפתח/רשימה שחורה מותאמות אישית מה-DataStore
                val customKeywords  = prefs.customKeywords.first().toList()
                val customBlacklist = prefs.customBlacklist.first().toList()
                val parsed = parser.parse(body, sender, receivedAt, customKeywords, customBlacklist)
                    ?: return@launch

                // בדיקה: האם ה-SMS הזה כבר קיים?
                // (SMS חדשים אין להם ID קבוע עדיין, נשתמש ב-timestamp+sender)
                val tempSmsId = "${sender}_${receivedAt}"

                val coupon = Coupon(
                    smsId            = tempSmsId,
                    sender           = sender,
                    rawSmsBody       = body,
                    couponCode       = parsed.couponCode,
                    originalAmount   = parsed.originalAmount,
                    remainingBalance = parsed.originalAmount,
                    currency         = parsed.currency,
                    merchantName     = parsed.merchantName,
                    websiteUrl       = parsed.websiteUrl,
                    receivedAt       = receivedAt,
                    expiresAt        = parsed.expiresAt,
                    confidence       = parsed.confidence,
                    isPending        = parsed.requiresUserConfirmation
                )

                val insertedId = repository.insertCoupon(coupon)
                if (insertedId > 0) {
                    Log.d(TAG, "New coupon saved: id=$insertedId, code=${parsed.couponCode}")
                    notificationHelper.notifyNewCoupon(
                        couponCode   = parsed.couponCode,
                        merchantName = parsed.merchantName,
                        notifId      = (NEW_COUPON_NOTIF_BASE + insertedId % 100_000).toInt()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save coupon from SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
