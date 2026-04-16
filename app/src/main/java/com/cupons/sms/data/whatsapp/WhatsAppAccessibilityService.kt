package com.cupons.sms.data.whatsapp

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.data.sms.SmsParser
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.repository.CouponRepository
import com.cupons.sms.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WhatsAppAccessibilityService — מאזין להתראות WhatsApp וסורק לקופונים.
 *
 * כאשר מגיעה התראת WhatsApp, מחלצים את תוכן ההודעה ומעבירים ל-SmsParser.
 * אם מזוהה קופון — נשמר ב-DB.
 *
 * כדי להפעיל: Settings → Accessibility → Qponix → הפעל
 */
@AndroidEntryPoint
class WhatsAppAccessibilityService : AccessibilityService() {

    @Inject lateinit var parser            : SmsParser
    @Inject lateinit var repository        : CouponRepository
    @Inject lateinit var prefs             : AppPreferences
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG            = "WAAccessibility"
        private const val WHATSAPP_PKG   = "com.whatsapp"
        private const val WHATSAPP_BUSS  = "com.whatsapp.w4b"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return
        if (event.packageName !in listOf(WHATSAPP_PKG, WHATSAPP_BUSS)) return

        scope.launch {
            // בדוק שהמשתמש הפעיל את האפשרות
            if (!prefs.whatsappEnabled.first()) return@launch

            val parcelable = event.parcelableData
            if (parcelable !is Notification) return@launch

            val tickerText = (
                parcelable.extras?.getCharSequence(Notification.EXTRA_TEXT)
                    ?: parcelable.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: parcelable.tickerText
            )?.toString()?.takeIf { it.isNotBlank() } ?: return@launch
            val sender     = event.packageName?.toString() ?: WHATSAPP_PKG
            val receivedAt = System.currentTimeMillis()

            Log.d(TAG, "WhatsApp notification: ${tickerText.take(80)}")

            val parsed = parser.parse(tickerText, sender, receivedAt) ?: return@launch

            val tempId = "${sender}_wa_${receivedAt}"
            val rejectedIds = prefs.getRejectedSmsIds()
            if (tempId in rejectedIds) return@launch

            val coupon = Coupon(
                smsId            = tempId,
                sender           = "WhatsApp",
                rawSmsBody       = tickerText,
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
                Log.d(TAG, "Coupon from WhatsApp saved: ${parsed.couponCode}")
                if (!parsed.requiresUserConfirmation) {
                    notificationHelper.notifyNewCoupon(
                        couponCode   = parsed.couponCode,
                        merchantName = parsed.merchantName,
                        notifId      = (receivedAt % Int.MAX_VALUE).toInt()
                    )
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "WhatsApp Accessibility Service connected")
    }
}
