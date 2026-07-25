package com.example.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole(val label: String, val level: Int) {
    USUARIO("Usuario", 1),
    SUPERVISOR("Supervisor", 2),
    ADMINISTRADOR("Administrador", 3)
}

enum class AuthProvider(val label: String) {
    GOOGLE("Google Workspace"),
    MICROSOFT("Microsoft 365"),
    APPLE("Apple ID"),
    EMAIL("Correo Corporativo")
}

data class UserProfile(
    val name: String,
    val email: String,
    val role: UserRole,
    val provider: AuthProvider,
    val avatarUrl: String? = null,
    val isEncryptedAES256: Boolean = true
)

object UserSessionManager {

    private val _currentUser = MutableStateFlow(
        UserProfile(
            name = "Edgar Gómez",
            email = "edgar.gomez@heavenly.ai",
            role = UserRole.SUPERVISOR,
            provider = AuthProvider.GOOGLE
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _auditLogs = MutableStateFlow(
        listOf(
            "10:15 AM - Edgar Gómez inició sesión vía Google Workspace.",
            "10:18 AM - Transcripción en tiempo real inicializada.",
            "10:22 AM - Análisis Gemini 2.5 Pro procesado y firmado con cifrado AES-256.",
            "09:30 AM - Auditoría de accesos completada sin novedades."
        )
    )
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    fun switchRole(newRole: UserRole) {
        val updated = _currentUser.value.copy(role = newRole)
        _currentUser.value = updated
        addAuditLog("Rol de usuario cambiado a: ${newRole.label}")
    }

    fun login(name: String, email: String, provider: AuthProvider, role: UserRole = UserRole.USUARIO) {
        val updated = UserProfile(
            name = name,
            email = email,
            role = role,
            provider = provider
        )
        _currentUser.value = updated
        addAuditLog("Inicio de sesión exitoso de $name ($email) mediante ${provider.label}")
    }

    fun addAuditLog(action: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLog = "$timestamp - $action"
        _auditLogs.value = listOf(newLog) + _auditLogs.value.take(20)
    }
}
