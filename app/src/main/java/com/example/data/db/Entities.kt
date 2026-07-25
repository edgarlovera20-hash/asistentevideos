package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val location: String = "Sala Principal / Google Meet",
    val category: String = "General",
    val status: String = "COMPLETADO", // GRABANDO, PROCESANDO, COMPLETADO
    val rawAudioPath: String? = null,
    val audioWaveformData: String? = null, // CSV float values
    val executiveSummary: String? = null,
    val conclusions: String? = null,
    val sentimentLabel: String? = "Positivo",
    val sentimentScore: Float? = 0.85f,
    val emotionalLevel: String? = "Alta Energía",
    val translatedLanguage: String? = "Español"
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val name: String,
    val role: String,
    val email: String = ""
)

@Entity(tableName = "transcript_segments")
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val speakerName: String,
    val speakerTag: String, // e.g. "Persona 1", "Persona 2"
    val text: String,
    val timestampMs: Long
)

@Entity(tableName = "action_tasks")
data class ActionTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val title: String,
    val assignee: String,
    val dueDate: String,
    val priority: String = "Media", // Alta, Media, Baja
    val isCompleted: Boolean = false
)

@Entity(tableName = "agreements")
data class AgreementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val agreementText: String,
    val category: String = "General"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val sender: String, // "Usuario", "Gemini"
    val messageText: String,
    val timestampMs: Long = System.currentTimeMillis()
)
