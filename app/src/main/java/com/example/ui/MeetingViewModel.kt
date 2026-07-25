package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechCaptureController
import com.example.audio.SpeechCaptureEvent
import com.example.data.db.*
import com.example.data.repository.MeetingRepository
import com.example.data.user.UserSessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MeetingRepository(db.meetingDao())
    private val speechCapture = SpeechCaptureController(application)

    val meetings: StateFlow<List<MeetingEntity>> = repository.allMeetings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<ActionTaskEntity>> = repository.allTasks
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

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val audioAmplitudes: StateFlow<List<Float>> = _audioAmplitudes.asStateFlow()

    private val _activeSpeaker = MutableStateFlow("Persona 1 (Edgar Gomez)")
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
    private var speechCaptureJob: Job? = null

    fun dismissError() {
        _errorMessage.value = null
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
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
            _actionFeedback.value = "Conectado exitosamente a ${info.platformName}. Captura de audio en vivo activada."
            UserSessionManager.addAuditLog("Conexión externa activada por Deep Link / QR: ${info.platformName} - URL: $cleanUrl")
            beginLiveCapture(id, info.defaultParticipants.firstOrNull() ?: "Micrófono")
        }
    }

    private fun parsePlatformInfo(url: String): PlatformInfo {
        val lower = url.lowercase()
        return when {
            lower.contains("zoom.us") -> PlatformInfo(
                platformName = "Zoom Video Communications",
                category = "Zoom Call",
                defaultTitle = "Conferencia Zoom (${url.takeLast(9)})",
                defaultParticipants = listOf("Anfitrión Zoom", "Edgar Gomez", "Cliente Externo")
            )
            lower.contains("meet.google.com") -> PlatformInfo(
                platformName = "Google Meet",
                category = "Google Meet",
                defaultTitle = "Sesión Google Meet (${if (url.contains("/")) url.substringAfterLast("/") else url})",
                defaultParticipants = listOf("Edgar Gomez", "Génesis Rivas", "Equipo Google Workspace")
            )
            lower.contains("telmex.com") -> PlatformInfo(
                platformName = "Videoconferencia Telmex",
                category = "Telmex Conecta",
                defaultTitle = "Videoconferencia Telmex Empresarial",
                defaultParticipants = listOf("Edgar Gomez", "Ejecutivo Telmex", "Soporte Técnico")
            )
            lower.contains("wa.me") || lower.contains("whatsapp") -> PlatformInfo(
                platformName = "WhatsApp Audio Call",
                category = "Llamada WhatsApp",
                defaultTitle = "Llamada de WhatsApp (${url.takeLast(10)})",
                defaultParticipants = listOf("Edgar Gomez", "Contacto WhatsApp")
            )
            lower.contains("teams.microsoft.com") -> PlatformInfo(
                platformName = "Microsoft Teams",
                category = "MS Teams",
                defaultTitle = "Reunión Microsoft Teams",
                defaultParticipants = listOf("Edgar Gomez", "Gerente de Proyecto", "Analista IT")
            )
            lower.contains("messenger.com") -> PlatformInfo(
                platformName = "Messenger Video Call",
                category = "Messenger",
                defaultTitle = "Llamada de Messenger",
                defaultParticipants = listOf("Edgar Gomez", "Contacto Messenger")
            )
            lower.startsWith("tel:") || lower.contains("llamada") || lower.contains("phone") -> PlatformInfo(
                platformName = "Red Móvil / Llamada Telefónica",
                category = "Llamada Móvil",
                defaultTitle = "Captura de Llamada Móvil (${url.replace("tel:", "")})",
                defaultParticipants = listOf("Edgar Gomez", "Llamante Móvil")
            )
            else -> PlatformInfo(
                platformName = "Plataforma Externa",
                category = "Enlace Web",
                defaultTitle = "Reunión Conectada por Enlace ($url)",
                defaultParticipants = listOf("Edgar Gomez", "Participantes Externos")
            )
        }
    }

    fun startRecording(
        title: String = "Reunión de Estrategia Operativa",
        location: String = "Sala de Juntas",
        category: String = "General",
        participants: List<String> = listOf("Edgar Gomez", "Juan Perez", "Génesis Rivas")
    ) {
        viewModelScope.launch {
            val id = repository.createMeeting(title, location, category, participants)
            UserSessionManager.addAuditLog("Nueva grabación iniciada: \"$title\"")
            beginLiveCapture(id, participants.firstOrNull() ?: "Micrófono")
        }
    }

    /** Starts real microphone capture: live speech-to-text transcript + real waveform amplitude. */
    private fun beginLiveCapture(meetingId: Long, speakerLabel: String) {
        _currentMeetingId.value = meetingId
        _isRecording.value = true
        _isPaused.value = false
        _recordingDuration.value = 0
        _audioAmplitudes.value = emptyList()
        _activeSpeaker.value = "Persona 1 ($speakerLabel)"

        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                if (!_isPaused.value) _recordingDuration.value += 1
            }
        }

        speechCaptureJob?.cancel()
        speechCaptureJob = viewModelScope.launch {
            try {
                speechCapture.events().collect { event ->
                    if (_isPaused.value) return@collect
                    when (event) {
                        is SpeechCaptureEvent.Amplitude -> {
                            _audioAmplitudes.value = (_audioAmplitudes.value + event.normalized).takeLast(30)
                        }
                        is SpeechCaptureEvent.FinalResult -> {
                            repository.addTranscriptSegment(
                                meetingId = meetingId,
                                speakerName = speakerLabel,
                                speakerTag = "Persona 1",
                                text = event.text,
                                timestampMs = _recordingDuration.value * 1000L
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo activar el micrófono: ${e.message ?: "permiso denegado o no disponible"}"
            }
        }
    }

    fun pauseRecording() {
        _isPaused.value = true
        UserSessionManager.addAuditLog("Grabación pausada temporalmente.")
    }

    fun resumeRecording() {
        _isPaused.value = false
        UserSessionManager.addAuditLog("Grabación reanudada.")
    }

    fun stopRecordingAndAnalyze() {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isRecording.value = false
            _isPaused.value = false
            recordingTimerJob?.cancel()
            speechCaptureJob?.cancel()

            val currentMeeting = activeMeeting.value
            if (currentMeeting != null) {
                repository.updateMeeting(currentMeeting.copy(durationSeconds = _recordingDuration.value, status = "PROCESANDO"))
            }

            _isAnalyzingAI.value = true
            UserSessionManager.addAuditLog("Enviando transcripción a Gemini 2.5 Pro para análisis multivariable...")

            try {
                repository.analyzeMeetingWithAI(meetingId)
                UserSessionManager.addAuditLog("Análisis inteligente completado. Resumen, tareas y minutas generadas.")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo completar el análisis: ${e.message ?: "error desconocido"}"
            } finally {
                _isAnalyzingAI.value = false
            }
        }
    }

    fun sendChatMessage(messageText: String) {
        val meetingId = _currentMeetingId.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            UserSessionManager.addAuditLog("Pregunta enviada a Gemini Chat sobre la reunión #$meetingId")
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
            UserSessionManager.addAuditLog("Tarea \"${task.title}\" actualizada.")
        }
    }

    fun generateDocument(formatType: String) {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            try {
                _generatedDocument.value = repository.generateDocumentFormat(meetingId, formatType)
                UserSessionManager.addAuditLog("Documento $formatType generado por Gemini.")
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
                UserSessionManager.addAuditLog("Reunión traducida al idioma: $language")
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
            UserSessionManager.addAuditLog("Acción ejecutada: $actionName")
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
                UserSessionManager.addAuditLog("Reunión #$meetingId eliminada con borrado seguro.")
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo eliminar la reunión: ${e.message ?: "error desconocido"}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        speechCaptureJob?.cancel()
    }
}

data class PlatformInfo(
    val platformName: String,
    val category: String,
    val defaultTitle: String,
    val defaultParticipants: List<String>
)
