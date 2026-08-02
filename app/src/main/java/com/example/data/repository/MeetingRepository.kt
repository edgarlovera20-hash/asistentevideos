package com.example.data.repository

import com.example.data.ai.AiTextService
import com.example.data.ai.GeminiTextService
import com.example.data.api.AudioTranscriptionResult
import com.example.data.api.GeminiAudioTranscriptionService
import com.example.data.api.GeminiMeetingService
import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

class MeetingRepository(
    private val dao: MeetingDao,
    private val aiService: AiTextService = GeminiTextService(),
    // Visual asset templates and audio transcription aren't part of AiTextService (see its
    // kdoc) — they stay on the concrete Gemini services regardless of the selected text provider.
    private val geminiService: GeminiMeetingService = GeminiMeetingService(),
    private val transcriptionService: GeminiAudioTranscriptionService = GeminiAudioTranscriptionService()
) {
    val allMeetings: Flow<List<MeetingEntity>> = dao.getAllMeetings()
    val allTasks: Flow<List<ActionTaskEntity>> = dao.getAllTasks()
    val allVisualAssets: Flow<List<VisualAssetEntity>> = dao.getAllVisualAssets()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getAuditLogs()

    suspend fun addAuditLog(message: String) = withContext(Dispatchers.IO) {
        dao.insertAuditLog(AuditLogEntity(message = message))
    }

    fun getMeetingById(id: Long): Flow<MeetingEntity?> = dao.getMeetingById(id)
    fun getMeetingWithDetails(id: Long): Flow<MeetingWithDetails?> = dao.getMeetingWithDetails(id)
    fun getSummaryForMeeting(meetingId: Long): Flow<MeetingSummaryEntity?> = dao.getSummaryForMeeting(meetingId)
    fun getParticipants(meetingId: Long): Flow<List<ParticipantEntity>> = dao.getParticipants(meetingId)
    fun getTranscript(meetingId: Long): Flow<List<TranscriptSegmentEntity>> = dao.getTranscriptSegments(meetingId)
    fun getTasks(meetingId: Long): Flow<List<ActionTaskEntity>> = dao.getTasksForMeeting(meetingId)
    fun getAgreements(meetingId: Long): Flow<List<AgreementEntity>> = dao.getAgreementsForMeeting(meetingId)
    fun getChatMessages(meetingId: Long): Flow<List<ChatMessageEntity>> = dao.getChatMessages(meetingId)
    fun getVisualAssets(meetingId: Long): Flow<List<VisualAssetEntity>> = dao.getVisualAssetsForMeeting(meetingId)
    fun searchMeetings(query: String): Flow<List<MeetingEntity>> = dao.searchMeetings(query)

    suspend fun saveTask(task: ActionTaskEntity): Long = withContext(Dispatchers.IO) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: ActionTaskEntity) = withContext(Dispatchers.IO) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        dao.deleteTaskById(taskId)
    }

    suspend fun createMeeting(
        title: String,
        location: String,
        category: String,
        participants: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val newMeeting = MeetingEntity(
            title = title,
            dateTimestamp = System.currentTimeMillis(),
            durationSeconds = 0,
            location = location,
            category = category,
            status = "GRABANDO"
        )
        val meetingId = dao.insertMeeting(newMeeting)

        val participantEntities = participants.map { name ->
            ParticipantEntity(
                meetingId = meetingId,
                name = name.trim(),
                role = if (name.contains("Edgar", true)) "Supervisor" else if (name.contains("Génesis", true)) "RRHH Lead" else "Participante",
                email = "${name.lowercase().replace(" ", ".")}@heavenly.ai"
            )
        }
        dao.insertParticipants(participantEntities)
        meetingId
    }

    suspend fun updateMeeting(meeting: MeetingEntity) = withContext(Dispatchers.IO) {
        dao.updateMeeting(meeting)
    }

    suspend fun deleteMeeting(meetingId: Long) = withContext(Dispatchers.IO) {
        dao.deleteMeetingCascade(meetingId)
    }

    suspend fun addTranscriptSegment(
        meetingId: Long,
        speakerName: String,
        speakerTag: String,
        text: String,
        timestampMs: Long
    ) = withContext(Dispatchers.IO) {
        dao.insertTranscriptSegment(
            TranscriptSegmentEntity(
                meetingId = meetingId,
                speakerName = speakerName,
                speakerTag = speakerTag,
                text = text,
                timestampMs = timestampMs
            )
        )
    }

    suspend fun processRecordedAudioFile(
        meetingId: Long,
        audioFile: File,
        meetingTitle: String,
        participants: List<String>
    ): AudioTranscriptionResult = withContext(Dispatchers.IO) {
        val result = transcriptionService.transcribeAudioFile(
            audioFile = audioFile,
            mimeType = "audio/mp4",
            meetingTitle = meetingTitle,
            participants = participants
        )

        val segments = result.segments.map { seg ->
            TranscriptSegmentEntity(
                meetingId = meetingId,
                speakerName = seg.speakerName,
                speakerTag = seg.speakerTag,
                text = seg.text,
                timestampMs = seg.timestampMs
            )
        }

        if (segments.isNotEmpty()) {
            dao.insertTranscriptSegments(segments)
        }

        result
    }

    suspend fun analyzeMeetingWithAI(meetingId: Long) = withContext(Dispatchers.IO) {
        val meeting = dao.getMeetingByIdSync(meetingId) ?: return@withContext
        val segments = dao.getTranscriptSegmentsSync(meetingId)
        val fullTranscript = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
        val participantsList = dao.getParticipants(meetingId).firstOrNull()?.joinToString(", ") { it.name } ?: "Participantes generales"

        val analysis = aiService.analyzeMeeting(fullTranscript, participantsList, meeting.title)

        val updatedMeeting = meeting.copy(
            status = "COMPLETADO",
            executiveSummary = analysis.summary,
            conclusions = analysis.conclusion,
            sentimentLabel = analysis.sentimentLabel,
            sentimentScore = analysis.sentimentScore,
            emotionalLevel = analysis.emotionalLevel
        )
        dao.updateMeeting(updatedMeeting)

        dao.insertSummary(
            MeetingSummaryEntity(
                meetingId = meetingId,
                executiveBriefing = analysis.summary,
                keyTakeaways = analysis.agreements.joinToString(" | "),
                decisionLog = analysis.conclusion,
                riskRegister = "Riesgos identificados y monitoreados en la sesión: Nivel de emoción ${analysis.emotionalLevel}",
                nextSteps = analysis.tasks.joinToString("; ") { "${it.title} (${it.assignee})" }
            )
        )

        val agreementEntities = analysis.agreements.map { text ->
            AgreementEntity(meetingId = meetingId, agreementText = text, category = meeting.category)
        }
        dao.insertAgreements(agreementEntities)

        val taskEntities = analysis.tasks.map { parsedTask ->
            ActionTaskEntity(
                meetingId = meetingId,
                title = parsedTask.title,
                assignee = parsedTask.assignee,
                dueDate = parsedTask.dueDate,
                priority = parsedTask.priority,
                isCompleted = false
            )
        }
        dao.insertTasks(taskEntities)

        dao.insertChatMessage(
            ChatMessageEntity(
                meetingId = meetingId,
                sender = "Gemini",
                messageText = "Hola. He procesado la reunión \"${meeting.title}\". Puedes preguntarme cualquier detalle sobre los acuerdos, quién dijo qué, o pedirme resúmenes específicos."
            )
        )

        val visualAssets = geminiService.generateVisualAssetsForMeeting(
            meetingId = meetingId,
            meetingTitle = meeting.title,
            transcript = fullTranscript,
            summary = analysis.summary
        )
        dao.insertVisualAssets(visualAssets)
    }

    suspend fun sendChatMessage(meetingId: Long, userText: String): String = withContext(Dispatchers.IO) {
        dao.insertChatMessage(
            ChatMessageEntity(meetingId = meetingId, sender = "Usuario", messageText = userText)
        )

        val meeting = dao.getMeetingByIdSync(meetingId)
        val segments = dao.getTranscriptSegmentsSync(meetingId)
        val transcript = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
        val context = "Reunión: ${meeting?.title}\nResumen: ${meeting?.executiveSummary}\n\nTranscripción:\n$transcript"

        val previousMessages = dao.getChatMessages(meetingId).firstOrNull() ?: emptyList()
        val chatHistory = previousMessages.takeLast(10).map { Pair(it.sender, it.messageText) }

        val aiResponse = aiService.chat(context, userText, chatHistory)

        dao.insertChatMessage(
            ChatMessageEntity(meetingId = meetingId, sender = "Gemini", messageText = aiResponse)
        )
        aiResponse
    }

    suspend fun toggleTaskCompletion(task: ActionTaskEntity) = withContext(Dispatchers.IO) {
        dao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun translateMeeting(meetingId: Long, language: String): String = withContext(Dispatchers.IO) {
        val meeting = dao.getMeetingByIdSync(meetingId) ?: return@withContext ""
        val summary = meeting.executiveSummary ?: "Sin resumen"
        val translated = aiService.translate(summary, language)
        dao.updateMeeting(meeting.copy(translatedLanguage = language, executiveSummary = translated))
        translated
    }

    suspend fun generateDocumentFormat(meetingId: Long, formatType: String): String = withContext(Dispatchers.IO) {
        val meeting = dao.getMeetingByIdSync(meetingId) ?: return@withContext ""
        val segments = dao.getTranscriptSegmentsSync(meetingId)
        val transcript = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
        aiService.generateDocument(meeting.title, transcript, meeting.executiveSummary ?: "", formatType)
    }

    suspend fun insertGeneratedImageAsset(
        meetingId: Long?,
        title: String,
        description: String,
        imageFilePath: String
    ): Long = withContext(Dispatchers.IO) {
        dao.insertVisualAsset(
            VisualAssetEntity(
                meetingId = meetingId,
                title = title,
                assetType = "AI_IMAGE",
                category = "Reuniones",
                description = description,
                mcpSource = "NVIDIA NIM",
                modelUsed = "FLUX.1-schnell",
                imageFilePath = imageFilePath
            )
        )
    }

    suspend fun createVisualAsset(
        meetingId: Long?,
        title: String,
        assetType: String,
        category: String,
        description: String,
        mcpSource: String = "Plantilla local",
        modelUsed: String = "Plantilla"
    ): Long = withContext(Dispatchers.IO) {
        val asset = VisualAssetEntity(
            meetingId = meetingId,
            title = title,
            assetType = assetType,
            category = category,
            description = description,
            mcpSource = mcpSource,
            modelUsed = modelUsed,
            visualDataJson = """{"generatedBy":"Visual Intelligence Engine","type":"$assetType","skill":"$title"}"""
        )
        dao.insertVisualAsset(asset)
    }
}
