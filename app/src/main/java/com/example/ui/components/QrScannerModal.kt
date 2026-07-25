package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerModal(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    var detectedUrl by remember { mutableStateOf<String?>(null) }
    var selectedPresetName by remember { mutableStateOf("Google Meet") }

    // Scanner beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Quick sample QRs for testing and demonstration
    val sampleQrs = listOf(
        Pair("Google Meet", "https://meet.google.com/abc-defg-hij"),
        Pair("Zoom Meeting", "https://zoom.us/j/9876543210?pwd=heavenlysecret"),
        Pair("Videoconferencia Telmex", "https://videoconferencia.telmex.com/j/555987123"),
        Pair("Llamada WhatsApp", "https://wa.me/call/heavenly-wa-987"),
        Pair("Microsoft Teams", "https://teams.microsoft.com/l/meetup-join/heavenly-team"),
        Pair("Llamada Móvil", "tel:+525512345678")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("qr_scanner_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CyanPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Escanear QR",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Escáner QR de Reunión",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Apunta al código impreso o de pantalla",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camera Scanner Viewport Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated Camera Grid Pattern
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.dp.toPx()
                        val cornerLen = 32.dp.toPx()
                        val boxSize = size.width.coerceAtMost(size.height) * 0.7f
                        val left = (size.width - boxSize) / 2
                        val top = (size.height - boxSize) / 2
                        val right = left + boxSize
                        val bottom = top + boxSize

                        // Viewfinder background dimmed overlay
                        drawRect(
                            color = Color.Black.copy(alpha = 0.4f),
                            size = size
                        )

                        // Clear bounding box center
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(boxSize, boxSize)
                        )

                        // Draw Corner Brackets (Cyan)
                        val cyan = Color(0xFF00E5FF)
                        // Top-Left
                        drawLine(cyan, Offset(left, top), Offset(left + cornerLen, top), strokeWidth * 2)
                        drawLine(cyan, Offset(left, top), Offset(left, top + cornerLen), strokeWidth * 2)
                        // Top-Right
                        drawLine(cyan, Offset(right, top), Offset(right - cornerLen, top), strokeWidth * 2)
                        drawLine(cyan, Offset(right, top), Offset(right, top + cornerLen), strokeWidth * 2)
                        // Bottom-Left
                        drawLine(cyan, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth * 2)
                        drawLine(cyan, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth * 2)
                        // Bottom-Right
                        drawLine(cyan, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth * 2)
                        drawLine(cyan, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth * 2)

                        // Laser Beam
                        val laserY = top + (boxSize * laserYRatio)
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(left + 8.dp.toPx(), laserY),
                            end = Offset(right - 8.dp.toPx(), laserY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // QR Code Scanner Overlay Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (detectedUrl != null) "¡Código QR Detectado!" else "Buscando QR de Zoom, Meet, Telmex...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (detectedUrl != null) EmeraldSuccess else CyanPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Preset Selector
                Text(
                    text = "Prueba Rápida de Enlaces / QR de Demostración:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleQrs) { (title, url) ->
                        val isSelected = (selectedPresetName == title)
                        Surface(
                            onClick = {
                                selectedPresetName = title
                                detectedUrl = url
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyanPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanPrimary else Color.Transparent
                            )
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detected Link Display Box
                val currentUrl = detectedUrl ?: sampleQrs.first().second
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm and Trigger Recording Button
                Button(
                    onClick = {
                        onQrScanned(currentUrl)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_qr_scan_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONECTAR & GRABAR AHORA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
