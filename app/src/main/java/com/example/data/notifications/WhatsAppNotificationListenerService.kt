package com.example.data.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.ai.AiServiceFactory
import com.example.data.db.AppDatabase
import com.example.data.db.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Watches WhatsApp notifications for meeting mentions and turns them into local reminders,
 * reusing the exact same ReminderEntity/ReminderNotifier pipeline CalendarSyncWorker uses.
 *
 * Privacy: this reads the title+text of every WhatsApp notification while active — the user
 * must explicitly enable it from Ajustes > Avisos (it requires a system-level notification
 * access grant, which Android itself gates behind an explicit settings screen, not a runtime
 * permission dialog). A cheap keyword pre-filter runs before any message is sent to the AI
 * provider, so most day-to-day chat never leaves the device.
 */
class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private val WATCHED_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        // ponytail: a plain keyword list, not NLP — good enough to skip the AI call for the
        // vast majority of everyday messages that have nothing to do with meetings.
        private val KEYWORDS = listOf(
            "reunión", "reunion", "junta", "cita", "meet", "zoom", "llamada",
            "nos vemos", "a las", "mañana a", "hoy a", "videollamada"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WATCHED_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (text.isBlank()) return

        val lower = text.lowercase()
        if (KEYWORDS.none { lower.contains(it) }) return

        scope.launch { detectAndSaveReminder(title, text, sbn.postTime) }
    }

    private suspend fun detectAndSaveReminder(sender: String, message: String, postTimeMs: Long) {
        val aiService = AiServiceFactory.create(applicationContext)
        val now = LocalDateTime.now()
        val prompt = """
            Fecha y hora actual: ${now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}
            Mensaje de WhatsApp de "$sender": "$message"

            ¿Este mensaje menciona o confirma una reunión, cita o llamada futura con fecha/hora
            (posiblemente relativa, como "mañana a las 3pm")? Responde ÚNICAMENTE con un JSON
            en una sola línea, sin texto adicional, con esta forma exacta:
            {"isMeeting": true|false, "title": "string corto", "isoDateTime": "YYYY-MM-DDTHH:MM:SS o vacío", "description": "string corto"}
        """.trimIndent()

        val raw = try {
            aiService.chat(meetingContext = "", userQuestion = prompt, chatHistory = emptyList())
        } catch (e: Exception) {
            return
        }

        val json = try {
            JSONObject(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1))
        } catch (e: Exception) {
            return
        }

        if (!json.optBoolean("isMeeting", false)) return
        val isoDateTime = json.optString("isoDateTime").ifBlank { return }

        val eventTimeMs = try {
            LocalDateTime.parse(isoDateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            return
        }
        if (eventTimeMs <= System.currentTimeMillis()) return

        val reminder = ReminderEntity(
            source = "WHATSAPP",
            title = json.optString("title").ifBlank { "Reunión con $sender" },
            description = json.optString("description").ifBlank { message },
            eventTimeMs = eventTimeMs,
            sourceRefId = hashMessage(sender, message, postTimeMs)
        )

        val dao = AppDatabase.getDatabase(applicationContext).meetingDao()
        val insertedId = dao.upsertReminder(reminder)
        if (insertedId != -1L) {
            ReminderNotifier.scheduleAlarm(applicationContext, reminder.copy(id = insertedId))
        }
    }

    private fun hashMessage(sender: String, message: String, postTimeMs: Long): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$sender|$message|$postTimeMs".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
