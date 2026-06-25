package com.subspy.app

import android.app.Application
import com.subspy.app.billing.BillingManager
import com.subspy.app.notifications.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SubSpyApplication : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onCreate() {
        super.onCreate()
        billingManager.startConnection()
        notificationScheduler.scheduleDaily()
    }

    override fun onTerminate() {
        super.onTerminate()
        billingManager.endConnection()
    }
}
