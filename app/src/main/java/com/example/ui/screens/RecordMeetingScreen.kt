package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TranscriptSegmentEntity
import com.example.ui.MeetingViewModel
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.JoinUrlDialog
import com.example.ui.components.QrScannerModal
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMeetingScreen(
    viewModel: MeetingViewModel,
    onRecordingFinished: () -> Unit
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val duration by viewModel.recordingDuration.collectAsState()
    val amplitudes by viewModel.audioAmplitudes.collectAsState()
    val activeSpeaker by viewModel.activeSpeaker.collectAsState()
    val transcript by viewModel.activeTranscript.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingAI.collectAsState()

    var showQrScannerModal by remember { mutableStateOf(false) }
    var showJoinUrlDialog by remember { mutableStateOf(false) }

    var meetingTitle by remember { mutableStateOf("Reunión Estratégica & Ventas") }
    var locationName by remember { mutableStateOf("Sala Ejecutiva A / Google Meet") }
    var categoryName by remember { mutableStateOf("Ventas") }
    var participantsText by remember { mutableStateOf("Edgar Gomez, Juan Perez, Génesis Rivas") }

    var hasMicPermission by remember { mutableStateOf(viewModel.audioRecorder.hasMicrophonePermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            val parts = participantsText.split(",").map { it.trim() }
            viewModel.startRecording(meetingTitle, locationName, categoryName, parts)
        }
    }

    val formattedDuration = remember(duration) {
        val mins = duration / 60
        val secs = duration % 60
        String.format("%02d:%02d", mins, secs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("record_meeting_screen")
    ) {
        Text(
            text = "Grabación & Transcripción en Tiempo Real",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = CodexWhite
        )
        Text(
            text = "Diarización automática de hablantes e IA adaptativa",
            style = MaterialTheme.typography.bodyMedium,
            color = CodexGrayLight
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isRecording && !isAnalyzing) {
            // Setup Form before starting
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = meetingTitle,
                        onValueChange = { meetingTitle = it },
                        label = { Text("Título de la Reunión", color = CodexGrayLight) },
                        modifier = Modifier.fillMaxWidth().testTag("meeting_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CodexWhite,
                            unfocusedTextColor = CodexWhite,
                            focusedBorderColor = CodexWhite,
                            unfocusedBorderColor = CodexBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Categoría", color = CodexGrayLight) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CodexWhite,
                                unfocusedTextColor = CodexWhite,
                                focusedBorderColor = CodexWhite,
                                unfocusedBorderColor = CodexBorder
                            )
                        )
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Ubicación", color = CodexGrayLight) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CodexWhite,
                                unfocusedTextColor = CodexWhite,
                                focusedBorderColor = CodexWhite,
                                unfocusedBorderColor = CodexBorder
                            )
                        )
                    }

                    OutlinedTextField(
                        value = participantsText,
                        onValueChange = { participantsText = it },
                        label = { Text("Participantes (separados por coma)", color = CodexGrayLight) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CodexWhite,
                            unfocusedTextColor = CodexWhite,
                            focusedBorderColor = CodexWhite,
                            unfocusedBorderColor = CodexBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // QR and URL Join options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showQrScannerModal = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CodexDarkSurface,
                                contentColor = CodexWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear QR", style = MaterialTheme.typography.labelMedium, color = CodexWhite)
                        }

                        Button(
                            onClick = { showJoinUrlDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CodexDarkSurface,
                                contentColor = CodexWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pegar Enlace", style = MaterialTheme.typography.labelMedium, color = CodexWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Giant Start Button - White background, black icon/text
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = {
                                if (viewModel.audioRecorder.hasMicrophonePermission()) {
                                    val parts = participantsText.split(",").map { it.trim() }
                                    viewModel.startRecording(meetingTitle, locationName, categoryName, parts)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = CircleShape,
                            color = CodexWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(100.dp)
                                .testTag("huge_start_recording_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Grabar",
                                    tint = CodexBlack,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "INICIAR",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = CodexBlack
                                )
                            }
                        }
                    }
                }
            }
        } else if (isAnalyzing) {
            // Analyzing AI State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CodexWhite,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Analizando Reunión con Gemini 2.5...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generando resumen, detección de tareas, sentimiento y minutas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CodexGrayLight
                    )
                }
            }
        } else {
            // Active Recording UI
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CodexWhite)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaused) "PAUSADO" else "GRABANDO...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CodexWhite
                            )
                        }

                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = CodexWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Waveform
                    AudioWaveformVisualizer(
                        amplitudes = amplitudes,
                        isRecording = !isPaused
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hablante Actual: $activeSpeaker",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CodexGrayLight
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPaused) {
                            Button(
                                onClick = { viewModel.resumeRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Continuar", tint = CodexBlack)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Continuar", color = CodexBlack, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.pauseRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CodexSurfaceVariant, contentColor = CodexWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = "Pausa", tint = CodexWhite)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pausa", color = CodexWhite)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.stopRecordingAndAnalyze()
                                onRecordingFinished()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("stop_recording_button")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Finalizar", tint = CodexBlack)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Finalizar & Analizar", color = CodexBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Transcripción en Vivo (Diarización)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transcript Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transcript) { segment ->
                    LiveTranscriptBubble(segment = segment)
                }
            }
        }

        // Modals
        if (showQrScannerModal) {
            QrScannerModal(
                onDismiss = { showQrScannerModal = false },
                onQrScanned = { url ->
                    viewModel.joinMeetingFromUrl(url)
                }
            )
        }

        if (showJoinUrlDialog) {
            JoinUrlDialog(
                onDismiss = { showJoinUrlDialog = false },
                onJoinUrl = { url, title ->
                    viewModel.joinMeetingFromUrl(url, title)
                }
            )
        }
    }
}

@Composable
fun LiveTranscriptBubble(segment: TranscriptSegmentEntity) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CodexDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${segment.speakerTag} (${segment.speakerName})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = CodexWhite
                )
                Text(
                    text = "${segment.timestampMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = CodexGrayLight
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = CodexWhite
            )
        }
    }
}
