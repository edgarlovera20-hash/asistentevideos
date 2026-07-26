package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/**
 * Wraps Google's Authorization API (Identity Services) to get an access token scoped to
 * read-only Calendar access. Separate from sign-in — this is specifically for the API
 * scope grant, which on Android requires a "Web" OAuth client id as the server client id
 * (a quirk of the library, not a real server).
 *
 * ponytail: token is cached in plain SharedPreferences, not EncryptedSharedPreferences —
 * it's a short-lived, narrowly-scoped (calendar.readonly) OAuth access token for a
 * personal-use sideloaded app, not a long-lived credential. Upgrade if this ever needs a
 * stronger threat model.
 */
class GoogleAuthManager(context: Context) {

    companion object {
        const val CALENDAR_READONLY_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
        // drive.file = least privilege: only touches files this app itself creates, not the
        // user's whole Drive.
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

        // Replace with the "Web" OAuth Client ID created in Google Cloud Console (see README).
        const val WEB_CLIENT_ID = "235093422193-f7drutnpnof7fqm28qe029fii0r4kjdn.apps.googleusercontent.com"

        private const val PREFS_NAME = "google_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val authorizationClient = Identity.getAuthorizationClient(appContext)

    val isConnected: Boolean
        get() = getAccessToken() != null

    fun buildAuthorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(CALENDAR_READONLY_SCOPE), Scope(DRIVE_FILE_SCOPE)))
            // requestOfflineAccess is what actually uses WEB_CLIENT_ID — without it the
            // constant just sat there unused. This also gets us a server auth code path
            // instead of relying purely on the ~50min expiry guess in saveResult().
            .requestOfflineAccess(WEB_CLIENT_ID)
            .build()

    /** Launched from the UI (needs an Activity to resolve consent if not already granted). */
    suspend fun authorize(request: AuthorizationRequest): AuthorizationResult {
        val result = authorizationClient.authorize(request).await()
        if (!result.hasResolution()) saveResult(result)
        return result
    }

    /** Called after the consent IntentSender flow finishes successfully. */
    fun onAuthorizationResolved(result: AuthorizationResult) = saveResult(result)

    /**
     * Silent re-authorization for background use (CalendarSyncWorker) — no UI. Returns null
     * if Google requires fresh user interaction (e.g. token was revoked), in which case the
     * worker just skips this sync cycle instead of crashing; the user reconnects from the UI.
     */
    suspend fun authorizeSilently(): AuthorizationResult? = try {
        val result = authorizationClient.authorize(buildAuthorizationRequest()).await()
        if (!result.hasResolution()) {
            saveResult(result)
            result
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt in 1 until System.currentTimeMillis()) return null // expired
        return token
    }

    fun disconnect() {
        prefs.edit().clear().apply()
    }

    private fun saveResult(result: AuthorizationResult) {
        val token = result.accessToken ?: return
        // Google's Authorization API doesn't return an explicit expires_in here; assume a
        // conservative 50 minutes (typical Google access tokens last 60) and re-authorize
        // silently after that instead of risking a call with an expired token.
        val expiresAt = System.currentTimeMillis() + 50 * 60_000
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }
}
