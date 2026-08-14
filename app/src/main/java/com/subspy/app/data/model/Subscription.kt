package com.subspy.app.data.model

import com.google.firebase.firestore.DocumentId
import java.time.LocalDate

data class Subscription(
    @DocumentId
    val id: String = "",
    val serviceName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val frequency: BillingFrequency = BillingFrequency.MONTHLY,
    val nextBillingDate: String = "",
    val firstPaymentDate: String = "",
    val lastPaymentDate: String = "",
    val senderEmail: String = "",
    val logoUrl: String = "",
    val websiteUrl: String = "",
    val cancellationUrl: String = "",
    val isForgotten: Boolean = false,
    val userId: String = "",
    val isActive: Boolean = true,
    val source: String = "",
    val category: SubscriptionCategory = SubscriptionCategory.OTHER,
    val usage: UsageStatus = UsageStatus.UNKNOWN
) {
    val monthlyAmount: Double
        get() = when (frequency) {
            BillingFrequency.MONTHLY -> amount
            BillingFrequency.YEARLY -> amount / 12.0
        }

    val totalSpent: Double
        get() {
            if (firstPaymentDate.isBlank()) return amount
            val first = try { LocalDate.parse(firstPaymentDate) } catch (e: Exception) { return amount }
            val now = LocalDate.now()
            val months = java.time.temporal.ChronoUnit.MONTHS.between(first, now)
            return when (frequency) {
                BillingFrequency.MONTHLY -> amount * (months + 1)
                BillingFrequency.YEARLY -> amount * ((months / 12) + 1)
            }
        }
}

enum class BillingFrequency {
    MONTHLY,
    YEARLY
}

enum class SubscriptionCategory {
    ENTERTAINMENT,
    WORK,
    HEALTH,
    MUSIC,
    OTHER
}

enum class UsageStatus {
    UNKNOWN,
    ACTIVELY_USED,
    NOT_USED
}
