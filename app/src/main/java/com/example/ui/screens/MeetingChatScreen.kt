package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.db.ChatMessageEntity
import com.example.ui.MeetingViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingChatScreen(
    viewModel: MeetingViewModel
) {
    val activeMeeting by viewModel.activeMeeting.collectAsState()
    val chatMessages by viewModel.activeChatMessages.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickQuestions = listOf(
        "¿Quién llegó tarde?",
        "Resume solamente lo relacionado con ventas.",
        "¿Qué dijo Edgar?",
        "¿Cuáles son los principales compromisos de Génesis?",
        "¿Hubo algún riesgo mencionado?"
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("meeting_chat_screen")
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chat con la Reunión",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CodexWhite
                )
                Text(
                    text = activeMeeting?.title ?: "Memoria Corporativa Gemini",
                    style = MaterialTheme.typography.bodySmall,
                    color = CodexGrayLight
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CodexDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CodexWhite))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gemini 2.5 Pro",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Prompt Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickQuestions) { question ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendChatMessage(question)
                    },
                    label = { Text(question, style = MaterialTheme.typography.labelSmall, color = CodexWhite) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = CodexDarkSurface,
                        labelColor = CodexWhite
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Haz una pregunta sobre la reunión...", color = CodexGrayLight) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CodexWhite,
                    unfocusedBorderColor = CodexBorder,
                    focusedTextColor = CodexWhite,
                    unfocusedTextColor = CodexWhite,
                    focusedContainerColor = CodexDarkSurface,
                    unfocusedContainerColor = CodexDarkSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendChatMessage(text)
                    }
                },
                containerColor = CodexWhite,
                contentColor = CodexBlack,
                shape = CircleShape,
                modifier = Modifier.testTag("send_chat_message_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar", tint = CodexBlack)
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageEntity) {
    val isUser = message.sender == "Usuario"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 12.dp
            ),
            color = CodexDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) CodexWhite.copy(alpha = 0.5f) else CodexBorder),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isUser) CodexGrayLight else CodexWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CodexWhite
                )
            }
        }
    }
}
