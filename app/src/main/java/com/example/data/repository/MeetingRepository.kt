package com.example.data.repository

import android.content.Context
import com.example.data.api.GeminiMeetingService
import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class MeetingRepository(
    private val dao: MeetingDao,
    private val geminiService: GeminiMeetingService = GeminiMeetingService()
) {
    val allMeetings: Flow<List<MeetingEntity>> = dao.getAllMeetings()
    val allTasks: Flow<List<ActionTaskEntity>> = dao.getAllTasks()

    fun getMeetingById(id: Long): Flow<MeetingEntity?> = dao.getMeetingById(id)
    fun getParticipants(meetingId: Long): Flow<List<ParticipantEntity>> = dao.getParticipants(meetingId)
    fun getTranscript(meetingId: Long): Flow<List<TranscriptSegmentEntity>> = dao.getTranscriptSegments(meetingId)
    fun getTasks(meetingId: Long): Flow<List<ActionTaskEntity>> = dao.getTasksForMeeting(meetingId)
    fun getAgreements(meetingId: Long): Flow<List<AgreementEntity>> = dao.getAgreementsForMeeting(meetingId)
    fun getChatMessages(meetingId: Long): Flow<List<ChatMessageEntity>> = dao.getChatMessages(meetingId)
    fun searchMeetings(query: String): Flow<List<MeetingEntity>> = dao.searchMeetings(query)

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
        dao.deleteMeetingById(meetingId)
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

    suspend fun analyzeMeetingWithAI(meetingId: Long) = withContext(Dispatchers.IO) {
        val meeting = dao.getMeetingByIdSync(meetingId) ?: return@withContext
        val segments = dao.getTranscriptSegmentsSync(meetingId)
        val fullTranscript = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
        val participantsList = dao.getParticipants(meetingId).firstOrNull()?.joinToString(", ") { it.name } ?: "Participantes generales"

        val analysis = geminiService.analyzeMeetingFull(fullTranscript, participantsList, meeting.title)

        val updatedMeeting = meeting.copy(
            status = "COMPLETADO",
            executiveSummary = analysis.summary,
            conclusions = analysis.conclusion,
            sentimentLabel = analysis.sentimentLabel,
            sentimentScore = analysis.sentimentScore,
            emotionalLevel = analysis.emotionalLevel
        )
        dao.updateMeeting(updatedMeeting)

        // Insert agreements
        val agreementEntities = analysis.agreements.map { text ->
            AgreementEntity(meetingId = meetingId, agreementText = text, category = meeting.category)
        }
        dao.insertAgreements(agreementEntities)

        // Insert tasks
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

        // Add welcome AI chat message
        dao.insertChatMessage(
            ChatMessageEntity(
                meetingId = meetingId,
                sender = "Gemini",
                messageText = "Hola. He procesado la reunión \"${meeting.title}\". Puedes preguntarme cualquier detalle sobre los acuerdos, quién dijo qué, o pedirme resúmenes específicos."
            )
        )
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

        val aiResponse = geminiService.chatWithMeeting(context, userText, chatHistory)

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
        val translated = geminiService.translateText(summary, language)
        dao.updateMeeting(meeting.copy(translatedLanguage = language, executiveSummary = translated))
        translated
    }

    suspend fun generateDocumentFormat(meetingId: Long, formatType: String): String = withContext(Dispatchers.IO) {
        val meeting = dao.getMeetingByIdSync(meetingId) ?: return@withContext ""
        val segments = dao.getTranscriptSegmentsSync(meetingId)
        val transcript = segments.joinToString("\n") { "${it.speakerTag} (${it.speakerName}): ${it.text}" }
        geminiService.generateExportDocument(meeting.title, transcript, meeting.executiveSummary ?: "", formatType)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllMeetings().firstOrNull()
        if (existing.isNullOrEmpty()) {
            // Seed Meeting 1: Estrategia Comercial Telmex & Ventas
            val m1Id = dao.insertMeeting(
                MeetingEntity(
                    title = "Estrategia Comercial Telmex & Ventas",
                    dateTimestamp = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                    durationSeconds = 2450,
                    location = "Sala Executive & Google Meet",
                    category = "Ventas",
                    status = "COMPLETADO",
                    executiveSummary = "En esta reunión clave con la dirección de ventas, Edgar Gómez expuso los avances en la renovación del contrato estratégico con Telmex. Génesis propuso integrar el módulo de selección de personal y automatizaciones. Se acordaron metas de cierre mensual y revisiones legales.",
                    conclusions = "Acuerdo total sobre los términos de propuesta y envío de documentación formal este viernes.",
                    sentimentLabel = "Positivo",
                    sentimentScore = 0.92f,
                    emotionalLevel = "Alta Energía",
                    translatedLanguage = "Español"
                )
            )

            dao.insertParticipants(
                listOf(
                    ParticipantEntity(meetingId = m1Id, name = "Juan Perez", role = "Director Comercial", email = "juan@heavenly.ai"),
                    ParticipantEntity(meetingId = m1Id, name = "Edgar Gomez", role = "Supervisor Técnico", email = "edgar@heavenly.ai"),
                    ParticipantEntity(meetingId = m1Id, name = "Génesis Rivas", role = "Lead RRHH & Operaciones", email = "genesis@heavenly.ai")
                )
            )

            dao.insertTranscriptSegments(
                listOf(
                    TranscriptSegmentEntity(meetingId = m1Id, speakerName = "Juan Perez", speakerTag = "Persona 1", text = "Buenos días equipo. Iniciamos la revisión sobre el contrato Telmex y metas de ventas.", timestampMs = 0),
                    TranscriptSegmentEntity(meetingId = m1Id, speakerName = "Edgar Gomez", speakerTag = "Persona 2", text = "Perfecto Juan. Llegué 18 minutos después debido al tráfico, pero ya revisé la propuesta técnica.", timestampMs = 15000),
                    TranscriptSegmentEntity(meetingId = m1Id, speakerName = "Edgar Gomez", speakerTag = "Persona 2", text = "Los números con Telmex muestran un incremento proyectado del 24% en ingresos si cerramos la fase 2 este mes.", timestampMs = 35000),
                    TranscriptSegmentEntity(meetingId = m1Id, speakerName = "Génesis Rivas", speakerTag = "Persona 3", text = "Sugeriría incorporar la automatización de reclutamiento para garantizar el personal necesario en el servicio.", timestampMs = 55000),
                    TranscriptSegmentEntity(meetingId = m1Id, speakerName = "Juan Perez", speakerTag = "Persona 1", text = "Excelente propuesta Génesis. Queda aprobado. Edgar, por favor envía la minuta ajustada el Lunes.", timestampMs = 75000)
                )
            )

            dao.insertAgreements(
                listOf(
                    AgreementEntity(meetingId = m1Id, agreementText = "Aprobar la propuesta comercial ajustada para Telmex con +24% de volumen.", category = "Ventas"),
                    AgreementEntity(meetingId = m1Id, agreementText = "Integrar el módulo de reclutamiento de Heavenly Dreams en la arquitectura.", category = "Operaciones")
                )
            )

            dao.insertTasks(
                listOf(
                    ActionTaskEntity(meetingId = m1Id, title = "Enviar versión final de propuesta a cliente Telmex", assignee = "Juan Perez", dueDate = "Este Viernes", priority = "Alta", isCompleted = false),
                    ActionTaskEntity(meetingId = m1Id, title = "Revisión de términos legales y anexos técnicos", assignee = "Edgar Gomez", dueDate = "Próximo Lunes", priority = "Alta", isCompleted = false),
                    ActionTaskEntity(meetingId = m1Id, title = "Elaborar expediente de personal y requisición de RRHH", assignee = "Génesis Rivas", dueDate = "En 4 días", priority = "Media", isCompleted = true)
                )
            )

            dao.insertChatMessage(
                ChatMessageEntity(meetingId = m1Id, sender = "Gemini", messageText = "Hola. Soy Heavenly AI. He analizado la reunión con Edgar y Génesis. ¿En qué puedo ayudarte?")
            )

            // Seed Meeting 2: Planificación de Infraestructura & Cloud
            val m2Id = dao.insertMeeting(
                MeetingEntity(
                    title = "Planificación de Infraestructura & Cloud",
                    dateTimestamp = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
                    durationSeconds = 1800,
                    location = "Microsoft Teams",
                    category = "Ingeniería",
                    status = "COMPLETADO",
                    executiveSummary = "Revisión del consumo de Firebase Cloud Functions y Vertex AI Vector Search para la memoria empresarial corporativa. Se afinaron parámetros de cifrado AES-256.",
                    conclusions = "Mantenimiento programado sin interrupción del servicio.",
                    sentimentLabel = "Optimista",
                    sentimentScore = 0.85f,
                    emotionalLevel = "Calmado",
                    translatedLanguage = "Español"
                )
            )

            dao.insertParticipants(
                listOf(
                    ParticipantEntity(meetingId = m2Id, name = "Edgar Gomez", role = "Supervisor", email = "edgar@heavenly.ai"),
                    ParticipantEntity(meetingId = m2Id, name = "Carlos Ruiz", role = "DevOps Lead", email = "carlos@heavenly.ai")
                )
            )

            dao.insertTranscriptSegments(
                listOf(
                    TranscriptSegmentEntity(meetingId = m2Id, speakerName = "Edgar Gomez", speakerTag = "Persona 1", text = "Hola Carlos, revisemos la latencia de las consultas semánticas en la memoria empresarial.", timestampMs = 0),
                    TranscriptSegmentEntity(meetingId = m2Id, speakerName = "Carlos Ruiz", speakerTag = "Persona 2", text = "Todo en orden Edgar. Gemini 2.5 Flash responde en menos de 400 milisegundos.", timestampMs = 20000)
                )
            )

            dao.insertAgreements(
                listOf(
                    AgreementEntity(meetingId = m2Id, agreementText = "Mantener política de cifrado AES-256 en repositorios de audio.", category = "Seguridad")
                )
            )

            dao.insertTasks(
                listOf(
                    ActionTaskEntity(meetingId = m2Id, title = "Auditoría de logs de acceso mensual", assignee = "Carlos Ruiz", dueDate = "Fin de mes", priority = "Baja", isCompleted = true)
                )
            )
        }
    }
}
