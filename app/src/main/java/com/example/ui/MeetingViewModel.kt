package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private var recordingTimerJob: Job? = null

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
            _currentMeetingId.value = id
            _isRecording.value = true
            _isPaused.value = false
            _recordingDuration.value = 0
            _audioAmplitudes.value = emptyList()
            _actionFeedback.value = "Conectado exitosamente a ${info.platformName}. Captura de audio en vivo activada."

            UserSessionManager.addAuditLog("Conexión externa activada por Deep Link / QR: ${info.platformName} - URL: $cleanUrl")

            recordingTimerJob?.cancel()
            recordingTimerJob = launch {
                var utteranceIndex = 0
                while (_isRecording.value) {
                    delay(1000)
                    if (!_isPaused.value) {
                        _recordingDuration.value += 1

                        val randomAmp = (0.2f..1.0f).random()
                        val updatedAmps = (_audioAmplitudes.value + randomAmp).takeLast(30)
                        _audioAmplitudes.value = updatedAmps

                        if (_recordingDuration.value % 5 == 0 && utteranceIndex < info.demoUtterances.size) {
                            val (speaker, text) = info.demoUtterances[utteranceIndex]
                            _activeSpeaker.value = speaker
                            val tag = if (speaker.contains(" ")) speaker.substringBefore(" ") else "Hablante"
                            val speakerName = if (speaker.contains("(")) speaker.substringAfter("(").substringBefore(")") else speaker

                            repository.addTranscriptSegment(
                                meetingId = id,
                                speakerName = speakerName,
                                speakerTag = tag,
                                text = text,
                                timestampMs = _recordingDuration.value * 1000L
                            )
                            utteranceIndex = (utteranceIndex + 1) % info.demoUtterances.size
                        }
                    }
                }
            }
        }
    }

    private fun parsePlatformInfo(url: String): PlatformInfo {
        val lower = url.lowercase()
        return when {
            lower.contains("zoom.us") -> PlatformInfo(
                platformName = "Zoom Video Communications",
                category = "Zoom Call",
                defaultTitle = "Conferencia Zoom (${url.takeLast(9)})",
                defaultParticipants = listOf("Anfitrión Zoom", "Edgar Gomez", "Cliente Externo"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Anfitrión Zoom)", "Bienvenidos a la sesión de Zoom. Todos tienen habilitado el micrófono."),
                    Pair("Persona 2 (Edgar Gomez)", "Hola, nos conectamos desde Heavenly AI para transcribir y registrar el acta en tiempo real."),
                    Pair("Persona 3 (Cliente Externo)", "Excelente, comencemos revisando los presupuestos asignados.")
                )
            )
            lower.contains("meet.google.com") -> PlatformInfo(
                platformName = "Google Meet",
                category = "Google Meet",
                defaultTitle = "Sesión Google Meet (${if (url.contains("/")) url.substringAfterLast("/") else url})",
                defaultParticipants = listOf("Edgar Gomez", "Génesis Rivas", "Equipo Google Workspace"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Edgar Gomez)", "Iniciando captura automática en Google Meet."),
                    Pair("Persona 2 (Génesis Rivas)", "La presentación ya está compartida en pantalla."),
                    Pair("Persona 3 (Equipo Google Workspace)", "Confirmado. Recibimos la agenda correctamente.")
                )
            )
            lower.contains("telmex.com") -> PlatformInfo(
                platformName = "Videoconferencia Telmex",
                category = "Telmex Conecta",
                defaultTitle = "Videoconferencia Telmex Empresarial",
                defaultParticipants = listOf("Edgar Gomez", "Ejecutivo Telmex", "Soporte Técnico"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Ejecutivo Telmex)", "Buenas tardes, canal prioritario de Videoconferencia Telmex activo."),
                    Pair("Persona 2 (Edgar Gomez)", "Verificado enlace de red y encriptación AES-256."),
                    Pair("Persona 3 (Soporte Técnico)", "Línea de audio limpia. Procedemos con la auditoría.")
                )
            )
            lower.contains("wa.me") || lower.contains("whatsapp") -> PlatformInfo(
                platformName = "WhatsApp Audio Call",
                category = "Llamada WhatsApp",
                defaultTitle = "Llamada de WhatsApp (${url.takeLast(10)})",
                defaultParticipants = listOf("Edgar Gomez", "Contacto WhatsApp"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Edgar Gomez)", "Hola, grabación de voz iniciada para llamada de WhatsApp."),
                    Pair("Persona 2 (Contacto WhatsApp)", "De acuerdo, te confirmo los datos del pedido por esta llamada.")
                )
            )
            lower.contains("teams.microsoft.com") -> PlatformInfo(
                platformName = "Microsoft Teams",
                category = "MS Teams",
                defaultTitle = "Reunión Microsoft Teams",
                defaultParticipants = listOf("Edgar Gomez", "Gerente de Proyecto", "Analista IT"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Gerente de Proyecto)", "Iniciando reunión de Teams. Revisando backlog de desarrollo."),
                    Pair("Persona 2 (Edgar Gomez)", "Heavenly AI registrando minutas y asignación de tareas.")
                )
            )
            lower.contains("messenger.com") -> PlatformInfo(
                platformName = "Messenger Video Call",
                category = "Messenger",
                defaultTitle = "Llamada de Messenger",
                defaultParticipants = listOf("Edgar Gomez", "Contacto Messenger"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Edgar Gomez)", "Enlace Messenger detectado. Capturando flujo de audio."),
                    Pair("Persona 2 (Contacto Messenger)", "Hola Edgar, coordinamos la cita para el viernes.")
                )
            )
            lower.startsWith("tel:") || lower.contains("llamada") || lower.contains("phone") -> PlatformInfo(
                platformName = "Red Móvil / Llamada Telefónica",
                category = "Llamada Móvil",
                defaultTitle = "Captura de Llamada Móvil (${url.replace("tel:", "")})",
                defaultParticipants = listOf("Edgar Gomez", "Llamante Móvil"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Llamante Móvil)", "Hola Edgar, hablo para dar seguimiento a la propuesta comercial."),
                    Pair("Persona 2 (Edgar Gomez)", "Perfecto, la llamada está siendo transcrita para generar compromisos automáticos.")
                )
            )
            else -> PlatformInfo(
                platformName = "Plataforma Externa",
                category = "Enlace Web",
                defaultTitle = "Reunión Conectada por Enlace ($url)",
                defaultParticipants = listOf("Edgar Gomez", "Participantes Externos"),
                demoUtterances = listOf(
                    Pair("Persona 1 (Edgar Gomez)", "Iniciada sincronización de audio desde enlace externo."),
                    Pair("Persona 2 (Participantes Externos)", "Conexión establecida con Heavenly AI.")
                )
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
            _currentMeetingId.value = id
            _isRecording.value = true
            _isPaused.value = false
            _recordingDuration.value = 0
            _audioAmplitudes.value = emptyList()

            UserSessionManager.addAuditLog("Nueva grabación iniciada: \"$title\"")

            recordingTimerJob?.cancel()
            recordingTimerJob = launch {
                val demoUtterances = listOf(
                    Pair("Persona 1 (Juan Perez)", "Iniciamos la reunión. Vamos a revisar los avances de la semana y pendientes."),
                    Pair("Persona 2 (Edgar Gomez)", "Buenas tardes a todos. Por mi parte la integración con Gemini y Telmex está terminada."),
                    Pair("Persona 3 (Génesis Rivas)", "Excelente Edgar. Desde RRHH ya tenemos listos los expedientes del nuevo personal."),
                    Pair("Persona 1 (Juan Perez)", "Perfecto. Aseguremos los entregables para el cierre del trimestre."),
                    Pair("Persona 2 (Edgar Gomez)", "De acuerdo, enviaré la documentación requerida por correo inmediatamente.")
                )
                var utteranceIndex = 0

                while (_isRecording.value) {
                    delay(1000)
                    if (!_isPaused.value) {
                        _recordingDuration.value += 1

                        // Generate smooth waveform wave floats
                        val randomAmp = (0.2f..1.0f).random()
                        val updatedAmps = (_audioAmplitudes.value + randomAmp).takeLast(30)
                        _audioAmplitudes.value = updatedAmps

                        // Add live transcript segment every 6 seconds
                        if (_recordingDuration.value % 6 == 0 && utteranceIndex < demoUtterances.size) {
                            val (speaker, text) = demoUtterances[utteranceIndex]
                            _activeSpeaker.value = speaker
                            val tag = speaker.substringBefore(" ")

                            repository.addTranscriptSegment(
                                meetingId = id,
                                speakerName = speaker.substringAfter("(").substringBefore(")"),
                                speakerTag = tag,
                                text = text,
                                timestampMs = _recordingDuration.value * 1000L
                            )
                            utteranceIndex = (utteranceIndex + 1) % demoUtterances.size
                        }
                    }
                }
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

            val currentMeeting = activeMeeting.value
            if (currentMeeting != null) {
                repository.updateMeeting(currentMeeting.copy(durationSeconds = _recordingDuration.value, status = "PROCESANDO"))
            }

            _isAnalyzingAI.value = true
            UserSessionManager.addAuditLog("Enviando transcripción a Gemini 2.5 Pro para análisis multivariable...")

            repository.analyzeMeetingWithAI(meetingId)

            _isAnalyzingAI.value = false
            UserSessionManager.addAuditLog("Análisis inteligente completado. Resumen, tareas y minutas generadas.")
        }
    }

    fun sendChatMessage(messageText: String) {
        val meetingId = _currentMeetingId.value ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            UserSessionManager.addAuditLog("Pregunta enviada a Gemini Chat sobre la reunión #$meetingId")
            repository.sendChatMessage(meetingId, messageText)
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
            val doc = repository.generateDocumentFormat(meetingId, formatType)
            _generatedDocument.value = doc
            _isAnalyzingAI.value = false
            UserSessionManager.addAuditLog("Documento $formatType generado por Gemini.")
        }
    }

    fun translateMeeting(language: String) {
        val meetingId = _currentMeetingId.value ?: return
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            repository.translateMeeting(meetingId, language)
            _isAnalyzingAI.value = false
            UserSessionManager.addAuditLog("Reunión traducida al idioma: $language")
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
            repository.deleteMeeting(meetingId)
            _currentMeetingId.value = null
            UserSessionManager.addAuditLog("Reunión #$meetingId eliminada con borrado seguro.")
        }
    }
}

data class PlatformInfo(
    val platformName: String,
    val category: String,
    val defaultTitle: String,
    val defaultParticipants: List<String>,
    val demoUtterances: List<Pair<String, String>>
)

private fun ClosedRange<Float>.random() =
    (Math.random() * (endInclusive - start) + start).toFloat()
