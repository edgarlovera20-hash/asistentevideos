package com.example.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AudioRecordingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "heavenly_audio_recording_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "ACTION_START_AUDIO_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_AUDIO_RECORDING"
        const val EXTRA_MEETING_TITLE = "EXTRA_MEETING_TITLE"

        fun startService(context: Context, meetingTitle: String = "Reunión Activa") {
            val intent = Intent(context, AudioRecordingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MEETING_TITLE, meetingTitle)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioRecordingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_MEETING_TITLE) ?: "Reunión Activa"
                val notification = buildNotification(title)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación de Audio en Tiempo Real",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente activa mientras Heavenly AI graba audio de la reunión"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(meetingTitle: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Heavenly AI - Grabando Audio")
            .setContentText("Reunión: $meetingTitle")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
