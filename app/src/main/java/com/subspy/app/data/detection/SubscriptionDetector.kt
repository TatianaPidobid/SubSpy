package com.subspy.app.data.detection

import com.subspy.app.data.model.BillingFrequency
import com.subspy.app.data.model.Subscription
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Shared logic for turning raw [PaymentEvent]s (from Gmail, Outlook, SMS or bank
 * notifications) into recurring [Subscription]s. Centralised so every payment
 * source detects subscriptions the same way.
 */
object SubscriptionDetector {

    val KNOWN_SERVICES: Map<String, Pair<String, String>> = mapOf(
        "netflix" to Pair("Netflix", "https://www.netflix.com"),
        "spotify" to Pair("Spotify", "https://www.spotify.com"),
        "apple" to Pair("Apple", "https://www.apple.com"),
        "itunes" to Pair("Apple", "https://www.apple.com"),
        "amazon" to Pair("Amazon Prime", "https://www.amazon.com"),
        "google" to Pair("Google", "https://play.google.com"),
        "youtube" to Pair("YouTube Premium", "https://www.youtube.com"),
        "hulu" to Pair("Hulu", "https://www.hulu.com"),
        "disney" to Pair("Disney+", "https://www.disneyplus.com"),
        "hbo" to Pair("HBO Max", "https://www.max.com"),
        "adobe" to Pair("Adobe", "https://www.adobe.com"),
        "microsoft" to Pair("Microsoft 365", "https://www.microsoft.com"),
        "dropbox" to Pair("Dropbox", "https://www.dropbox.com"),
        "slack" to Pair("Slack", "https://slack.com"),
        "zoom" to Pair("Zoom", "https://zoom.us"),
        "notion" to Pair("Notion", "https://www.notion.so"),
        "canva" to Pair("Canva", "https://www.canva.com"),
        "grammarly" to Pair("Grammarly", "https://www.grammarly.com"),
        "openai" to Pair("ChatGPT Plus", "https://chat.openai.com"),
        "chatgpt" to Pair("ChatGPT Plus", "https://chat.openai.com"),
        "duolingo" to Pair("Duolingo", "https://www.duolingo.com"),
        "headspace" to Pair("Headspace", "https://www.headspace.com"),
        "peloton" to Pair("Peloton", "https://www.onepeloton.com"),
        "nordvpn" to Pair("NordVPN", "https://nordvpn.com"),
        "expressvpn" to Pair("ExpressVPN", "https://www.expressvpn.com")
    )

