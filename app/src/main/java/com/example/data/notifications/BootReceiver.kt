package com.example.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms don't survive a reboot — reschedule every pending reminder on boot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).meetingDao()
                dao.getUnnotifiedReminders().forEach { reminder ->
                    ReminderNotifier.scheduleAlarm(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
