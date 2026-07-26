package com.example.data.notifications

import android.Manifest
import android.app.AlarmManager
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
import com.example.MainActivity
import com.example.R
import com.example.data.db.ReminderEntity

/**
 * Posts local reminder notifications and schedules the exact alarms that trigger them.
 * A plain object (not a class) — it's stateless, and gets called from a Worker, a
 * BroadcastReceiver, and the ViewModel, none of which share a natural owner for an instance.
 */
object ReminderNotifier {
    const val CHANNEL_ID = "meeting_reminders"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    // Notify this long before the event starts. Fixed for now — could become a user setting.
    const val REMINDER_LEAD_MS = 15 * 60_000L

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Reuniones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de reuniones próximas detectadas en Calendar o WhatsApp"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /** Schedules an exact alarm to fire [REMINDER_LEAD_MS] before the event, or notifies now if that window already passed. */
    fun scheduleAlarm(context: Context, reminder: ReminderEntity) {
        val fireAtMs = reminder.eventTimeMs - REMINDER_LEAD_MS

        if (fireAtMs <= System.currentTimeMillis()) {
            if (reminder.eventTimeMs > System.currentTimeMillis()) postNotification(context, reminder)
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtMs, pendingIntent)
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM was revoked by the user in system settings — fall back to
            // an inexact alarm rather than silently dropping the reminder.
            alarmManager.set(AlarmManager.RTC_WAKEUP, fireAtMs, pendingIntent)
        }
    }

    fun postNotification(context: Context, reminder: ReminderEntity) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return // user hasn't granted POST_NOTIFICATIONS — nothing we can do here
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, reminder.id.toInt(), openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutesUntil = ((reminder.eventTimeMs - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
        val body = reminder.description.ifBlank { "En $minutesUntil min" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
    }
}
