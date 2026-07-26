package com.example.data.ai

import com.example.data.api.MeetingAnalysisResult
import com.example.data.api.ParsedTask
import com.example.data.api.truncateForPrompt

/**
 * Shared prompt text + response parsing + offline fallbacks for the non-Gemini providers
 * (Anthropic, OpenAI-compatible). Same structured ---HEADERS--- format Gemini already
 * uses, so one parser works for all of them — no reason to invent a second shape.
 */

fun buildAnalysisPrompt(transcript: String, participants: String, meetingTitle: String): String = """
    Eres un asistente experto en análisis de reuniones empresariales.
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

fun buildChatPrompt(meetingContext: String, userQuestion: String, chatHistory: List<Pair<String, String>>): String {
    val historyText = chatHistory.joinToString("\n") { "${it.first}: ${it.second}" }
    return """
        Eres un asistente corporativo experto en las reuniones de la empresa.

        CONTEXTO DE LA REUNIÓN:
        ${truncateForPrompt(meetingContext)}

        HISTORIAL DE CHAT PREVIO:
        $historyText

        PREGUNTA DEL USUARIO:
        "$userQuestion"

        Responde de manera precisa, profesional, clara y directa en español basándote estricta y detalladamente en el contexto proporcionado.
    """.trimIndent()
}

fun buildDocumentPrompt(meetingTitle: String, transcript: String, analysis: String, formatType: String): String {
    val instruction = when (formatType) {
        "WORD" -> "Genera el texto completo en formato de Minuta Oficial de Word (.docx) con encabezados formales, tabla de participantes, resumen, decisiones, tareas asignadas y firmas requeridas."
        "EXCEL" -> "Genera el contenido estructurado como tabla de Excel (.xlsx) con columnas: ID | Módulo | Tarea / Pendiente | Responsable | Fecha Límite | Prioridad | Estado | Comentarios."
        "POWERPOINT" -> "Genera la estructura de presentación PowerPoint de 5 diapositivas (.pptx). Diapositiva 1: Título y Objetivos. Diapositiva 2: Resumen de Discusión. Diapositiva 3: Decisiones Clave. Diapositiva 4: Hoja de Ruta y Tareas. Diapositiva 5: Conclusiones y Próximos Pasos."
        else -> "Genera el reporte ejecutivo completo en formato PDF institucional con introducción, métricas clave, matriz de riesgos y plan de acción."
    }
    return "$instruction\n\nReunión: $meetingTitle\nTranscripción: ${truncateForPrompt(transcript)}\nAnálisis previo: $analysis"
}

fun buildTranslatePrompt(text: String, targetLanguage: String): String =
    "Traduce fielmente el siguiente texto de reunión al idioma: $targetLanguage. Mantén los nombres propios y formato original.\n\n$text"

fun parseMeetingAnalysisText(rawText: String): MeetingAnalysisResult {
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

fun fallbackAnalysis(providerLabel: String, title: String): MeetingAnalysisResult = MeetingAnalysisResult(
    summary = "No se pudo generar un análisis con $providerLabel para \"$title\" (sin conexión o configuración inválida). Este es un resumen de respaldo genérico — revisa la transcripción completa para el detalle real.",
    agreements = listOf("Se acuerda enviar un reporte de seguimiento a los involucrados."),
    tasks = listOf(ParsedTask("Revisar términos y documentación pendiente", "Sin asignar", "Próximo Lunes", "Alta")),
    risks = listOf("Análisis de IA no disponible en este momento."),
    sentimentLabel = "Sin analizar",
    sentimentScore = 0f,
    emotionalLevel = "Sin analizar",
    conclusion = "Reintenta el análisis cuando haya conexión y la configuración de $providerLabel sea válida."
)

fun fallbackChatResponse(providerLabel: String): String =
    "No se pudo conectar con $providerLabel para responder tu pregunta (sin conexión o configuración inválida). Revisa tu conexión o la configuración del proveedor de IA e intenta de nuevo."

fun fallbackDocument(providerLabel: String, title: String): String = """
    ----------------------------------------------------
    DOCUMENTO DE RESPALDO — $title
    ----------------------------------------------------
    No se pudo generar con $providerLabel (sin conexión o configuración inválida).
    Revisa la conexión o la configuración del proveedor de IA e intenta de nuevo.
""".trimIndent()
