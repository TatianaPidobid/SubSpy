package com.subspy.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.subspy.app.data.model.Subscription
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
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
        val ctx = applicationContext
        val enabledOffsets = NotificationPrefs.enabledOffsets(ctx)
        if (enabledOffsets.isEmpty()) return Result.success()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val subscriptions = try {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("subscriptions")
                .get()
                .await()
                .toObjects(Subscription::class.java)
        } catch (e: Exception) {
            return Result.retry()
        }

        val today = LocalDate.now()
        subscriptions.forEach { sub ->
            if (!sub.isActive) return@forEach
            val nextBilling = try {
                LocalDate.parse(sub.nextBillingDate)
            } catch (e: Exception) {
                return@forEach
            }
            val daysUntil = ChronoUnit.DAYS.between(today, nextBilling).toInt()
            val subscriptionKey = sub.id.ifBlank { sub.serviceName }
            if (daysUntil in enabledOffsets &&
                NotificationPrefs.markReminderSent(
                    ctx,
                    subscriptionKey,
                    daysUntil,
                    sub.nextBillingDate
                )
            ) {
                SubSpyNotifier.notifyReminder(
                    context = ctx,
                    serviceName = sub.serviceName,
                    amount = sub.amount,
                    currency = sub.currency,
                    daysUntil = daysUntil
                )
            }
        }
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
