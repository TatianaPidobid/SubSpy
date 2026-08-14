package com.subspy.app.notifications

import android.content.Context

/**
 * Stores which "days before billing" reminders the user has enabled. Kept in
 * SharedPreferences so the background reminder worker can read it without a
 * network call.
 */
object NotificationPrefs {

    /** Reminder offsets (in days before the charge) the user can toggle. */
    val OFFSETS = listOf(1, 3, 5, 10, 14)

    private const val PREFS = "subspy_notif_prefs"
    private const val KEY_PREFIX = "remind_"
    private val DEFAULT_ENABLED = setOf(3)

    fun isEnabled(context: Context, days: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PREFIX + days, days in DEFAULT_ENABLED)
    }

    fun setEnabled(context: Context, days: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREFIX + days, enabled)
            .apply()
    }

    fun enabledOffsets(context: Context): List<Int> =
        OFFSETS.filter { isEnabled(context, it) }

    /**
     * Remembers that a reminder for [billingDate] was already sent, so a worker
     * running twice on the same day does not notify twice. Returns false when the
     * same reminder was sent before.
     */
    fun markReminderSent(
        context: Context,
        subscriptionId: String,
        days: Int,
        billingDate: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "sent_${subscriptionId}_$days"
        if (prefs.getString(key, null) == billingDate) return false
        prefs.edit().putString(key, billingDate).apply()
        return true
    }
}
