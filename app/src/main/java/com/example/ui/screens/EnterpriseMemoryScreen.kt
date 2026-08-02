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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Memoria & Búsqueda, 1: Integraciones, 2: Seguridad & Logs

    val sampleQueries = listOf(
        "Contrato",
        "Ventas",
        "Presupuesto",
        "Seguimiento"
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
            subtitle = "Indexación semántica global y gobernanza de datos",
            roleTag = currentUser.role.label
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = activeTab,
            containerColor = CodexDarkSurface,
            contentColor = CodexWhite,
            indicator = { tabPositions ->
                if (activeTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = CodexWhite
                    )
                }
            }
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Búsqueda global", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = if (activeTab == 0) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Integraciones (9)", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = if (activeTab == 1) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Seguridad", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = if (activeTab == 2) CodexWhite else CodexGrayLight)
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
                        placeholder = { Text("Buscar por cliente, persona, tema...", color = CodexGrayLight) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CodexWhite) },
                        modifier = Modifier.fillMaxWidth().testTag("search_meetings_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CodexWhite,
                            unfocusedTextColor = CodexWhite,
                            focusedBorderColor = CodexWhite,
                            unfocusedBorderColor = CodexBorder
                        )
                    )

                    // Suggestion Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sugerencias:", style = MaterialTheme.typography.labelSmall, color = CodexGrayLight)
                        sampleQueries.forEach { tag ->
                            Surface(
                                onClick = { viewModel.updateSearchQuery(tag) },
                                shape = RoundedCornerShape(8.dp),
                                color = CodexBorder
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = CodexWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Resultados Encontrados (${searchResults.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite
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
                // Integrations Suite — only Calendar/Drive are real (see Avisos tab + botones de
                // Compartir/Drive en el detalle de reunión); the rest are not implemented and are
                // shown disabled rather than as live toggles that would silently do nothing.
                val isGoogleConnected = viewModel.authManager.isConnected
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Integraciones",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CodexWhite
                        )
                    }

                    item {
                        IntegrationRow(
                            title = "Google Calendar",
                            desc = "Recordatorios de reuniones próximas. Se conecta desde la pestaña Avisos.",
                            isConnected = isGoogleConnected,
                            statusLabel = if (isGoogleConnected) "Conectado" else "Sin conectar"
                        )
                    }
                    item {
                        IntegrationRow(
                            title = "Google Drive",
                            desc = "Guardar minutas como archivo de texto. Botón \"Guardar en Drive\" en el detalle de la reunión.",
                            isConnected = isGoogleConnected,
                            statusLabel = if (isGoogleConnected) "Conectado" else "Sin conectar"
                        )
                    }
                    item {
                        IntegrationRow(
                            title = "WhatsApp",
                            desc = "Detecta menciones de reuniones en tus chats. Se activa desde la pestaña Avisos.",
                            isConnected = false,
                            statusLabel = "Ver en Avisos"
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Próximamente",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CodexGrayLight
                        )
                    }

                    items(
                        listOf(
                            "Gmail" to "Envío de reportes ejecutivos por correo",
                            "Zoom" to "Unirse y grabar vía enlace de Zoom (compartir manual, sin transcripción en vivo)",
                            "Microsoft Teams" to "Alertas de acuerdos e IA conversacional",
                            "Slack" to "Notificaciones de tareas asignadas",
                            "Discord" to "Canales de voz y transcripción de proyectos"
                        )
                    ) { (title, desc) ->
                        IntegrationRow(title = title, desc = desc, isConnected = false, statusLabel = "No implementado")
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
                            shape = RoundedCornerShape(12.dp),
                            color = CodexDarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Conexión API cifrada (HTTPS/TLS)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CodexWhite
                                    )
                                    Text(
                                        text = "Las llamadas a Gemini viajan por HTTPS. La base de datos local aún no está cifrada en reposo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CodexGrayLight
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Registro de Auditoría de Accesos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CodexWhite
                        )
                    }

                    items(auditLogs) { log ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CodexDarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${
                                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                        .format(java.util.Date(log.timestampMs))
                                } - ${log.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CodexGrayLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrationRow(title: String, desc: String, isConnected: Boolean, statusLabel: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
                Text(text = desc, style = MaterialTheme.typography.labelSmall, color = CodexGrayLight)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isConnected) CodexWhite else CodexBorder
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isConnected) CodexBlack else CodexGrayLight,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
