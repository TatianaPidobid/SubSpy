package com.subspy.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subspy.app.MainActivity
import com.subspy.app.R
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Central helper for posting local notifications: reminders before an upcoming
 * charge and alerts right after a charge is detected from a bank notification.
 */
object SubSpyNotifier {

    const val REMINDERS_CHANNEL = "subspy_billing_reminders"
    const val CHARGES_CHANNEL = "subspy_charges"

    fun notifyReminder(
        context: Context,
        serviceName: String,
        amount: Double,
        currency: String,
        daysUntil: Int
    ) {
        val money = formatMoney(amount, currency)
        val text = when {
            daysUntil <= 0 -> context.getString(R.string.reminder_text_today, serviceName, money)
            daysUntil == 1 -> context.getString(R.string.reminder_text_tomorrow, serviceName, money)
            else -> context.getString(R.string.reminder_text_days, serviceName, money, daysUntil)
        }
        post(
            context = context,
            channelId = REMINDERS_CHANNEL,
            channelName = context.getString(R.string.reminders_channel_name),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            title = context.getString(R.string.reminder_title),
            text = text,
            id = ("reminder_$serviceName").hashCode()
        )
    }

    fun notifyCharge(
        context: Context,
        merchant: String,
        amount: Double,
        currency: String
    ) {
        val money = formatMoney(amount, currency)
        post(
            context = context,
            channelId = CHARGES_CHANNEL,
            channelName = context.getString(R.string.charges_channel_name),
            importance = NotificationManager.IMPORTANCE_HIGH,
            title = context.getString(R.string.charge_title),
            text = context.getString(R.string.charge_text, money, merchant),
            id = System.currentTimeMillis().toInt()
        )
    }

    private fun post(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int,
        title: String,
        text: String,
        id: Int
    ) {
        ensureChannel(context, channelId, channelName, importance)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, channelName, importance)
                )
            }
        }
    }

    private fun formatMoney(amount: Double, currency: String): String {
        return try {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                setCurrency(Currency.getInstance(currency))
            }.format(amount)
        } catch (e: Exception) {
            "$amount $currency"
        }
    }
}