    /** Words that indicate a message/notification is about a payment. */
    val PAYMENT_KEYWORDS = listOf(
        // English
        "paid", "payment", "charged", "debited", "purchase", "subscription", "transaction",
        "receipt", "renewal", "billed", "spent", "debit", "txn",
        // Russian
        "оплата", "оплачено", "списан", "списание", "покупка", "платеж", "платёж", "чек",
        // Ukrainian
        "сплачено", "списано", "оплата", "покупка"
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:to|at|for|from)\s+([A-Za-z][A-Za-z0-9&.\-* ]{2,30})""", RegexOption.IGNORE_CASE),
        Regex("""(?:в|у|на|від)\s+([A-Za-zА-Яа-яІіЇїЄє][A-Za-zА-Яа-яІіЇїЄє0-9&.\-* ]{2,30})""", RegexOption.IGNORE_CASE)
    )

    /**
     * Best-effort extraction of a merchant name from free text (SMS body or bank
     * notification). Falls back to [fallbackSender] when it looks like a name.
     */
    fun extractMerchant(text: String, fallbackSender: String): String? {
        val lower = text.lowercase()
        KNOWN_SERVICES.keys.firstOrNull { lower.contains(it) }?.let { return it }
        for (pattern in MERCHANT_PATTERNS) {
            val candidate = pattern.find(text)?.groupValues?.getOrNull(1)?.trim()
            if (!candidate.isNullOrBlank()) {
                return candidate.split(Regex("""\s{2,}|[.,;]""")).first().trim()
            }
        }
        if (fallbackSender.isNotBlank() && !fallbackSender.any { it.isDigit() }) {
            return fallbackSender
        }
        return null
    }

    /**
     * Currency symbols/codes we recognise, mapped to an ISO code, ordered so the
     * regex tries the most specific tokens first.
     */
    private val CURRENCY_TOKENS = listOf(
        "$" to "USD", "USD" to "USD",
        "€" to "EUR", "EUR" to "EUR",
        "£" to "GBP", "GBP" to "GBP",
        "₹" to "INR", "INR" to "INR", "Rs" to "INR",
        "₽" to "RUB", "RUB" to "RUB", "руб" to "RUB", "р." to "RUB",
        "₴" to "UAH", "UAH" to "UAH", "грн" to "UAH",
        "zł" to "PLN", "PLN" to "PLN"
    )

    /** Extracts (amount, currency) from arbitrary text, or null if none found. */
    fun extractAmountAndCurrency(text: String): Pair<Double, String>? {
        // Symbol/code before the number: $12.99, USD 12.99, ₹499
        val before = Regex("""(\$|€|£|₹|₽|₴|zł|руб|грн|USD|EUR|GBP|INR|RUB|UAH|PLN|Rs)\s?(\d{1,3}(?:[ ,]\d{3})*(?:[.,]\d{1,2})?)""", RegexOption.IGNORE_CASE)
        before.find(text)?.let { m ->
            val currency = normalizeCurrency(m.groupValues[1])
            parseNumber(m.groupValues[2])?.let { return it to currency }
        }
        // Number then symbol/code: 12,99 € / 499 INR / 12.99 USD
        val after = Regex("""(\d{1,3}(?:[ ,]\d{3})*(?:[.,]\d{1,2})?)\s?(\$|€|£|₹|₽|₴|zł|руб|грн|USD|EUR|GBP|INR|RUB|UAH|PLN|Rs)""", RegexOption.IGNORE_CASE)
        after.find(text)?.let { m ->
            val currency = normalizeCurrency(m.groupValues[2])
            parseNumber(m.groupValues[1])?.let { return it to currency }
        }
        return null
    }

    private fun normalizeCurrency(token: String): String {
        val t = token.trim().lowercase()
        return CURRENCY_TOKENS.firstOrNull { it.first.lowercase() == t }?.second ?: "USD"
    }

    private fun parseNumber(raw: String): Double? {
        var s = raw.trim().replace(" ", "")
        // Handle both "1,234.56" and "1.234,56" / "12,99" styles.
        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')
        s = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) s.replace(".", "").replace(",", ".")
                else s.replace(",", "")
            lastComma >= 0 -> // only comma: treat as decimal separator
                s.replace(",", ".")
            else -> s
        }
        return s.toDoubleOrNull()
    }

    /**
     * Groups payment events by merchant and keeps only merchants seen at least
     * twice (a real recurring charge). Manual entries should bypass this and be
     * saved directly.
     */
    fun detectSubscriptions(events: List<PaymentEvent>): List<Subscription> {
        val grouped = events.groupBy { it.merchantKey }

        return grouped.mapNotNull { (merchantKey, merchantEvents) ->
            if (merchantEvents.size < 2) return@mapNotNull null

            val sortedByDate = merchantEvents.sortedBy { it.date }
            val mostCommonAmount = merchantEvents.groupBy { it.amount }
                .maxByOrNull { it.value.size }?.key ?: return@mapNotNull null

            val frequency = detectFrequency(sortedByDate)
            val serviceInfo = KNOWN_SERVICES.entries.firstOrNull {
                merchantKey.contains(it.key) ||
                    merchantEvents.first().displayName.lowercase().contains(it.key)
            }

            val lastDate = sortedByDate.last().date
            val nextBilling = calculateNextBilling(lastDate, frequency)
            val isForgotten = ChronoUnit.MONTHS.between(lastDate, LocalDate.now()) >= 6
            val sources = merchantEvents.map { it.source }.distinct()
            val source = if (sources.size == 1) sources.first() else "multiple"

            Subscription(
                id = merchantKey.hashCode().toString(),
                serviceName = serviceInfo?.value?.first ?: merchantEvents.first().displayName,
                amount = mostCommonAmount,
                currency = merchantEvents.first().currency,
                frequency = frequency,
                nextBillingDate = nextBilling.toString(),
                firstPaymentDate = sortedByDate.first().date.toString(),
                lastPaymentDate = lastDate.toString(),
                senderEmail = merchantEvents.first().rawSender,
                websiteUrl = serviceInfo?.value?.second ?: "",
                isForgotten = isForgotten,
                isActive = true,
                source = source
            )
        }.sortedByDescending { it.amount }
    }

    private fun detectFrequency(events: List<PaymentEvent>): BillingFrequency {
        if (events.size < 2) return BillingFrequency.MONTHLY
        val intervals = events.zipWithNext().map { (a, b) ->
            ChronoUnit.DAYS.between(a.date, b.date)
        }
        val avgInterval = intervals.average()
        return if (avgInterval > 180) BillingFrequency.YEARLY else BillingFrequency.MONTHLY
    }

    private fun calculateNextBilling(lastDate: LocalDate, frequency: BillingFrequency): LocalDate {
        var next = when (frequency) {
            BillingFrequency.MONTHLY -> lastDate.plusMonths(1)
            BillingFrequency.YEARLY -> lastDate.plusYears(1)
        }
        while (next.isBefore(LocalDate.now())) {
            next = when (frequency) {
                BillingFrequency.MONTHLY -> next.plusMonths(1)
                BillingFrequency.YEARLY -> next.plusYears(1)
            }
        }
        return next
    }
}
