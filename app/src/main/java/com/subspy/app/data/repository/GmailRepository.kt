package com.subspy.app.data.repository

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.subspy.app.data.detection.PaymentEvent
import com.subspy.app.data.detection.SubscriptionDetector
import com.subspy.app.data.detection.SubscriptionSource
import com.subspy.app.data.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

        private val DATE_PATTERNS = listOf(
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d MMM yyyy")
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

            val events = messageIds.take(500).mapNotNull { msg ->
                try {
                    val fullMessage = gmailService.users().messages().get("me", msg.id)
                        .setFormat("full")
                        .execute()
                    parseEmailToEvent(fullMessage)
                } catch (e: Exception) {
                    null
                }
            }

            SubscriptionDetector.detectSubscriptions(events)
        }

    private fun parseEmailToEvent(message: Message): PaymentEvent? {
        val headers = message.payload?.headers ?: return null
        val from = headers.find { it.name.equals("From", ignoreCase = true) }?.value ?: return null
        val subject = headers.find { it.name.equals("Subject", ignoreCase = true) }?.value ?: ""
        val dateStr = headers.find { it.name.equals("Date", ignoreCase = true) }?.value ?: ""

        val body = extractBody(message)
        val (amount, currency) = SubscriptionDetector.extractAmountAndCurrency(subject + " " + body)
            ?: return null
        val date = parseDate(dateStr) ?: LocalDate.now()
        val merchantKey = normalizeSender(from)

        return PaymentEvent(
            merchantKey = merchantKey,
            displayName = merchantKey.replaceFirstChar { it.uppercase() },
            amount = amount,
            currency = currency,
            date = date,
            source = SubscriptionSource.GMAIL,
            rawSender = from
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

    private fun normalizeSender(sender: String): String {
        val emailMatch = Regex("""<(.+?)>""").find(sender)
        val email = emailMatch?.groupValues?.get(1) ?: sender
        val domain = email.substringAfter("@").substringBefore(".")
        return domain.lowercase()
    }
}
