package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ActionTaskEntity
import com.example.data.db.AgreementEntity
import com.example.data.db.MeetingEntity
import com.example.ui.MeetingViewModel
import com.example.ui.components.SentimentBadge
import com.example.ui.theme.*

private fun buildShareText(meeting: MeetingEntity, agreements: List<AgreementEntity>, tasks: List<ActionTaskEntity>): String = buildString {
    appendLine(meeting.title)
    appendLine()
    appendLine("Resumen:")
    appendLine(meeting.executiveSummary ?: "Sin resumen")
    if (agreements.isNotEmpty()) {
        appendLine()
        appendLine("Acuerdos:")
        agreements.forEach { appendLine("- ${it.agreementText}") }
    }
    if (tasks.isNotEmpty()) {
        appendLine()
        appendLine("Tareas:")
        tasks.forEach { appendLine("- ${it.title} (${it.assignee}, ${it.dueDate})") }
    }
}

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

    val visualAssets by viewModel.activeVisualAssets.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Resumen & IA, 1: Tareas, 2: Generador, 3: Acciones IA, 4: Visual IA
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
                    color = CodexWhite
                )
                Text(
                    text = "${currentMeeting.location} • ${currentMeeting.durationSeconds / 60} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = CodexGrayLight
                )
            }

            SentimentBadge(
                sentiment = currentMeeting.sentimentLabel,
                score = currentMeeting.sentimentScore
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open Meeting Chat Button (Fase 6 link) - White primary action
        Button(
            onClick = onOpenChatClick,
            colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_meeting_chat_button")
        ) {
            Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat", tint = CodexBlack)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Conversar con esta Reunión (IA Chat)", fontWeight = FontWeight.Bold, color = CodexBlack)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Export row: universal Android share sheet + real Google Drive save
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val shareText = buildShareText(currentMeeting, agreements, tasks)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, currentMeeting.title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir reunión"))
                },
                modifier = Modifier.weight(1f).testTag("share_meeting_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir...", color = CodexWhite)
            }
            OutlinedButton(
                onClick = {
                    val content = buildShareText(currentMeeting, agreements, tasks)
                    viewModel.saveToGoogleDrive(currentMeeting.title, content)
                },
                modifier = Modifier.weight(1f).testTag("save_to_drive_button")
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar en Drive", color = CodexWhite)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tab Row
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
                Text("Resumen", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 0) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Tareas (${tasks.size})", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 1) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Docs", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 2) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("Acciones", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 3) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                Text("Visual IA (${visualAssets.size})", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 4) CodexWhite else CodexGrayLight)
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
                    onAction = { action -> viewModel.executeActionWorkflow(action) },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentMeeting.title)
                            putExtra(Intent.EXTRA_TEXT, buildShareText(currentMeeting, agreements, tasks))
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir reunión"))
                    },
                    onSaveDrive = {
                        viewModel.saveToGoogleDrive(currentMeeting.title, buildShareText(currentMeeting, agreements, tasks))
                    }
                )
                4 -> MeetingVisualAssetsTab(
                    visualAssets = visualAssets,
                    onExecuteSkill = { skill -> viewModel.executeVisualSkill(skill) }
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
                shape = RoundedCornerShape(12.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resumen Ejecutivo Gemini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = meeting.executiveSummary ?: "Sin resumen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CodexWhite
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conclusión & Estado Emocional",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = meeting.conclusions ?: "Sin conclusión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CodexWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nivel Emocional: ${meeting.emotionalLevel ?: "Normal"}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CodexGrayLight
                    )
                }
            }
        }

        item {
            Text(
                text = "Participantes (${participants.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
        }

        items(participants) { participant ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
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
                            .background(CodexBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = participant.name.take(1),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CodexWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CodexWhite
                        )
                        Text(
                            text = "${participant.role} • ${participant.email}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodexGrayLight
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
                text = "Acuerdos de la Reunión",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
        }

        items(agreements) { agreement ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CodexWhite)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = agreement.agreementText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CodexWhite
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tareas y Pendientes Detectados por IA",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
        }

        items(tasks) { task ->
            Surface(
                onClick = { onToggleTask(task) },
                shape = RoundedCornerShape(10.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth().testTag("task_item_${task.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CodexWhite,
                            checkmarkColor = CodexBlack,
                            uncheckedColor = CodexGrayLight
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = CodexWhite
                        )
                        Text(
                            text = "Responsable: ${task.assignee} • Límite: ${task.dueDate} • Prioridad: ${task.priority}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodexGrayLight
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
            text = "Generador de Documentos Corporativos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = CodexWhite
        )
        Text(
            text = "Generación automática con IA formateada para suites de oficina:",
            style = MaterialTheme.typography.bodySmall,
            color = CodexGrayLight
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onGenerate("WORD") },
                colors = ButtonDefaults.buttonColors(containerColor = CodexDarkSurface, contentColor = CodexWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_word_button")
            ) {
                Text("Word (.docx)")
            }
            Button(
                onClick = { onGenerate("EXCEL") },
                colors = ButtonDefaults.buttonColors(containerColor = CodexDarkSurface, contentColor = CodexWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_excel_button")
            ) {
                Text("Excel (.xlsx)")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onGenerate("POWERPOINT") },
                colors = ButtonDefaults.buttonColors(containerColor = CodexDarkSurface, contentColor = CodexWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_ppt_button")
            ) {
                Text("PowerPoint")
            }
            Button(
                onClick = { onGenerate("PDF") },
                colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("generate_pdf_button")
            ) {
                Text("PDF Reporte", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ActionsAndTranslationTab(
    onTranslate: (String) -> Unit,
    onAction: (String) -> Unit,
    onShare: () -> Unit,
    onSaveDrive: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Traducción Multilingüe (Fase 9)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Español", "Inglés", "Francés", "Alemán", "Japonés", "Portugués")) { lang ->
                    Button(
                        onClick = { onTranslate(lang) },
                        colors = ButtonDefaults.buttonColors(containerColor = CodexDarkSurface, contentColor = CodexWhite),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
                    ) {
                        Text(lang)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "IA de Acciones Automáticas (Fase 10)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Enviar por correo (Gmail y más)", Icons.Default.Email, onShare),
                    Triple("Crear eventos en Google Calendar", Icons.Default.CalendarToday, null),
                    Triple("Enviar reporte rápido a WhatsApp Business", Icons.Default.Send, null),
                    Triple("Crear tareas en Gestor de Proyectos", Icons.Default.TaskAlt, null),
                    Triple("Guardar minuta en Drive", Icons.Default.CloudUpload, onSaveDrive)
                ).forEach { (actionTitle, icon, realAction) ->
                    Surface(
                        onClick = { realAction?.invoke() ?: onAction(actionTitle) },
                        shape = RoundedCornerShape(10.dp),
                        color = CodexDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = CodexWhite)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = actionTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CodexWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingVisualAssetsTab(
    visualAssets: List<com.example.data.db.VisualAssetEntity>,
    onExecuteSkill: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Activos Visuales de la Reunión (${visualAssets.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )
            Text(
                text = "Artefactos diagramados automáticamente por el Visual Intelligence Agent:",
                style = MaterialTheme.typography.bodySmall,
                color = CodexGrayLight
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExecuteSkill("GenerateMindMap") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack)
                ) {
                    Text("Mapa Mental", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onExecuteSkill("GenerateArchitectureDiagram") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CodexDarkSurface, contentColor = CodexWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
                ) {
                    Text("Arquitectura C4", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        items(visualAssets) { asset ->
            VisualAssetCard(
                asset = asset,
                onPreview = {},
                onExport = { onExecuteSkill("Exportar ${asset.title}") }
            )
        }
    }
}
