package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.db.MeetingEntity
import com.example.data.user.UserRole
import com.example.data.user.UserSessionManager
import com.example.ui.MeetingViewModel
import com.example.ui.components.HeaderBanner
import com.example.ui.components.JoinUrlDialog
import com.example.ui.components.QrScannerModal
import com.example.ui.components.SentimentBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MeetingViewModel,
    onStartRecordingClick: () -> Unit,
    onSelectMeeting: (Long) -> Unit,
    onOpenVisualCenter: () -> Unit = {}
) {
    val meetings by viewModel.meetings.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    var showRoleDialog by remember { mutableStateOf(false) }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var showJoinUrlDialog by remember { mutableStateOf(false) }

    val totalMeetings = meetings.size
    val totalSeconds = meetings.sumOf { it.durationSeconds }
    val totalHoursStr = String.format("%.1f hrs", totalSeconds / 3600f)
    val pendingTasksCount = allTasks.count { !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Heavenly AI Meeting",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hola, ${currentUser.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = { showRoleDialog = true },
                shape = RoundedCornerShape(20.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("role_switcher_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Rol",
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentUser.role.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Callout
            item {
                HeaderBanner(
                    title = "Memoria Corporativa Activa",
                    subtitle = "Todas tus reuniones transcritas, resumidas e indexadas en tiempo real con Gemini 2.5 Pro.",
                    roleTag = "${currentUser.provider.label} • AES-256"
                )
            }

            // Hero Callout
            item {
                HeaderBanner(
                    title = "Memoria Corporativa Activa",
                    subtitle = "Todas tus reuniones transcritas, resumidas e indexadas en tiempo real con Gemini 2.5 Pro.",
                    roleTag = "${currentUser.provider.label} • AES-256"
                )
            }

            // Feedback Toast Banner if action performed
            if (!actionFeedback.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyanPrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyanPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionFeedback!!,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyanPrimary
                            )
                        }
                    }
                }
            }

            // Primary Action Buttons Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartRecordingClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_meeting_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Iniciar",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "● INICIAR NUEVA GRABACIÓN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = onOpenVisualCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("visual_intelligence_center_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Visual Center",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎨 VISUAL INTELLIGENCE CENTER (AGENTE #11)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showQrScannerModal = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("scan_qr_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR",
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Escanear QR",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = CyanPrimary
                            )
                        }

                        OutlinedButton(
                            onClick = { showJoinUrlDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("join_link_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IndigoSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Enlace",
                                tint = IndigoSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pegar Enlace",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = IndigoSecondary
                            )
                        }
                    }
                }
            }

            // Quick Platform Connectors Carousel
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Conexión a Plataformas Externas",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Detector Activo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val platformQuickLinks = listOf(
                            Triple("Google Meet", "https://meet.google.com/demo-heavenly-meet", Icons.Default.VideoCameraFront),
                            Triple("Zoom", "https://zoom.us/j/987654321", Icons.Default.Videocam),
                            Triple("Telmex", "https://videoconferencia.telmex.com/j/12345", Icons.Default.CastConnected),
                            Triple("WhatsApp", "https://wa.me/call/heavenly-123", Icons.Default.Call),
                            Triple("Teams", "https://teams.microsoft.com/l/meetup", Icons.Default.Groups),
                            Triple("Llamada Móvil", "tel:+525512345678", Icons.Default.Phone)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(platformQuickLinks) { (platformName, url, icon) ->
                                Surface(
                                    onClick = {
                                        viewModel.joinMeetingFromUrl(url)
                                        onStartRecordingClick()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("quick_platform_${platformName.lowercase().replace(" ", "_")}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = platformName,
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = platformName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stat Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Horas Grabadas",
                            value = totalHoursStr,
                            subtitle = "+2.4h esta semana",
                            icon = Icons.Default.Timer,
                            accentColor = CyanPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Reuniones Total",
                            value = "$totalMeetings",
                            subtitle = "100% analizadas",
                            icon = Icons.Default.Groups,
                            accentColor = IndigoSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Pendientes IA",
                            value = "$pendingTasksCount",
                            subtitle = "Asignados con IA",
                            icon = Icons.Default.Assignment,
                            accentColor = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Productividad",
                            value = "94%",
                            subtitle = "Clima Positivo",
                            icon = Icons.Default.TrendingUp,
                            accentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Meetings Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reuniones Recientes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${meetings.size} Guardadas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(meetings) { meeting ->
                MeetingCardItem(
                    meeting = meeting,
                    onClick = { onSelectMeeting(meeting.id) }
                )
            }
        }
    }

    // Role Switcher Modal
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Cambiar Rol de Usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona un rol para simular permisos de plataforma:", style = MaterialTheme.typography.bodySmall)
                    UserRole.values().forEach { role ->
                        Surface(
                            onClick = {
                                UserSessionManager.switchRole(role)
                                showRoleDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentUser.role == role) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = role.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentUser.role == role) CyanPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Nivel de acceso: ${role.level}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (currentUser.role == role) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // QR Code Scanner Modal
    if (showQrScannerModal) {
        QrScannerModal(
            onDismiss = { showQrScannerModal = false },
            onQrScanned = { url ->
                viewModel.joinMeetingFromUrl(url)
                onStartRecordingClick()
            }
        )
    }

    // Join URL / External Link Dialog
    if (showJoinUrlDialog) {
        JoinUrlDialog(
            onDismiss = { showJoinUrlDialog = false },
            onJoinUrl = { url, title ->
                viewModel.joinMeetingFromUrl(url, title)
                onStartRecordingClick()
            }
        )
    }
}

@Composable
fun MeetingCardItem(
    meeting: MeetingEntity,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meeting_card_${meeting.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IndigoSecondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = meeting.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IndigoSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                SentimentBadge(
                    sentiment = meeting.sentimentLabel,
                    score = meeting.sentimentScore
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = meeting.executiveSummary ?: "Sin resumen generado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${meeting.durationSeconds / 60} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }
            }
        }
    }
}
