package com.example.data.api

import com.squareup.moshi.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class CalendarEventsResponse(
    val items: List<CalendarEvent>?
)

data class CalendarEvent(
    val id: String,
    val summary: String?,
    val description: String?,
    val start: CalendarEventTime?
)

data class CalendarEventTime(
    @Json(name = "dateTime") val dateTime: String?,
    // All-day events only have "date" (no time) — we skip those, a reminder needs a time.
    val date: String?
)

interface CalendarApi {
    @GET("calendar/v3/calendars/primary/events")
    suspend fun listUpcomingEvents(
        @Header("Authorization") bearerToken: String,
        @Query("timeMin") timeMin: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): CalendarEventsResponse
}

private object CalendarRetrofitClient {
    private const val BASE_URL = "https://www.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: CalendarApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CalendarApi::class.java)
    }
}

data class UpcomingMeeting(
    val eventId: String,
    val title: String,
    val description: String,
    val startTimeMs: Long
)

class GoogleCalendarService {

    suspend fun fetchUpcomingMeetings(accessToken: String): List<UpcomingMeeting> {
        val response = retryIO(times = 1) {
            CalendarRetrofitClient.api.listUpcomingEvents(
                bearerToken = "Bearer $accessToken",
                timeMin = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )
        }

        return response.items.orEmpty().mapNotNull { event ->
            val dateTime = event.start?.dateTime ?: return@mapNotNull null // skip all-day events
            val startMs = try {
                // Calendar returns RFC3339 with a numeric offset (e.g. "...-06:00"), not the
                // "Z"-only subset Instant.parse accepts.
                OffsetDateTime.parse(dateTime).toInstant().toEpochMilli()
            } catch (e: Exception) {
                return@mapNotNull null
            }
            UpcomingMeeting(
                eventId = event.id,
                title = event.summary ?: "Reunión sin título",
                description = event.description.orEmpty(),
                startTimeMs = startMs
            )
        }
    }
}
