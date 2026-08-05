package com.subspy.app.notifications

import android.content.Context
import com.subspy.app.data.detection.PaymentEvent
import com.subspy.app.data.detection.SubscriptionSource
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Lightweight persistent buffer for payment events captured from bank push
 * notifications by [BankNotificationListenerService]. Stored in SharedPreferences
 * as JSON so it survives restarts; capped to avoid unbounded growth.
 */
object NotificationCaptureStore {

    private const val PREFS = "subspy_notification_capture"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 1000

    @Synchronized
    fun addEvent(context: Context, merchant: String, amount: Double, currency: String, sourceApp: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(KEY_EVENTS, "[]"))

        val obj = JSONObject().apply {
            put("merchant", merchant)
            put("amount", amount)
            put("currency", currency)
            put("epochDay", LocalDate.now().toEpochDay())
            put("sourceApp", sourceApp)
        }
        array.put(obj)

        // Keep only the most recent MAX_EVENTS entries.
        val trimmed = if (array.length() > MAX_EVENTS) {
            JSONArray().also { out ->
                for (i in (array.length() - MAX_EVENTS) until array.length()) {
                    out.put(array.get(i))
                }
            }
        } else {
            array
        }

        prefs.edit().putString(KEY_EVENTS, trimmed.toString()).apply()
    }

    fun getEvents(context: Context): List<PaymentEvent> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(KEY_EVENTS, "[]"))
        val events = mutableListOf<PaymentEvent>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val merchant = obj.optString("merchant")
            if (merchant.isBlank()) continue
            events.add(
                PaymentEvent(
                    merchantKey = merchant.lowercase(),
                    displayName = merchant.replaceFirstChar { it.uppercase() },
                    amount = obj.optDouble("amount", 0.0),
                    currency = obj.optString("currency", "USD"),
                    date = LocalDate.ofEpochDay(obj.optLong("epochDay", LocalDate.now().toEpochDay())),
                    source = SubscriptionSource.NOTIFICATION,
                    rawSender = obj.optString("sourceApp")
                )
            )
        }
        return events
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_EVENTS).apply()
    }
}
