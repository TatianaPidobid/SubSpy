package com.subspy.app.data.detection

import java.time.LocalDate

/**
 * A single detected payment/charge coming from any source (Gmail, Outlook, SMS,
 * bank push notification). Multiple events from the same merchant are grouped by
 * [SubscriptionDetector] into a recurring [com.subspy.app.data.model.Subscription].
 */
data class PaymentEvent(
    val merchantKey: String,
    val displayName: String,
    val amount: Double,
    val currency: String = "USD",
    val date: LocalDate,
    val source: String,
    val rawSender: String = ""
)

/** Identifies where a subscription/payment was discovered. */
object SubscriptionSource {
    const val GMAIL = "gmail"
    const val OUTLOOK = "outlook"
    const val SMS = "sms"
    const val NOTIFICATION = "notification"
    const val MANUAL = "manual"
}
