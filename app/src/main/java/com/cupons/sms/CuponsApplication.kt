package com.cupons.sms

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.cupons.sms.data.workers.ExpiryNotificationWorker
import com.cupons.sms.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CuponsApplication : Application(), Configuration.Provider {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        scheduleExpiryWorker()
    }

    private fun scheduleExpiryWorker() {
        val wm = WorkManager.getInstance(this)
        // תזמון יומי
        wm.enqueueUniquePeriodicWork(
            ExpiryNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            ExpiryNotificationWorker.buildRequest()
        )
        // הרצה מיידית כדי לשלוח התראות מיד עם פתיחת האפליקציה
        wm.enqueue(ExpiryNotificationWorker.buildImmediateRequest())
    }
}
