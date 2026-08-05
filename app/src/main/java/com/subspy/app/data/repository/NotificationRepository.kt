package com.subspy.app.data.repository

import android.content.Context
import com.subspy.app.data.detection.SubscriptionDetector
import com.subspy.app.data.model.Subscription
import com.subspy.app.notifications.NotificationCaptureStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns payment events captured from bank push notifications into recurring
 * [Subscription]s. Notifications are collected in the background by
 * [com.subspy.app.notifications.BankNotificationListenerService].
 */
@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanNotifications(): List<Subscription> = withContext(Dispatchers.IO) {
        val events = NotificationCaptureStore.getEvents(context)
        SubscriptionDetector.detectSubscriptions(events)
    }
}
