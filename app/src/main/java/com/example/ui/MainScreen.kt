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
import com.example.ui.theme.CyanPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MeetingViewModel = viewModel(),
    initialDeepLinkUrl: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val meetings by viewModel.meetings.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Deep Link auto join trigger
    LaunchedEffect(initialDeepLinkUrl) {
        if (!initialDeepLinkUrl.isNullOrEmpty()) {
            viewModel.joinMeetingFromUrl(initialDeepLinkUrl)
            selectedTab = 1
            onDeepLinkConsumed()
        }
    }

    // Default select first meeting if available when opening detail or chat
    LaunchedEffect(meetings) {
        if (meetings.isNotEmpty() && viewModel.currentMeetingId.value == null) {
            viewModel.selectMeeting(meetings.first().id)
        }
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_bot_logo_1785008057179),
                                contentDescription = "Bot Asistente",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Heavenly AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enterprise Meeting Intelligence",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyanPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Inicio") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Grabar") },
                    label = { Text("Grabar") },
                    modifier = Modifier.testTag("nav_item_record")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Análisis") },
                    label = { Text("Análisis") },
                    modifier = Modifier.testTag("nav_item_detail")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("Chat IA") },
                    modifier = Modifier.testTag("nav_item_chat")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.ManageSearch, contentDescription = "Memoria") },
                    label = { Text("Memoria") },
                    modifier = Modifier.testTag("nav_item_memory")
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
                    }
                )
                1 -> RecordMeetingScreen(
                    viewModel = viewModel,
                    onRecordingFinished = { selectedTab = 2 }
                )
                2 -> MeetingDetailScreen(
                    viewModel = viewModel,
                    onOpenChatClick = { selectedTab = 3 }
                )
                3 -> MeetingChatScreen(
                    viewModel = viewModel
                )
                4 -> EnterpriseMemoryScreen(
                    viewModel = viewModel,
                    onSelectMeeting = { meetingId ->
                        viewModel.selectMeeting(meetingId)
                        selectedTab = 2
                    }
                )
            }
        }
    }
}
