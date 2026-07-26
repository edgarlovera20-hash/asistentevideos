package com.example.data.ai

import com.example.data.api.MeetingAnalysisResult

/**
 * The 4 text-based AI operations the app needs, implemented once per provider. Audio
 * transcription isn't here — it's multimodal and only Gemini handles it today
 * (GeminiAudioTranscriptionService, unchanged). Visual asset generation isn't here either
 * — those are static local templates now (GeminiMeetingService.generateVisualAssetsForMeeting),
 * not an AI call.
 */
interface AiTextService {
    suspend fun analyzeMeeting(transcript: String, participants: String, meetingTitle: String): MeetingAnalysisResult
    suspend fun chat(meetingContext: String, userQuestion: String, chatHistory: List<Pair<String, String>>): String
    suspend fun generateDocument(meetingTitle: String, transcript: String, analysis: String, formatType: String): String
    suspend fun translate(text: String, targetLanguage: String): String
}
