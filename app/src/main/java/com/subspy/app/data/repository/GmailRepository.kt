package com.subspy.app.data.repository

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.subspy.app.data.model.BillingFrequency
import com.subspy.app.data.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailRepository @Inject constructor() {

    companion object {
        private val SEARCH_KEYWORDS = listOf(
            "receipt", "invoice", "payment", "subscription",
            "billing", "charged", "renewal", "your subscription"
        )

        private val AMOUNT_PATTERN = Regex("""\$(\d+(?:\.\d{2})?)""")
        private val DATE_PATTERNS = listOf(
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d MMM yyyy")
        )

        private val KNOWN_SERVICES = mapOf(
            "netflix" to Pair("Netflix", "https://www.netflix.com"),
            "spotify" to Pair("Spotify", "https://www.spotify.com"),
            "apple" to Pair("Apple", "https://www.apple.com"),
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
    }

    suspend fun scanEmails(credential: GoogleAccountCredential): List<Subscription> =
        withContext(Dispatchers.IO) {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val gmailService = Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName("SubSpy")
                .build()

            val query = SEARCH_KEYWORDS.joinToString(" OR ") { "\"$it\"" }
            val messageIds = mutableListOf<Message>()

            var pageToken: String? = null
            var pages = 0
            do {
                val response = gmailService.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(100)
                    .setPageToken(pageToken)
                    .execute()

                response.messages?.let { messageIds.addAll(it) }
                pageToken = response.nextPageToken
                pages++
            } while (pageToken != null && pages < 5)

            val emailDataList = messageIds.take(500).mapNotNull { msg ->
                try {
                    val fullMessage = gmailService.users().messages().get("me", msg.id)
                        .setFormat("full")
                        .execute()
                    parseEmailToData(fullMessage)
                } catch (e: Exception) {
                    null
                }
            }

            groupIntoSubscriptions(emailDataList)
        }

    private fun parseEmailToData(message: Message): EmailData? {
        val headers = message.payload?.headers ?: return null
        val from = headers.find { it.name.equals("From", ignoreCase = true) }?.value ?: return null
        val subject = headers.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
        val dateStr = headers.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""

        val body = extractBody(message)
        val amount = extractAmount(subject + " " + body) ?: return null
        val date = parseDate(dateStr) ?: LocalDate.now()

        return EmailData(
            sender = from,
            subject = subject,
            body = body,
            amount = amount,
            date = date
        )
    }

    private fun extractBody(message: Message): String {
        val parts = message.payload?.parts
        if (parts != null) {
            for (part in parts) {
                if (part.mimeType == "text/plain" && part.body?.data != null) {
                    return String(Base64.getUrlDecoder().decode(part.body.data))
                        .take(2000)
                }
            }
        }
        val bodyData = message.payload?.body?.data
        if (bodyData != null) {
            return String(Base64.getUrlDecoder().decode(bodyData)).take(2000)
        }
        return ""
    }

    private fun extractAmount(text: String): Double? {
        val match = AMOUNT_PATTERN.find(text) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }

    private fun parseDate(dateStr: String): LocalDate? {
        for (formatter in DATE_PATTERNS) {
            try {
                return LocalDate.parse(dateStr.trim().take(20), formatter)
            } catch (_: Exception) { }
        }
        try {
            val cleaned = dateStr.replace(Regex("""\s*\(.*\)\s*"""), "")
                .replace(Regex("""\s+[+-]\d{4}"""), "")
                .trim()
            val rfcFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss")
            return LocalDate.parse(cleaned, rfcFormatter)
        } catch (_: Exception) { }
        return null
    }

    private fun groupIntoSubscriptions(emails: List<EmailData>): List<Subscription> {
        val grouped = emails.groupBy { normalizeSender(it.sender) }

        return grouped.mapNotNull { (senderKey, senderEmails) ->
            if (senderEmails.size < 2) return@mapNotNull null

            val sortedByDate = senderEmails.sortedBy { it.date }
            val mostCommonAmount = senderEmails.groupBy { it.amount }
                .maxByOrNull { it.value.size }?.key ?: return@mapNotNull null

            val frequency = detectFrequency(sortedByDate)
            val serviceName = identifyService(senderKey, senderEmails.first().subject)
            val serviceInfo = KNOWN_SERVICES.entries.firstOrNull {
                senderKey.contains(it.key) || serviceName.lowercase().contains(it.key)
            }

            val lastDate = sortedByDate.last().date
            val nextBilling = calculateNextBilling(lastDate, frequency)
            val isForgotten = ChronoUnit.MONTHS.between(lastDate, LocalDate.now()) >= 6

            Subscription(
                id = senderKey.hashCode().toString(),
                serviceName = serviceInfo?.value?.first ?: serviceName,
                amount = mostCommonAmount,
                frequency = frequency,
                nextBillingDate = nextBilling.toString(),
                firstPaymentDate = sortedByDate.first().date.toString(),
                lastPaymentDate = lastDate.toString(),
                senderEmail = senderEmails.first().sender,
                websiteUrl = serviceInfo?.value?.second ?: "",
                isForgotten = isForgotten,
                isActive = true
            )
        }.sortedByDescending { it.amount }
    }

    private fun normalizeSender(sender: String): String {
        val emailMatch = Regex("""<(.+?)>""").find(sender)
        val email = emailMatch?.groupValues?.get(1) ?: sender
        val domain = email.substringAfter("@").substringBefore(".")
        return domain.lowercase()
    }

    private fun identifyService(senderKey: String, subject: String): String {
        KNOWN_SERVICES[senderKey]?.let { return it.first }
        return senderKey.replaceFirstChar { it.uppercase() }
    }

    private fun detectFrequency(emails: List<EmailData>): BillingFrequency {
        if (emails.size < 2) return BillingFrequency.MONTHLY
        val intervals = emails.zipWithNext().map { (a, b) ->
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

    private data class EmailData(
        val sender: String,
        val subject: String,
        val body: String,
        val amount: Double,
        val date: LocalDate
    )
}
