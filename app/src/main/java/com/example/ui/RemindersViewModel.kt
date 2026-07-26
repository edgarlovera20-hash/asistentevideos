package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.auth.GoogleAuthManager
import com.example.data.db.AppDatabase
import com.example.data.db.ReminderEntity
import com.example.data.work.CalendarSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).meetingDao()
    val authManager = GoogleAuthManager(application)

    private val _isGoogleConnected = MutableStateFlow(authManager.isConnected)
    val isGoogleConnected: StateFlow<Boolean> = _isGoogleConnected.asStateFlow()

    val upcomingReminders: StateFlow<List<ReminderEntity>> = dao.getUpcomingReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun dismissStatus() {
        _statusMessage.value = null
    }

    /** Called by RemindersScreen after authManager.onAuthorizationResolved(...) has run. */
    fun onGoogleAuthorized() {
        _isGoogleConnected.value = authManager.isConnected
        if (_isGoogleConnected.value) {
            _statusMessage.value = "Google Calendar conectado. Sincronizando..."
            syncNow()
            CalendarSyncWorker.schedulePeriodic(getApplication())
        } else {
            _statusMessage.value = "No se pudo conectar con Google Calendar."
        }
    }

    fun disconnectGoogle() {
        authManager.disconnect()
        _isGoogleConnected.value = false
        CalendarSyncWorker.cancel(getApplication())
        _statusMessage.value = "Google Calendar desconectado."
    }

    /** Triggers an immediate one-off sync instead of waiting for the next 15-min cycle. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
        WorkManager.getInstance(getApplication()).enqueue(request)
    }

    fun dismissReminderManually(reminder: ReminderEntity) {
        viewModelScope.launch {
            dao.markReminderNotified(reminder.id)
        }
    }
}
