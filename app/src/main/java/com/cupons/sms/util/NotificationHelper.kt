package com.cupons.sms.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cupons.sms.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_EXPIRY   = "coupon_expiry"
        const val CHANNEL_NEW      = "new_coupon"
        private const val TAG      = "NotificationHelper"
    }

    fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EXPIRY,
                "פקיעת תוקף קופונים",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "התראה לפני שקופון עומד לפוג"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW,
                "קופון חדש",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "קופון חדש זוהה מהודעת SMS"
            }
        )
    }

    fun notifyExpiringSoon(couponCode: String, merchantName: String?, daysLeft: Int, notifId: Int) {
        val merchant = merchantName ?: "קופון"
        val title    = "קופון עומד לפוג"
        val text     = "$merchant — קוד: $couponCode יפוג בעוד $daysLeft ימים"

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pending = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        safeNotify(notifId, notification)
    }

    fun notifyNewCoupon(couponCode: String, merchantName: String?, notifId: Int) {
        val merchant = merchantName ?: "שולח לא ידוע"
        val title    = "קופון חדש!"
        val text     = "$merchant — קוד: $couponCode"

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pending = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        safeNotify(notifId, notification)
    }

    /**
     * בדיקת הרשאת POST_NOTIFICATIONS (נדרשת מ-API 33). לפני כן — תמיד מותר.
     */
    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * שליחת התראה בטוחה — בודק הרשאה, וגם עוטף ב-try/catch כי ההרשאה
     * עלולה להישלל בין הבדיקה לקריאה (וגם משתיק את אזהרת ה-lint).
     */
    private fun safeNotify(notifId: Int, notification: android.app.Notification) {
        if (!canPostNotifications()) return
        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // ההרשאה נשללה — מתעלמים בשקט
        }
    }
}
