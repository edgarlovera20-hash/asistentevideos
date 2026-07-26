package com.example.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.GoogleCalendarService
import com.example.data.auth.GoogleAuthManager
import com.example.data.db.AppDatabase
import com.example.data.db.ReminderEntity
import com.example.data.notifications.ReminderNotifier
import java.util.concurrent.TimeUnit

/**
 * Periodic refresh of upcoming Google Calendar events into [ReminderEntity] rows, scheduling
 * a local notification alarm for each new one. 15 minutes is WorkManager's minimum interval
 * for periodic work — there's no way to poll more often than that without a foreground service.
 */
class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "calendar_sync"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val authManager = GoogleAuthManager(applicationContext)
        val accessToken = authManager.getAccessToken()
            ?: authManager.authorizeSilently()?.accessToken
            ?: return Result.success() // not connected yet, or needs fresh consent — try again next cycle

        return try {
            val meetings = GoogleCalendarService().fetchUpcomingMeetings(accessToken)
            val dao = AppDatabase.getDatabase(applicationContext).meetingDao()

            meetings.forEach { meeting ->
                val reminder = ReminderEntity(
                    source = "CALENDAR",
                    title = meeting.title,
                    description = meeting.description,
                    eventTimeMs = meeting.startTimeMs,
                    sourceRefId = meeting.eventId
                )
                val insertedId = dao.upsertReminder(reminder)
                if (insertedId != -1L) {
                    ReminderNotifier.scheduleAlarm(applicationContext, reminder.copy(id = insertedId))
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
