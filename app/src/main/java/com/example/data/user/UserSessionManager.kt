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
    val avatarUrl: String? = null
)

// Mock session state — no real authentication backs this. The persisted, real audit
// trail lives in Room (AuditLogEntity, via MeetingRepository.addAuditLog).
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

    fun switchRole(newRole: UserRole) {
        _currentUser.value = _currentUser.value.copy(role = newRole)
    }

    fun login(name: String, email: String, provider: AuthProvider, role: UserRole = UserRole.USUARIO) {
        _currentUser.value = UserProfile(name = name, email = email, role = role, provider = provider)
    }
}
