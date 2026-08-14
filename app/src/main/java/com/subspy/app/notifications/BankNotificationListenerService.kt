package com.subspy.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.subspy.app.data.detection.SubscriptionDetector

/**
 * Reads incoming notifications (e.g. from bank apps) and, when one looks like a
 * payment, stores it as a payment event for later subscription detection.
 * Requires the user to grant "Notification access" in system settings.
 */
class BankNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        // Never read our own notifications.
        if (sbn.packageName == packageName) return

        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val content = listOf(title, text, bigText).joinToString(" ").trim()
        if (content.isBlank()) return

        val lower = content.lowercase()
        if (SubscriptionDetector.PAYMENT_KEYWORDS.none { lower.contains(it) }) return

        val (amount, currency) = SubscriptionDetector.extractAmountAndCurrency(content) ?: return
        val merchant = SubscriptionDetector.extractMerchant(content, title) ?: return
        val displayMerchant = merchant.replaceFirstChar { it.uppercase() }

        NotificationCaptureStore.addEvent(
            context = applicationContext,
            merchant = merchant,
            amount = amount,
            currency = currency,
            sourceApp = sbn.packageName ?: ""
        )

        // Alert the user right after a charge is detected.
        SubSpyNotifier.notifyCharge(
            context = applicationContext,
            merchant = displayMerchant,
            amount = amount,
            currency = currency
        )
    }
}
