package com.example.data.ai

import com.example.data.api.GeminiMeetingService
import com.example.data.api.MeetingAnalysisResult

/** Thin adapter — delegates to the existing, already-working GeminiMeetingService instead of duplicating its logic. */
class GeminiTextService(private val gemini: GeminiMeetingService = GeminiMeetingService()) : AiTextService {
    override suspend fun analyzeMeeting(transcript: String, participants: String, meetingTitle: String): MeetingAnalysisResult =
        gemini.analyzeMeetingFull(transcript, participants, meetingTitle)

    override suspend fun chat(meetingContext: String, userQuestion: String, chatHistory: List<Pair<String, String>>): String =
        gemini.chatWithMeeting(meetingContext, userQuestion, chatHistory)

    override suspend fun generateDocument(meetingTitle: String, transcript: String, analysis: String, formatType: String): String =
        gemini.generateExportDocument(meetingTitle, transcript, analysis, formatType)

    override suspend fun translate(text: String, targetLanguage: String): String =
        gemini.translateText(text, targetLanguage)
}
