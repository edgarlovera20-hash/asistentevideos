package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MeetingViewModel = viewModel(),
    initialDeepLinkUrl: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAiSettings by remember { mutableStateOf(false) }
    val meetings by viewModel.meetings.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Deep Link join: ask for confirmation before touching the microphone —
    // opening a link (from anywhere) should never silently start a recording.
    var pendingDeepLinkUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialDeepLinkUrl) {
        if (!initialDeepLinkUrl.isNullOrEmpty()) {
            pendingDeepLinkUrl = initialDeepLinkUrl
            onDeepLinkConsumed()
        }
    }

    pendingDeepLinkUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingDeepLinkUrl = null },
            title = { Text("¿Conectar y grabar audio?") },
            text = { Text("Este enlace quiere iniciar una reunión y grabar audio del micrófono:\n\n$url") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.joinMeetingFromUrl(url)
                    selectedTab = 1
                    pendingDeepLinkUrl = null
                }) { Text("Conectar y grabar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeepLinkUrl = null }) { Text("Cancelar") }
            }
        )
    }

    // Default select first meeting if available when opening detail or chat
    LaunchedEffect(meetings) {
        if (meetings.isNotEmpty() && viewModel.currentMeetingId.value == null) {
            viewModel.selectMeeting(meetings.first().id)
        }
    }

    if (showAiSettings) {
        AiSettingsScreen(onBack = { showAiSettings = false })
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Bot Asistente",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Heavenly Dreams",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CodexWhite
                            )
                            Text(
                                text = "Captura • Transcribe • Organiza",
                                style = MaterialTheme.typography.labelSmall,
                                color = CodexGrayLight
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAiSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de IA", tint = CodexWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CodexDarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CodexDarkSurface,
                contentColor = CodexWhite
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CodexWhite,
                    selectedTextColor = CodexWhite,
                    unselectedIconColor = CodexGrayLight,
                    unselectedTextColor = CodexGrayLight,
                    indicatorColor = CodexBorder
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Inicio") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Grabar") },
                    label = { Text("Grabar") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_record")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Análisis") },
                    label = { Text("Análisis") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_detail")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Palette, contentDescription = "Visual IA") },
                    label = { Text("Visual IA") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_visual")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("Chat IA") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_chat")
                )
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.ManageSearch, contentDescription = "Memoria") },
                    label = { Text("Memoria") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_memory")
                )
                NavigationBarItem(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Recordatorios") },
                    label = { Text("Avisos") },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_item_reminders")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onStartRecordingClick = { selectedTab = 1 },
                    onSelectMeeting = { meetingId ->
                        viewModel.selectMeeting(meetingId)
                        selectedTab = 2
                    },
                    onOpenVisualCenter = { selectedTab = 3 }
                )
                1 -> RecordMeetingScreen(
                    viewModel = viewModel,
                    onRecordingFinished = { selectedTab = 2 }
                )
                2 -> MeetingDetailScreen(
                    viewModel = viewModel,
                    onOpenChatClick = { selectedTab = 4 }
                )
                3 -> VisualIntelligenceScreen(
                    viewModel = viewModel,
                    onSelectMeeting = { meetingId ->
                        viewModel.selectMeeting(meetingId)
                        selectedTab = 2
                    }
                )
                4 -> MeetingChatScreen(
                    viewModel = viewModel
                )
                5 -> EnterpriseMemoryScreen(
                    viewModel = viewModel,
                    onSelectMeeting = { meetingId ->
                        viewModel.selectMeeting(meetingId)
                        selectedTab = 2
                    }
                )
                6 -> RemindersScreen()
            }
        }
    }
}
