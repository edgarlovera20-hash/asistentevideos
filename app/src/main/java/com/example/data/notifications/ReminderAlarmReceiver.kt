package com.example.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fired by the exact alarm ReminderNotifier schedules — posts the notification and marks it done. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderNotifier.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).meetingDao()
                val reminder = dao.getReminderByIdSync(reminderId) ?: return@launch
                if (!reminder.notified) {
                    ReminderNotifier.postNotification(context, reminder)
                    dao.markReminderNotified(reminderId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
