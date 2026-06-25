package com.subspy.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.subspy.app.data.model.Subscription
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDaily() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<BillingReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "billing_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork("billing_reminder_check")
    }
}

class BillingReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // In production, this would check Firestore for upcoming subscriptions
        // and trigger local notifications
        return Result.success()
    }

    companion object {
        fun shouldNotify(subscription: Subscription, daysBefore: Int): Boolean {
            val nextBilling = try {
                LocalDate.parse(subscription.nextBillingDate)
            } catch (e: Exception) {
                return false
            }
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextBilling)
            return daysUntil.toInt() == daysBefore
        }
    }
}
