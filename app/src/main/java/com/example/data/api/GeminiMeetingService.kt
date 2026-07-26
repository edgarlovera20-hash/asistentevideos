package com.example.data.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Turns a network/HTTP exception into a short human-readable detail (HTTP code + Google's own error message, if any) instead of a generic "algo salió mal". */
fun describeError(e: Exception): String = when (e) {
    is HttpException -> {
        val body = e.response()?.errorBody()?.string()?.take(300)
        "HTTP ${e.code()}${if (!body.isNullOrBlank()) " — $body" else ""}"
    }
    else -> e.message ?: e.javaClass.simpleName
}


data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = "user"
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mimeType: String,
    val data: String // base64-encoded
)

data class GeminiGenerationConfig(
    val temperature: Float? = 0.2f,
    val topP: Float? = 0.95f
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-flash-latest:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ponytail: a plain manual loop, not a library — 3 call sites don't justify a retry framework.
suspend fun <T> retryIO(times: Int = 2, initialDelayMs: Long = 500, block: suspend () -> T): T {
    var delayMs = initialDelayMs
    repeat(times) {
        try {
            return block()
        } catch (e: Exception) {
            delay(delayMs)
            delayMs *= 3
        }
    }
    return block() // last attempt: let it throw so the caller's catch handles the fallback
}

const val MAX_TRANSCRIPT_CHARS = 30_000

fun truncateForPrompt(text: String): String {
    if (text.length <= MAX_TRANSCRIPT_CHARS) return text
    return "[transcripción recortada, se usaron los últimos $MAX_TRANSCRIPT_CHARS caracteres]\n" +
        text.takeLast(MAX_TRANSCRIPT_CHARS)
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}

// [apiKeyOverride] lets a runtime-configured key (Ajustes IA) take priority over the one
// baked into the build via .env — without it, changing the key in the app's Settings screen
// would silently do nothing for Gemini specifically, unlike every other provider.
class GeminiMeetingService(private val apiKeyOverride: String? = null) {

    private val apiKey: String
        get() = apiKeyOverride?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY

    val hasValidApiKey: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    suspend fun analyzeMeetingFull(
        transcript: String,
        participants: String,
        meetingTitle: String
    ): MeetingAnalysisResult = withContext(Dispatchers.IO) {
        if (!hasValidApiKey) {
            return@withContext generateFallbackAnalysis(transcript, meetingTitle)
        }

        val prompt = """
            Eres Heavenly AI, la inteligencia artificial empresarial más avanzada para análisis de reuniones.
            Analiza la siguiente reunión llamada "$meetingTitle" realizada por los participantes: $participants.

            TRANSCRIPCIÓN COMPLETA:
            ${truncateForPrompt(transcript)}

            Debes generar un análisis estructurado completo con las siguientes secciones exactas marcadas con encabezados:
            ---RESUMEN EJECUTIVO---
            (Resumen conciso y profesional de los puntos clave tratados)
            
            ---ACUERDOS---
            (Lista numerada de decisiones formales y acuerdos alcanzados)
            
            ---PENDIENTES Y TAREAS---
            (Formato: [Prioridad: Alta/Media/Baja] Tarea | Asignado | Fecha límite)
            
            ---RIESGOS Y TEMAS CRÍTICOS---
            (Factores de riesgo o cuellos de botella detectados)
            
            ---SENTIMIENTO Y CLIMA EMOCIONAL---
            Sentimiento: [Positivo / Neutral / Tenso / Crítico / Optimista]
            Puntaje: [0.0 a 1.0]
            Nivel Emocional: [Alta Energía / Calmado / Colaborativo / Tenso]
            Explicación breve del clima emocional de la sesión.
            
            ---CONCLUSIÓN Y PASOS SIGUIENTES---
            (Conclusión global de la reunión)
        """.trimIndent()

        try {
            val response = retryIO {
                RetrofitClient.api.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.2f)
                    )
                )
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                generateFallbackAnalysis(transcript, meetingTitle)
            } else {
                parseMeetingAnalysisText(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateFallbackAnalysis(transcript, meetingTitle)
        }
    }

    suspend fun chatWithMeeting(
        meetingContext: String,
        userQuestion: String,
        chatHistory: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        if (!hasValidApiKey) {
            return@withContext generateMockChatResponse(userQuestion)
        }

        val historyText = chatHistory.joinToString("\n") { "${it.first}: ${it.second}" }
        val prompt = """
            Eres Heavenly AI Assistant, un asistente corporativo experto en las reuniones de la empresa.
            
            CONTEXTO DE LA REUNIÓN / MEMORIA CORPORATIVA:
            ${truncateForPrompt(meetingContext)}

            HISTORIAL DE CHAT PREVIO:
            $historyText
            
            PREGUNTA DEL USUARIO:
            "$userQuestion"
            
            Responde de manera precisa, profesional, clara y directa en español basándote estricta y detalladamente en el contexto proporcionado.
        """.trimIndent()

        try {
            val response = retryIO {
                RetrofitClient.api.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                    )
                )
            }
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No se obtuvo respuesta de la IA. Por favor intenta nuevamente."
        } catch (e: Exception) {
            e.printStackTrace()
            generateMockChatResponse(userQuestion, describeError(e))
        }
    }

    suspend fun generateExportDocument(
        meetingTitle: String,
        transcript: String,
        analysis: String,
        formatType: String // "WORD", "EXCEL", "POWERPOINT", "PDF"
    ): String = withContext(Dispatchers.IO) {
        if (!hasValidApiKey) {
            return@withContext generateFallbackDocumentFormat(meetingTitle, formatType)
        }

        val prompt = when (formatType) {
            "WORD" -> "Genera el texto completo en formato de Minuta Oficial de Word (.docx) con encabezados formales, tabla de participantes, resumen, decisiones, tareas asignadas y firmas requeridas."
            "EXCEL" -> "Genera el contenido estructurado como tabla de Excel (.xlsx) con columnas: ID | Módulo | Tarea / Pendiente | Responsable | Fecha Límite | Prioridad | Estado | Comentarios."
            "POWERPOINT" -> "Genera la estructura de presentación PowerPoint de 5 diapositivas (.pptx). Diapositiva 1: Título y Objetivos. Diapositiva 2: Resumen de Discusión. Diapositiva 3: Decisiones Clave. Diapositiva 4: Hoja de Ruta y Tareas. Diapositiva 5: Conclusiones y Próximos Pasos."
            else -> "Genera el reporte ejecutivo completo en formato PDF institucional con sello de agua Heavenly AI, introducción, métricas clave, matriz de riesgos y plan de acción."
        } + "\n\nReunión: $meetingTitle\nTranscripción: ${truncateForPrompt(transcript)}\nAnálisis previo: $analysis"

        try {
            val response = retryIO {
                RetrofitClient.api.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                    )
                )
            }
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: generateFallbackDocumentFormat(meetingTitle, formatType)
        } catch (e: Exception) {
            generateFallbackDocumentFormat(meetingTitle, formatType)
        }
    }

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        if (!hasValidApiKey) {
            return@withContext "[Traducción a $targetLanguage]\n" + text.replace("Buenos días", "Good morning / Bonjour / Guten Tag")
        }

        val prompt = "Traduce fielmente el siguiente texto de reunión al idioma: $targetLanguage. Mantén los nombres propios y formato original.\n\n$text"

        try {
            val response = retryIO {
                RetrofitClient.api.generateContent(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
                    )
                )
            }
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: text
        } catch (e: Exception) {
            text
        }
    }

    private fun parseMeetingAnalysisText(rawText: String): MeetingAnalysisResult {
        var summary = ""
        val agreements = mutableListOf<String>()
        val tasks = mutableListOf<ParsedTask>()
        val risks = mutableListOf<String>()
        var sentiment = "Positivo"
        var sentimentScore = 0.85f
        var emotionalLevel = "Colaborativo"
        var conclusion = ""

        val sections = rawText.split("---")
        for (section in sections) {
            val lines = section.trim().lines()
            if (lines.isEmpty()) continue
            val header = lines.first().uppercase()
            val content = lines.drop(1).joinToString("\n").trim()

            when {
                header.contains("RESUMEN") -> summary = content
                header.contains("ACUERDOS") -> {
                    agreements.addAll(content.lines().map { it.replace(Regex("^[0-9]+[.\\-]\\s*"), "").trim() }.filter { it.isNotBlank() })
                }
                header.contains("PENDIENTES") || header.contains("TAREAS") -> {
                    content.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            val parts = line.split("|")
                            if (parts.size >= 2) {
                                tasks.add(
                                    ParsedTask(
                                        title = parts[0].replace(Regex("\\[Prioridad:.*?\\]"), "").trim(),
                                        assignee = parts.getOrNull(1)?.trim() ?: "Sin asignar",
                                        dueDate = parts.getOrNull(2)?.trim() ?: "Próxima semana",
                                        priority = if (line.contains("Alta", true)) "Alta" else if (line.contains("Baja", true)) "Baja" else "Media"
                                    )
                                )
                            } else {
                                tasks.add(ParsedTask(title = line.trim(), assignee = "Equipo", dueDate = "En 5 días", priority = "Media"))
                            }
                        }
                    }
                }
                header.contains("RIESGOS") -> {
                    risks.addAll(content.lines().filter { it.isNotBlank() })
                }
                header.contains("SENTIMIENTO") -> {
                    content.lines().forEach { line ->
                        when {
                            line.contains("Sentimiento:", true) -> sentiment = line.substringAfter(":").trim()
                            line.contains("Puntaje:", true) -> sentimentScore = line.substringAfter(":").trim().toFloatOrNull() ?: 0.85f
                            line.contains("Nivel Emocional:", true) -> emotionalLevel = line.substringAfter(":").trim()
                        }
                    }
                }
                header.contains("CONCLUSIÓN") -> conclusion = content
            }
        }

        if (summary.isBlank()) summary = rawText.take(300) + "..."
        if (agreements.isEmpty()) agreements.add("Aprobación de la hoja de ruta y seguimiento semanal.")

        return MeetingAnalysisResult(
            summary = summary,
            agreements = agreements,
            tasks = tasks,
            risks = risks,
            sentimentLabel = sentiment,
            sentimentScore = sentimentScore,
            emotionalLevel = emotionalLevel,
            conclusion = conclusion
        )
    }

    private fun generateFallbackAnalysis(transcript: String, title: String): MeetingAnalysisResult {
        val tasks = mutableListOf(
            ParsedTask("Enviar propuesta o resumen a las partes involucradas", "Sin asignar", "En 3 días", "Alta"),
            ParsedTask("Revisar términos y documentación pendiente", "Sin asignar", "Próximo Lunes", "Alta"),
            ParsedTask("Coordinar sesión de seguimiento", "Sin asignar", "En 5 días", "Media")
        )
        val agreements = listOf(
            "Se aprueba el calendario de implementación propuesto.",
            "Se acuerda enviar un reporte de seguimiento a los involucrados."
        )
        return MeetingAnalysisResult(
            summary = "No se pudo generar un análisis con Gemini para \"$title\" (sin conexión o sin API key configurada). Este es un resumen de respaldo genérico — revisa la transcripción completa para el detalle real.",
            agreements = agreements,
            tasks = tasks,
            risks = listOf("Análisis de IA no disponible en este momento."),
            sentimentLabel = "Sin analizar",
            sentimentScore = 0f,
            emotionalLevel = "Sin analizar",
            conclusion = "Reintenta el análisis cuando haya conexión a internet y una API key de Gemini configurada."
        )
    }

    private fun generateMockChatResponse(question: String, errorDetail: String? = null): String {
        val detail = errorDetail?.let { " Detalle: $it" } ?: ""
        return "No se pudo conectar con Gemini para responder tu pregunta (sin conexión o sin API key configurada).$detail Revisa tu conexión o la configuración de la API key e intenta de nuevo."
    }

    private fun generateFallbackDocumentFormat(title: String, format: String): String {
        return when (format) {
            "WORD" -> """
                ====================================================
                MINUTA DE REUNIÓN — DOCUMENTO DE RESPALDO
                ====================================================
                Título: $title
                Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
                Estado: No se pudo generar con Gemini (sin conexión o sin API key)

                Este documento es un respaldo genérico. Revisa la transcripción y
                vuelve a intentar la generación cuando haya conexión disponible.
            """.trimIndent()
            "EXCEL" -> """
                ID,Módulo,Tarea/Pendiente,Responsable,Fecha Límite,Prioridad,Estado
                101,General,Documento de respaldo — sin conexión a Gemini,Sin asignar,Pendiente,Media,Pendiente
            """.trimIndent()
            "POWERPOINT" -> """
                [SLIDE 1] TÍTULO: $title - Presentación Ejecutiva
                [SLIDE 2] RESUMEN DE LA REUNIÓN: Puntos destacados de discusión y visión general.
                [SLIDE 3] DECISIONES Y ACUERDOS: 3 acuerdos clave aprobados por la dirección.
                [SLIDE 4] HOJA DE RUTA Y TAREAS: Plan de acción con responsables directos.
                [SLIDE 5] CONCLUSIONES: Próximos pasos e indicadores de éxito.
            """.trimIndent()
            else -> """
                ----------------------------------------------------
                REPORTE EJECUTIVO — DOCUMENTO DE RESPALDO
                ----------------------------------------------------
                No se pudo generar con Gemini (sin conexión o sin API key configurada).
                Revisa la conexión o la configuración de la API key e intenta de nuevo.
            """.trimIndent()
        }
    }

    suspend fun generateVisualAssetsForMeeting(
        meetingId: Long,
        meetingTitle: String,
        transcript: String,
        summary: String
    ): List<com.example.data.db.VisualAssetEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val defaultAssets = mutableListOf<com.example.data.db.VisualAssetEntity>()

        // 1. Resumen Visual / Poster Ejecutivo
        defaultAssets.add(
            com.example.data.db.VisualAssetEntity(
                meetingId = meetingId,
                title = "Poster Ejecutivo & Infografía: $meetingTitle",
                assetType = "EXECUTIVE_POSTER",
                category = "Reuniones",
                description = "Resumen de alto impacto con métricas clave, decisiones estratégicas y clima de la sesión.",
                visualDataJson = """{"nodes":[{"id":"title","label":"$meetingTitle","type":"header"},{"id":"kpi1","label":"Resumen pendiente de generar","type":"stat"}]}""",
                exportFormats = "SVG, PNG, PDF, Canva",
                mcpSource = "Plantilla local",
                modelUsed = "Plantilla",
                timestampMs = now
            )
        )

        // 2. Mapas Mentales
        defaultAssets.add(
            com.example.data.db.VisualAssetEntity(
                meetingId = meetingId,
                title = "Mapa Mental de Temas: $meetingTitle",
                assetType = "MIND_MAP",
                category = "Reuniones",
                description = "Desglose conceptual de acuerdos, roles de participantes y dependencias operativas.",
                visualDataJson = """{"root":"$meetingTitle","children":[{"title":"Temas tratados","nodes":["Pendiente de análisis"]}]}""",
                exportFormats = "SVG, HTML, MindNode, Whimsical",
                mcpSource = "Plantilla local",
                modelUsed = "Plantilla",
                timestampMs = now + 1
            )
        )

        // 3. Arquitectura de Software / Cloud
        defaultAssets.add(
            com.example.data.db.VisualAssetEntity(
                meetingId = meetingId,
                title = "Arquitectura Cloud & Microservicios",
                assetType = "CLOUD_INFRA",
                category = "Arquitectura",
                description = "Diagrama C4 y topología de Google Cloud / AWS acordada en la sesión.",
                visualDataJson = """graph TD\n    A[Mobile App - Jetpack Compose] -->|HTTPS| B[Gemini API]\n    B --> C[Room Local DB]""",
                exportFormats = "Mermaid, PlantUML, SVG, Figma",
                mcpSource = "Plantilla local",
                modelUsed = "Plantilla",
                timestampMs = now + 2
            )
        )

        // 4. Wireframe UI / Dashboard Mockup
        defaultAssets.add(
            com.example.data.db.VisualAssetEntity(
                meetingId = meetingId,
                title = "Wireframe UX: Módulo M3 & Executive Dashboard",
                assetType = "WIREFRAME",
                category = "Producto",
                description = "Prototipo interactivo UI/UX para la plataforma corporativa derivado de los requerimientos.",
                visualDataJson = """{"layout":"Mobile UI","components":[{"type":"HeaderBanner","title":"Visual Intelligence Center"},{"type":"StatsGrid","items":["Diagramas","Mockups","PowerPoints"]},{"type":"VisualCanvas","render":"Interactive SVG"}]}""",
                exportFormats = "Figma, Excalidraw, HTML, PNG",
                mcpSource = "Plantilla local",
                modelUsed = "Plantilla",
                timestampMs = now + 3
            )
        )

        // 5. Flowchart BPMN & Timeline Roadmap
        defaultAssets.add(
            com.example.data.db.VisualAssetEntity(
                meetingId = meetingId,
                title = "Flujo BPMN & Roadmap de Ejecución",
                assetType = "ROADMAP",
                category = "Negocio",
                description = "Diagrama de secuencia de procesos de negocio y hoja de ruta de implementación a 30 días.",
                visualDataJson = """{"phases":[{"name":"Semana 1","tasks":["Pendiente de definir"]}]}""",
                exportFormats = "SVG, Lucidchart, Miro, PDF",
                mcpSource = "Plantilla local",
                modelUsed = "Plantilla",
                timestampMs = now + 4
            )
        )

        defaultAssets
    }
}

data class MeetingAnalysisResult(
    val summary: String,
    val agreements: List<String>,
    val tasks: List<ParsedTask>,
    val risks: List<String>,
    val sentimentLabel: String,
    val sentimentScore: Float,
    val emotionalLevel: String,
    val conclusion: String
)

data class ParsedTask(
    val title: String,
    val assignee: String,
    val dueDate: String,
    val priority: String
)
