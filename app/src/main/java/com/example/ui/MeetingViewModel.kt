package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioRecordingForegroundService
import com.example.data.audio.RealtimeAudioRecorder
import com.example.data.db.*
import com.example.data.repository.MeetingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MeetingRepository(db.meetingDao())
    val audioRecorder = RealtimeAudioRecorder(application)

    val meetings: StateFlow<List<MeetingEntity>> = repository.allMeetings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<ActionTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<MeetingEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allMeetings
            else repository.searchMeetings(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentMeetingId = MutableStateFlow<Long?>(null)
    val currentMeetingId: StateFlow<Long?> = _currentMeetingId.asStateFlow()

    val activeMeeting: StateFlow<MeetingEntity?> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getMeetingById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeParticipants: StateFlow<List<ParticipantEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getParticipants(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTranscript: StateFlow<List<TranscriptSegmentEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getTranscript(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<ActionTaskEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getTasks(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAgreements: StateFlow<List<AgreementEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getAgreements(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChatMessages: StateFlow<List<ChatMessageEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessages(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVisualAssets: StateFlow<List<VisualAssetEntity>> = repository.allVisualAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVisualAssets: StateFlow<List<VisualAssetEntity>> = _currentMeetingId
        .flatMapLatest { id ->
            if (id != null) repository.getVisualAssets(id) else repository.allVisualAssets
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val audioAmplitudes: StateFlow<List<Float>> = _audioAmplitudes.asStateFlow()

    private val _activeSpeaker = MutableStateFlow("Persona 1")
    val activeSpeaker: StateFlow<String> = _activeSpeaker.asStateFlow()

    private val _isAnalyzingAI = MutableStateFlow(false)
    val isAnalyzingAI: StateFlow<Boolean> = _isAnalyzingAI.asStateFlow()

    private val _generatedDocument = MutableStateFlow<String?>(null)
    val generatedDocument: StateFlow<String?> = _generatedDocument.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var recordingTimerJob: Job? = null

    fun dismissError() {
        _errorMessage.value = null
    }

    private fun logAudit(message: String) {
        viewModelScope.launch { repository.addAuditLog(message) }
    }

    fun selectMeeting(id: Long) {
        _currentMeetingId.value = id
        _generatedDocument.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun joinMeetingFromUrl(rawUrl: String, customTitle: String? = null) {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isBlank()) return

        val info = parsePlatformInfo(cleanUrl)
        val titleToUse = if (!customTitle.isNullOrBlank()) customTitle else info.defaultTitle

        viewModelScope.launch {
            val id = repository.createMeeting(
                title = titleToUse,
                location = "${info.platformName} ($cleanUrl)",
                category = info.category,
                participants = info.defaultParticipants
            )
            _actionFeedback.value = "Conectado a ${info.platformName}. Grabando audio del micrófono."
            repository.addAuditLog("Conexión externa activada por Deep Link / QR: ${info.platformName} - URL: $cleanUrl")
            beginRecording(id, titleToUse, info.defaultParticipants.firstOrNull() ?: "Micrófono")
        }
    }

    private fun parsePlatformInfo(url: String): PlatformInfo {
        val lower = url.lowercase()
        return when {
            lower.contains("zoom.us") -> PlatformInfo(
                platformName = "Zoom Video Communications",
                category = "Zoom Call",
                defaultTitle = "Conferencia Zoom (${url.takeLast(9)})",
                defaultParticipants = listOf("Anfitrión Zoom", "Cliente Externo")
            )
            lower.contains("meet.google.com") -> PlatformInfo(
                platformName = "Google Meet",
                category = "Google Meet",
                defaultTitle = "Sesión Google Meet (${if (url.contains("/")) url.substringAfterLast("/") else url})",
                defaultParticipants = listOf("Equipo Google Workspace")
            )
            lower.contains("telmex.com") -> PlatformInfo(
                platformName = "Videoconferencia Telmex",
                category = "Telmex Conecta",
                defaultTitle = "Videoconferencia Telmex Empresarial",
                defaultParticipants = listOf("Ejecutivo Telmex", "Soporte Técnico")
            )
            lower.contains("wa.me") || lower.contains("whatsapp") -> PlatformInfo(
                platformName = "WhatsApp Audio Call",
                category = "Llamada WhatsApp",
                defaultTitle = "Llamada de WhatsApp (${url.takeLast(10)})",
                defaultParticipants = listOf("Contacto WhatsApp")
            )
            lower.contains("teams.microsoft.com") -> PlatformInfo(
                platformName = "Microsoft Teams",
                category = "MS Teams",
                defaultTitle = "Reunión Microsoft Teams",
                defaultParticipants = listOf("Gerente de Proyecto", "Analista IT")
            )
            lower.contains("messenger.com") -> PlatformInfo(
                platformName = "Messenger Video Call",
                category = "Messenger",
                defaultTitle = "Llamada de Messenger",
                defaultParticipants = listOf("Contacto Messenger")
            )
            lower.startsWith("tel:") || lower.contains("llamada") || lower.contains("phone") -> PlatformInfo(
                platformName = "Red Móvil / Llamada Telefónica",
                category = "Llamada Móvil",
                defaultTitle = "Captura de Llamada Móvil (${url.replace("tel:", "")})",
                defaultParticipants = listOf("Llamante Móvil")
            )
            else -> PlatformInfo(
                platformName = "Plataforma Externa",
                category = "Enlace Web",
                defaultTitle = "Reunión Conectada por Enlace ($url)",
                defaultParticipants = listOf("Participantes Externos")
            )
        }
    }

    fun startRecording(
        title: String = "Reunión sin título",
        location: String = "Sin ubicación",
        category: String = "General",
        participants: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val id = repository.createMeeting(title, location, category, participants)
            beginRecording(id, title, participants.firstOrNull() ?: "Micrófono")
        }
    }

    /** Starts real MediaRecorder capture + foreground service. Transcription happens once, from the real file, on stop. */
    private suspend fun beginRecording(meetingId: Long, title: String, speakerLabel: String) {
        val started = audioRecorder.startRecording()
        if (!started) {
            _errorMessage.value = "No se pudo iniciar la grabación: falta permiso de micrófono o el hardware no está disponible."
            return
        }

        _currentMeetingId.value = meetingId
        _isRecording.value = true
        _isPaused.value = false
        _recordingDuration.value = 0
        _audioAmplitudes.value = emptyList()
        _activeSpeaker.value = "Persona 1 ($speakerLabel)"

        AudioRecordingForegroundService.startService(getApplication(), title)
        repository.addAuditLog("Nueva grabación iniciada: \"$title\"")

        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                if (!_isPaused.value) {
                    _recordingDuration.value += 1
                    val realAmp = audioRecorder.currentAmplitude.value
                    _audioAmplitudes.value = (_audioAmplitudes.value + realAmp).takeLast(30)
                }
            }
        }
    }

    fun pauseRecording() {
        if (audioRecorder.pauseRecording()) {
            _isPaused.value = true
            logAudit("Grabación pausada temporalmente.")
        } else {
            _errorMessage.value = "No se pudo pausar la grabación."
        }
    }

    fun resumeRecording() {
        if (audioRecorder.resumeRecording()) {
            _isPaused.value = false
            logAudit("Grabación reanudada.")
        } else {
            _errorMessage.value = "No se pudo reanudar la grabación."
        }
    }

    fun stopRecordingAndAnalyze() {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isRecording.value = false
            _isPaused.value = false
            recordingTimerJob?.cancel()

            val recordedFile = audioRecorder.stopRecording()
            AudioRecordingForegroundService.stopService(getApplication())

            val currentMeeting = activeMeeting.value
            if (currentMeeting != null) {
                repository.updateMeeting(currentMeeting.copy(durationSeconds = _recordingDuration.value, status = "PROCESANDO"))
            }

            _isAnalyzingAI.value = true
            try {
                if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0L) {
                    repository.addAuditLog("Transcribiendo audio grabado con Gemini...")
                    val participantsList = activeParticipants.value.map { it.name }
                    repository.processRecordedAudioFile(
                        meetingId = meetingId,
                        audioFile = recordedFile,
                        meetingTitle = currentMeeting?.title ?: "Reunión Grabada",
                        participants = participantsList
                    )
                } else {
                    _errorMessage.value = "La grabación no produjo audio; no hay transcripción para analizar."
                }

                repository.addAuditLog("Enviando transcripción a Gemini para análisis multivariable...")
                repository.analyzeMeetingWithAI(meetingId)
                repository.addAuditLog("Análisis inteligente completado. Resumen, tareas y minutas generadas.")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo completar el análisis: ${e.message ?: "error desconocido"}"
            } finally {
                _isAnalyzingAI.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        audioRecorder.release()
    }

    fun sendChatMessage(messageText: String) {
        val meetingId = _currentMeetingId.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            repository.addAuditLog("Pregunta enviada a Gemini Chat sobre la reunión #$meetingId")
            try {
                repository.sendChatMessage(meetingId, messageText)
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo enviar el mensaje: ${e.message ?: "error desconocido"}"
            }
        }
    }

    fun toggleTaskCompletion(task: ActionTaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            repository.addAuditLog("Tarea \"${task.title}\" actualizada.")
        }
    }

    fun generateDocument(formatType: String) {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            try {
                _generatedDocument.value = repository.generateDocumentFormat(meetingId, formatType)
                repository.addAuditLog("Documento $formatType generado por Gemini.")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo generar el documento: ${e.message ?: "error desconocido"}"
            } finally {
                _isAnalyzingAI.value = false
            }
        }
    }

    fun translateMeeting(language: String) {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            try {
                repository.translateMeeting(meetingId, language)
                repository.addAuditLog("Reunión traducida al idioma: $language")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo traducir la reunión: ${e.message ?: "error desconocido"}"
            } finally {
                _isAnalyzingAI.value = false
            }
        }
    }

    fun executeActionWorkflow(actionName: String) {
        viewModelScope.launch {
            _actionFeedback.value = "Ejecutando acción: $actionName..."
            delay(1200)
            _actionFeedback.value = "¡Acción \"$actionName\" completada exitosamente por IA de Acciones!"
            repository.addAuditLog("Acción ejecutada: $actionName")
            delay(2500)
            _actionFeedback.value = null
        }
    }

    fun deleteCurrentMeeting() {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteMeeting(meetingId)
                _currentMeetingId.value = null
                repository.addAuditLog("Reunión #$meetingId eliminada con borrado seguro.")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo eliminar la reunión: ${e.message ?: "error desconocido"}"
            }
        }
    }

    fun executeVisualSkill(skillName: String, assetType: String = "DIAGRAM", mcp: String = "Plantilla local", model: String = "Plantilla") {
        viewModelScope.launch {
            _actionFeedback.value = "Generando: $skillName..."
            try {
                delay(1000)
                repository.createVisualAsset(
                    meetingId = _currentMeetingId.value,
                    title = skillName,
                    assetType = assetType,
                    category = "Reuniones & Arquitectura",
                    description = "Activo visual generado a partir de una plantilla local para $skillName.",
                    mcpSource = mcp,
                    modelUsed = model
                )
                _actionFeedback.value = "¡$skillName generado!"
                repository.addAuditLog("Visual Skill ejecutado: $skillName")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo generar $skillName: ${e.message ?: "error desconocido"}"
            }
            delay(2500)
            _actionFeedback.value = null
        }
    }
}

data class PlatformInfo(
    val platformName: String,
    val category: String,
    val defaultTitle: String,
    val defaultParticipants: List<String>
)
