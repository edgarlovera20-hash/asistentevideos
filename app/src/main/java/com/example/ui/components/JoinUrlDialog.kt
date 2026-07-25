package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.IndigoSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinUrlDialog(
    onDismiss: () -> Unit,
    onJoinUrl: (url: String, title: String?) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var meetingTitle by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Google Meet") }

    val platformTemplates = listOf(
        Triple("Google Meet", "https://meet.google.com/abc-defg-hij", "Google Meet"),
        Triple("Zoom", "https://zoom.us/j/123456789", "Zoom Meeting"),
        Triple("Telmex", "https://videoconferencia.telmex.com/j/555123", "Videoconferencia Telmex"),
        Triple("WhatsApp", "https://wa.me/call/987654321", "Llamada WhatsApp"),
        Triple("MS Teams", "https://teams.microsoft.com/l/meetup-join/123", "Reunión Teams"),
        Triple("Llamada Móvil", "tel:+525512345678", "Llamada Telefónica")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("join_url_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unirse por Enlace o Llamada",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Text(
                    text = "Plataformas Compatibles:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(platformTemplates) { (name, sampleUrl, defaultName) ->
                        val isSelected = selectedPlatform == name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPlatform = name
                                urlText = sampleUrl
                                if (meetingTitle.isBlank()) {
                                    meetingTitle = defaultName
                                }
                            },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.25f),
                                selectedLabelColor = CyanPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("URL o Enlace de la Reunión / Teléfono") },
                    placeholder = { Text("ej. https://meet.google.com/... o tel:+52...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_meeting_url"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = CyanPrimary)
                    }
                )

                OutlinedTextField(
                    value = meetingTitle,
                    onValueChange = { meetingTitle = it },
                    label = { Text("Título Personalizado (Opcional)") },
                    placeholder = { Text("ej. Sesión con Cliente Estratégico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (urlText.isNotBlank()) {
                                onJoinUrl(urlText, meetingTitle.ifBlank { null })
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = urlText.isNotBlank(),
                        modifier = Modifier.testTag("submit_join_url_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conectar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
