package com.subspy.app.data.repository

import android.content.Context
import android.provider.Telephony
import com.subspy.app.data.detection.PaymentEvent
import com.subspy.app.data.detection.SubscriptionDetector
import com.subspy.app.data.detection.SubscriptionSource
import com.subspy.app.data.model.Subscription
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device SMS inbox looking for bank/payment messages and turns
 * recurring charges into [Subscription]s. Common in RU / IN / Asia where banks
 * text every charge. Requires the READ_SMS runtime permission.
 */
@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val PAYMENT_KEYWORDS = listOf(
            // English
            "paid", "payment", "charged", "debited", "purchase", "subscription", "transaction",
            // Russian
            "оплата", "оплачено", "списан", "списание", "покупка", "платеж", "платёж",
            // Ukrainian
            "оплата", "сплачено", "списано", "покупка",
            // Hindi (romanised) / generic bank
            "txn", "debit", "spent"
        )

        private val MERCHANT_PATTERNS = listOf(
            Regex("""(?:to|at|for)\s+([A-Za-z][A-Za-z0-9&.\-* ]{2,30})""", RegexOption.IGNORE_CASE),
            Regex("""(?:в|у|на)\s+([A-Za-zА-Яа-я][A-Za-zА-Яа-я0-9&.\-* ]{2,30})""", RegexOption.IGNORE_CASE)
        )
    }

    suspend fun scanSms(): List<Subscription> = withContext(Dispatchers.IO) {
        val events = mutableListOf<PaymentEvent>()

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

            var scanned = 0
            while (cursor.moveToNext() && scanned < 2000) {
                scanned++
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                if (body.isBlank()) continue
                val lower = body.lowercase()
                if (PAYMENT_KEYWORDS.none { lower.contains(it) }) continue

                val (amount, currency) = SubscriptionDetector.extractAmountAndCurrency(body)
                    ?: continue

                val address = if (addressIdx >= 0) cursor.getString(addressIdx) ?: "" else ""
                val dateMillis = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()

                val merchant = extractMerchant(body, address) ?: continue

                events.add(
                    PaymentEvent(
                        merchantKey = merchant.lowercase(),
                        displayName = merchant.replaceFirstChar { it.uppercase() },
                        amount = amount,
                        currency = currency,
                        date = date,
                        source = SubscriptionSource.SMS,
                        rawSender = address
                    )
                )
            }
        }

        SubscriptionDetector.detectSubscriptions(events)
    }

    private fun extractMerchant(body: String, address: String): String? {
        val lower = body.lowercase()
        // 1. Prefer a known service name mentioned in the message.
        SubscriptionDetector.KNOWN_SERVICES.keys.firstOrNull { lower.contains(it) }?.let {
            return it
        }
        // 2. Try "to/at <Merchant>" style patterns.
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            val candidate = match?.groupValues?.getOrNull(1)?.trim()
            if (!candidate.isNullOrBlank()) {
                return candidate.split(Regex("""\s{2,}|[.,;]""")).first().trim()
            }
        }
        // 3. Fall back to the sender if it looks like a name (not a phone number).
        if (address.isNotBlank() && !address.any { it.isDigit() }) {
            return address
        }
        return null
    }
}
