package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

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

@Entity(tableName = "visual_assets")
data class VisualAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long? = null,
    val title: String,
    val assetType: String, // MIND_MAP, CONCEPT_MAP, ARCHITECTURE, WIREFRAME, MOCKUP, WHITEBOARD, PRESENTATION, INFOGRAPHIC, ROADMAP, BPMN, SEQUENCE, ERD, C4, CLOUD_INFRA, DECISION_TREE, EXECUTIVE_POSTER, KNOWLEDGE_GRAPH
    val category: String = "Reuniones", // Ingeniería, Arquitectura, Producto, Negocio, Reuniones
    val description: String = "",
    val visualDataJson: String = "", // Mermaid, PlantUML, SVG spec, or Node JSON
    val exportFormats: String = "SVG, HTML, PNG, Figma, Canva",
    val mcpSource: String = "Native SVG Engine", // Figma MCP, Canva MCP, Excalidraw MCP, Miro MCP, etc.
    val modelUsed: String = "Gemini Vision", // Gemini Flash, Gemini Pro, Mermaid, PlantUML, Excalidraw, Imagen
    val version: Int = 1,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "meeting_summaries")
data class MeetingSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val meetingId: Long,
    val executiveBriefing: String,
    val keyTakeaways: String, // JSON array or pipe separated
    val decisionLog: String,
    val riskRegister: String,
    val nextSteps: String,
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)

data class MeetingWithDetails(
    @Embedded val meeting: MeetingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "meetingId"
    )
    val participants: List<ParticipantEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "meetingId"
    )
    val transcripts: List<TranscriptSegmentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "meetingId"
    )
    val tasks: List<ActionTaskEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "meetingId"
    )
    val agreements: List<AgreementEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "meetingId"
    )
    val summaries: List<MeetingSummaryEntity>
)

