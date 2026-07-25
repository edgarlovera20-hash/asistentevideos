package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.data.db.ActionTaskEntity
import com.example.ui.MeetingViewModel
import com.example.ui.components.SentimentBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    viewModel: MeetingViewModel,
    onOpenChatClick: () -> Unit
) {
    val meeting by viewModel.activeMeeting.collectAsState()
    val participants by viewModel.activeParticipants.collectAsState()
    val tasks by viewModel.activeTasks.collectAsState()
    val agreements by viewModel.activeAgreements.collectAsState()
    val generatedDoc by viewModel.generatedDocument.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingAI.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Resumen & IA, 1: Acuerdos & Tareas, 2: Exportar Doc, 3: Traducir & Acciones
    var showDocModal by remember { mutableStateOf(false) }

    if (meeting == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Selecciona una reunión para ver sus detalles.")
        }
        return
    }

    val currentMeeting = meeting!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("meeting_detail_screen")
    ) {
        // Feedback Bar
        AnimatedVisibility(visible = actionFeedback != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = actionFeedback ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentMeeting.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${currentMeeting.location} • ${currentMeeting.durationSeconds / 60} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SentimentBadge(
                sentiment = currentMeeting.sentimentLabel,
                score = currentMeeting.sentimentScore
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open Meeting Chat Button (Fase 6 link)
        Button(
            onClick = onOpenChatClick,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoSecondary, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_meeting_chat_button")
        ) {
            Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat")
            Spacer(modifier = Modifier.width(8.dp))
            Text("💬 Conversar con esta Reunión (IA Chat)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tab Row
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = CyanPrimary
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Resumen", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Tareas (${tasks.size})", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Generador", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("Acciones IA", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else {
            when (activeTab) {
                0 -> ExecutiveSummaryTab(currentMeeting, participants)
                1 -> TasksAndAgreementsTab(tasks, agreements, onToggleTask = { viewModel.toggleTaskCompletion(it) })
                2 -> DocumentGeneratorTab(
                    onGenerate = { format ->
                        viewModel.generateDocument(format)
                        showDocModal = true
                    }
                )
                3 -> ActionsAndTranslationTab(
                    onTranslate = { lang -> viewModel.translateMeeting(lang) },
                    onAction = { action -> viewModel.executeActionWorkflow(action) }
                )
            }
        }
    }

    // Modal to view generated Word, Excel, PowerPoint, PDF content
    if (showDocModal && generatedDoc != null) {
        AlertDialog(
            onDismissRequest = { showDocModal = false },
            title = { Text("Documento Generado por IA") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(
                                text = generatedDoc ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDocModal = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun ExecutiveSummaryTab(
    meeting: com.example.data.db.MeetingEntity,
    participants: List<com.example.data.db.ParticipantEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📝 Resumen Ejecutivo Gemini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = meeting.executiveSummary ?: "Sin resumen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 Conclusión & Estado Emocional",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = meeting.conclusions ?: "Sin conclusión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nivel Emocional: ${meeting.emotionalLevel ?: "Normal"}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = VioletAccent
                    )
                }
            }
        }

        item {
            Text(
                text = "Participantes (${participants.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(participants) { participant ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = participant.name.take(1),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${participant.role} • ${participant.email}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TasksAndAgreementsTab(
    tasks: List<ActionTaskEntity>,
    agreements: List<com.example.data.db.AgreementEntity>,
    onToggleTask: (ActionTaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📌 Acuerdos de la Reunión",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldSuccess
            )
        }

        items(agreements) { agreement ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSuccess.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = agreement.agreementText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⏳ Tareas y Pendientes Detectados por IA",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AmberWarning
            )
        }

        items(tasks) { task ->
            Surface(
                onClick = { onToggleTask(task) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("task_item_${task.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Responsable: ${task.assignee} • Límite: ${task.dueDate} • Prioridad: ${task.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentGeneratorTab(
    onGenerate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📄 Generador de Documentos Corporativos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Generación automática con IA formateada para suites de oficina:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onGenerate("WORD") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B579A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_word_button")
            ) {
                Text("📄 Word (.docx)")
            }
            Button(
                onClick = { onGenerate("EXCEL") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF217346)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_excel_button")
            ) {
                Text("📊 Excel (.xlsx)")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onGenerate("POWERPOINT") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_ppt_button")
            ) {
                Text("📈 PowerPoint")
            }
            Button(
                onClick = { onGenerate("PDF") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9381E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_pdf_button")
            ) {
                Text("📋 PDF Reporte")
            }
        }
    }
}

@Composable
fun ActionsAndTranslationTab(
    onTranslate: (String) -> Unit,
    onAction: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "🌎 Traducción Multilingüe (Fase 9)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CyanPrimary
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Español", "Inglés", "Francés", "Alemán", "Japonés", "Portugués")) { lang ->
                    Button(
                        onClick = { onTranslate(lang) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = CyanPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(lang)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "⚡ IA de Acciones Automáticas (Fase 10)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VioletAccent
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair("Enviar correo resumen vía Gmail", Icons.Default.Email),
                    Pair("Crear eventos en Google Calendar", Icons.Default.CalendarToday),
                    Pair("Enviar reporte rápido a WhatsApp Business", Icons.Default.Send),
                    Pair("Crear tareas en Gestor de Proyectos", Icons.Default.TaskAlt),
                    Pair("Generar minuta firmada en Drive", Icons.Default.CloudUpload)
                ).forEach { (actionTitle, icon) ->
                    Surface(
                        onClick = { onAction(actionTitle) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = VioletAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = actionTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
