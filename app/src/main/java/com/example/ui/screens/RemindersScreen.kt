package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.db.ReminderEntity
import com.example.data.notifications.WhatsAppNotificationListenerService
import com.example.ui.RemindersViewModel
import com.example.ui.components.HeaderBanner
import com.example.ui.theme.*
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val expected = android.content.ComponentName(context, WhatsAppNotificationListenerService::class.java).flattenToString()
    return flat.split(":").any { it == expected }
}

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isConnected by viewModel.isGoogleConnected.collectAsState()
    val reminders by viewModel.upcomingReminders.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasNotifPermission) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    var whatsAppListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                whatsAppListenerEnabled = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(activityResult.data)
            viewModel.authManager.onAuthorizationResolved(result)
            viewModel.onGoogleAuthorized()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("reminders_screen")
    ) {
        HeaderBanner(
            title = "Recordatorios",
            subtitle = "Avisos automáticos de tus próximas reuniones",
            roleTag = if (isConnected) "Calendar conectado" else "Sin conectar"
        )

        Spacer(modifier = Modifier.height(16.dp))

        statusMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CodexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Calendar", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
                        Text(
                            if (isConnected) "Revisando tus próximas reuniones cada 15 min" else "Conecta tu cuenta para recibir recordatorios",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodexGrayLight
                        )
                    }
                    if (isConnected) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Conectado", tint = EmeraldSuccess)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.syncNow() }, modifier = Modifier.weight(1f)) {
                            Text("Sincronizar ahora")
                        }
                        OutlinedButton(onClick = { viewModel.disconnectGoogle() }, modifier = Modifier.weight(1f)) {
                            Text("Desconectar")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                val request = viewModel.authManager.buildAuthorizationRequest()
                                val result = viewModel.authManager.authorize(request)
                                val pendingIntent = result.pendingIntent
                                if (result.hasResolution() && pendingIntent != null) {
                                    authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                                } else {
                                    viewModel.onGoogleAuthorized()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("connect_google_calendar_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                    ) {
                        Text("Conectar Google Calendar")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CodexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vigilancia de WhatsApp", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
                        Text(
                            "Detecta reuniones mencionadas en tus chats y crea recordatorios",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodexGrayLight
                        )
                    }
                    if (whatsAppListenerEnabled) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Activo", tint = EmeraldSuccess)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Privacidad: al activarlo, la app puede leer el título y texto de tus notificaciones de WhatsApp para detectar menciones de reuniones. Solo los mensajes que coinciden con palabras clave (reunión, cita, zoom, hora, etc.) se envían al proveedor de IA configurado.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CodexGrayLight
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!whatsAppListenerEnabled) {
                    Button(
                        onClick = { context.startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth().testTag("enable_whatsapp_listener_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                    ) {
                        Text("Activar vigilancia de WhatsApp")
                    }
                } else {
                    OutlinedButton(
                        onClick = { context.startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Administrar acceso a notificaciones")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Próximos (${reminders.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (reminders.isEmpty()) {
            Text(
                "No hay recordatorios próximos todavía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderCard(reminder = reminder, onDismiss = { viewModel.dismissReminderManually(reminder) })
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: ReminderEntity, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
                Text(
                    SimpleDateFormat("EEE d MMM, HH:mm", Locale("es", "MX")).format(Date(reminder.eventTimeMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = CodexGrayLight
                )
            }
            TextButton(onClick = onDismiss) { Text("Ocultar") }
        }
    }
}
