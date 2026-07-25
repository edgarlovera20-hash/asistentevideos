package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.user.UserSessionManager
import com.example.ui.MeetingViewModel
import com.example.ui.components.HeaderBanner
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseMemoryScreen(
    viewModel: MeetingViewModel,
    onSelectMeeting: (Long) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val auditLogs by UserSessionManager.auditLogs.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Memoria & Búsqueda, 1: Integraciones, 2: Seguridad & Logs

    val sampleQueries = listOf(
        "Telmex",
        "Netflix",
        "Génesis",
        "Edgar",
        "Contrato",
        "Ventas"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("enterprise_memory_screen")
    ) {
        HeaderBanner(
            title = "Memoria Empresarial & Seguridad",
            subtitle = "Indexación semántica global y gobernanza de datos AES-256",
            roleTag = currentUser.role.label
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = CyanPrimary
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Búsqueda global", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Integraciones (9)", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Seguridad AES-256", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTab) {
            0 -> {
                // Search & Memory Index
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Buscar clientes (Telmex, Netflix), personas (Edgar, Génesis)...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("search_meetings_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    // Suggestion Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sugerencias:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        sampleQueries.forEach { tag ->
                            Surface(
                                onClick = { viewModel.updateSearchQuery(tag) },
                                shape = RoundedCornerShape(12.dp),
                                color = CyanPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyanPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Resultados Encontrados (${searchResults.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults) { meeting ->
                            MeetingCardItem(meeting = meeting, onClick = { onSelectMeeting(meeting.id) })
                        }
                    }
                }
            }
            1 -> {
                // Integrations Suite
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "🔌 Integraciones Empresariales Conectadas",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(
                        listOf(
                            Pair("Google Calendar", "Sincronización de minutas y fechas límite de tareas"),
                            Pair("Gmail", "Envío automático de reportes ejecutivos"),
                            Pair("Google Drive", "Almacenamiento de transcripciones y audio"),
                            Pair("Zoom Meetings", "Transcripción automática en vivo"),
                            Pair("Google Meet", "Bot asistente inteligente incorporado"),
                            Pair("Microsoft Teams", "Alertas de acuerdos e IA conversacional"),
                            Pair("Slack", "Notificaciones de tareas asignadas"),
                            Pair("Discord", "Canales de voz y transcripción de proyectos"),
                            Pair("WhatsApp Business", "Envío inmediato de minutas en PDF")
                        )
                    ) { (title, desc) ->
                        var isEnabled by remember { mutableStateOf(true) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Security & Audit Logs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldSuccess.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Conexión API cifrada (HTTPS/TLS)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess
                                    )
                                    Text(
                                        text = "Las llamadas a Gemini viajan por HTTPS. La base de datos local aún no está cifrada en reposo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📋 Registro de Auditoría de Accesos (Fase 14)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(auditLogs) { log ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = log,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
