package com.cupons.sms.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cupons.sms.data.db.dao.CouponDao
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import com.cupons.sms.data.prefs.AppPreferences
import com.cupons.sms.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * ExpiryNotificationWorker — רץ פעם ביום, שולח התראה על קופונים שעומדים לפוג.
 * מכבד את הגדרת "כמה ימים מראש" מ-AppPreferences.
 */
@HiltWorker
class ExpiryNotificationWorker @AssistedInject constructor(
    @Assisted context        : Context,
    @Assisted workerParams   : WorkerParameters,
    private val dao          : CouponDao,
    private val prefs        : AppPreferences,
    private val notifHelper  : NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG          = "ExpiryWorker"
        const val WORK_NAME            = "expiry_notification_work"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(1, TimeUnit.DAYS)
                .build()

        fun buildImmediateRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ExpiryNotificationWorker>().build()
    }

    override suspend fun doWork(): Result {
        return try {
            val notificationsEnabled = prefs.notificationsEnabled.first()
            if (!notificationsEnabled) return Result.success()

            val daysBefore = prefs.notificationDaysBefore.first()
            val now        = System.currentTimeMillis()
            val deadline   = now + TimeUnit.DAYS.toMillis(daysBefore.toLong())

            val expiring = dao.getCouponsExpiringBefore(now, deadline)
            Log.d(TAG, "Found ${expiring.size} expiring coupons")

            expiring.forEachIndexed { index, coupon ->
                val daysLeft = ((coupon.expiresAt!! - now) / TimeUnit.DAYS.toMillis(1)).toInt()
                    .coerceAtLeast(0)
                notifHelper.notifyExpiringSoon(
                    couponCode   = coupon.couponCode,
                    merchantName = coupon.merchantName,
                    daysLeft     = daysLeft,
                    notifId      = 1000 + index
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ExpiryNotificationWorker failed", e)
            Result.retry()
        }
    }
}
