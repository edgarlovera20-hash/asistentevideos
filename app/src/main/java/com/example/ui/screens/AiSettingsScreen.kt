package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.AiProvider
import com.example.data.ai.AiServiceFactory
import com.example.ui.AiSettingsViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = viewModel()
) {
    val activeProvider by viewModel.activeProvider.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proveedor de IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CodexDarkSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Elige qué modelo de IA analiza tus reuniones. El cambio aplica al cerrar y volver a abrir la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CodexGrayLight
                )
            }
            items(AiProvider.entries) { provider ->
                ProviderCard(
                    provider = provider,
                    isActive = provider == activeProvider,
                    viewModel = viewModel,
                    onSelect = { viewModel.selectProvider(provider) }
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProvider,
    isActive: Boolean,
    viewModel: AiSettingsViewModel,
    onSelect: () -> Unit
) {
    var apiKey by remember(provider) { mutableStateOf(viewModel.getApiKey(provider)) }
    var baseUrl by remember(provider) { mutableStateOf(viewModel.getBaseUrl(provider)) }
    var model by remember(provider) { mutableStateOf(viewModel.getModel(provider)) }
    val needsBaseUrl = provider == AiProvider.OLLAMA || provider == AiProvider.LMSTUDIO
    val needsApiKey = provider != AiProvider.OLLAMA && provider != AiProvider.LMSTUDIO

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var testResult by remember(provider) { mutableStateOf<Result<String>?>(null) }
    var isTesting by remember(provider) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CodexDarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(provider.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Activo", tint = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onSelect) { Text("Usar este") }
                }
            }

            if (needsApiKey) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.saveApiKey(provider, it)
                    },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (needsBaseUrl) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        viewModel.saveBaseUrl(provider, it)
                    },
                    label = { Text("URL del servidor (ej: http://192.168.1.20:11434/v1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    viewModel.saveModel(provider, it)
                },
                label = { Text("Modelo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isTesting = true
                    testResult = null
                    scope.launch {
                        val response = AiServiceFactory.createFor(context, provider)
                            .chat(meetingContext = "", userQuestion = "Responde únicamente con la palabra OK.", chatHistory = emptyList())
                        testResult = if (response.startsWith("No se pudo conectar")) {
                            Result.failure(Exception(response))
                        } else {
                            Result.success(response)
                        }
                        isTesting = false
                    }
                },
                enabled = !isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isTesting) "Probando..." else "Confirmar y probar conexión")
            }

            testResult?.let { result ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (result.isSuccess) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Conexión exitosa", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    } else {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            result.exceptionOrNull()?.message ?: "No se pudo conectar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE57373)
                        )
                    }
                }
            }
        }
    }
}
