package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.VisualAssetEntity
import com.example.data.user.UserSessionManager
import com.example.ui.MeetingViewModel
import com.example.ui.components.HeaderBanner
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualIntelligenceScreen(
    viewModel: MeetingViewModel,
    onSelectMeeting: (Long) -> Unit
) {
    val visualAssets by viewModel.activeVisualAssets.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()
    val currentUser by UserSessionManager.currentUser.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Centro Visual & Activos, 1: 30 Skills Engine, 2: Model Router & MCP, 3: 11 Agentes & Entregables
    var selectedAssetForPreview by remember { mutableStateOf<VisualAssetEntity?>(null) }
    var selectedModel by remember { mutableStateOf("Gemini Vision") }
    var selectedMcp by remember { mutableStateOf("Figma MCP") }

    val allVisualSkills = listOf(
        Pair("GenerateMindMap", "Mapas Mentales"),
        Pair("GenerateConceptMap", "Mapas Conceptuales"),
        Pair("GenerateArchitectureDiagram", "Arquitecturas C4"),
        Pair("GenerateERDiagram", "Diagramas ERD Database"),
        Pair("GenerateSequenceDiagram", "Diagramas de Secuencia"),
        Pair("GenerateFlowchart", "Flujogramas"),
        Pair("GenerateBPMN", "Procesos BPMN"),
        Pair("GenerateRoadmap", "Hoja de Ruta / Roadmap"),
        Pair("GenerateTimeline", "Línea del Tiempo"),
        Pair("GenerateStoryboard", "Storyboards de Reunión"),
        Pair("GenerateWireframe", "Wireframes UI"),
        Pair("GenerateMockup", "Mockups de Pantallas"),
        Pair("GeneratePrototype", "Prototipos UI/UX"),
        Pair("GenerateDashboardUI", "Dashboard UI Design"),
        Pair("GeneratePresentation", "Presentaciones Diapositivas"),
        Pair("GenerateInfographic", "Infografías Ejecutivas"),
        Pair("GenerateKnowledgeGraph", "Grafos de Conocimiento"),
        Pair("GenerateDecisionTree", "Árboles de Decisión"),
        Pair("GenerateRiskMap", "Mapas de Riesgos"),
        Pair("GenerateSprintBoard", "Tableros Sprint Agile"),
        Pair("GenerateWhiteboard", "Whiteboard Colaborativo"),
        Pair("GenerateCloudArchitecture", "Arquitectura Cloud AWS/GCP"),
        Pair("GenerateNetworkDiagram", "Diagramas de Red"),
        Pair("GenerateDesignSystem", "Design System Components"),
        Pair("GenerateExecutiveSummary", "Resumen Ejecutivo Visual"),
        Pair("GenerateVisualSummary", "Visual Summary Card"),
        Pair("GenerateMeetingPoster", "Poster de la Reunión"),
        Pair("GenerateBrandAssets", "Activos de Marca"),
        Pair("GenerateUXFlow", "Flujo UX de Usuario"),
        Pair("GenerateJourneyMap", "Customer Journey Map")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("visual_intelligence_screen")
    ) {
        HeaderBanner(
            title = "Visual Intelligence Engine",
            subtitle = "Motor IA nativo para transformación automática de reuniones en activos visuales",
            roleTag = "Agente #11 • ${currentUser.role.label}"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Feedback Banner
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

        // Navigation Tabs
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
                Text("Biblioteca Visual", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 0) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Skill Engine (30)", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 1) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Model Router & MCP", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 2) CodexWhite else CodexGrayLight)
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("11 Agentes & Entregables", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = if (activeTab == 3) CodexWhite else CodexGrayLight)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTab) {
            0 -> {
                // Visual Intelligence Center Overview & Assets
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "📊 Indicadores del Visual Intelligence Center",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IndicatorChip("Diagramas", "${visualAssets.size + 14}", CyanPrimary, Modifier.weight(1f))
                                IndicatorChip("Presentaciones", "12", IndigoSecondary, Modifier.weight(1f))
                                IndicatorChip("Mockups", "24", VioletAccent, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IndicatorChip("Wireframes", "35", AmberWarning, Modifier.weight(1f))
                                IndicatorChip("Arquitecturas", "15", EmeraldSuccess, Modifier.weight(1f))
                                IndicatorChip("Infografías", "20", CyanPrimary, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IndicatorChip("Mapas Mentales", "42", VioletAccent, Modifier.weight(1f))
                                IndicatorChip("Whiteboards", "10", IndigoSecondary, Modifier.weight(1f))
                                IndicatorChip("Tiempo Ahorrado", "120 hrs", EmeraldSuccess, Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎨 Activos Visuales Generados (${visualAssets.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Generación Automática Activa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    items(visualAssets) { asset ->
                        VisualAssetCard(
                            asset = asset,
                            onPreview = { selectedAssetForPreview = asset },
                            onExport = {
                                viewModel.executeVisualSkill("Exportar ${asset.title}", asset.assetType, asset.mcpSource, asset.modelUsed)
                            }
                        )
                    }
                }
            }
            1 -> {
                // 30 Visual Skills Catalog
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "⚡ Skill Engine - Catalogo de 30 Skills Visuales",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Selecciona un skill para invocar la generación mediante el Visual Intelligence Agent:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(allVisualSkills) { (skillName, label) ->
                        Surface(
                            onClick = {
                                viewModel.executeVisualSkill(skillName, "SKILL", selectedMcp, selectedModel)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().testTag("skill_item_$skillName")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = skillName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Ejecutar",
                                    tint = CyanPrimary
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                // Model Router & MCP Integrations
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "🔀 Model Router Inteligente",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Selección automática o manual del modelo especializado según el tipo de entregable:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        val models = listOf(
                            Triple("Gemini Flash", "Respuestas rápidas", Icons.Default.FlashOn),
                            Triple("Gemini Pro", "Análisis profundo", Icons.Default.Psychology),
                            Triple("GPT", "Razonamiento complejo", Icons.Default.Extension),
                            Triple("Claude", "Documentos largos", Icons.Default.Article),
                            Triple("Imagen", "Ilustraciones de alta calidad", Icons.Default.Image),
                            Triple("Gemini Vision", "Diagramas inteligentes", Icons.Default.Visibility),
                            Triple("Mermaid", "Diagramas técnicos", Icons.Default.Code),
                            Triple("PlantUML", "Arquitectura C4 & UML", Icons.Default.AccountTree),
                            Triple("Graphviz", "Grafos de red", Icons.Default.Hub),
                            Triple("Excalidraw", "Whiteboards colaborativos", Icons.Default.Draw),
                            Triple("Canva", "Presentaciones ejecutivas", Icons.Default.Slideshow),
                            Triple("Figma", "Interfaces UI/UX", Icons.Default.DesignServices)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            models.forEach { (name, desc, icon) ->
                                Surface(
                                    onClick = { selectedModel = name },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedModel == name) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedModel == name) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = CyanPrimary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (selectedModel == name) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔌 Integraciones MCP (Model Context Protocol)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        val mcpServers = listOf(
                            "Figma MCP" to "Exportación de wireframes y design systems a capas de Figma",
                            "Canva MCP" to "Sincronización de presentaciones e infografías ejecutivas",
                            "Miro MCP" to "Whiteboards colaborativos y mapas de empatía",
                            "Excalidraw MCP" to "Diagramación libre y prototipado rápido",
                            "Lucidchart MCP" to "Diagramas de procesos BPMN corporativos",
                            "Whimsical MCP" to "Mapas mentales y flujogramas de producto",
                            "Confluence MCP" to "Documentación técnica en wiki corporativa",
                            "Google Slides MCP" to "Generación directa de diapositivas en Google Workspace"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mcpServers.forEach { (mcpName, desc) ->
                                var isConnected by remember { mutableStateOf(true) }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mcpName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isConnected,
                                            onCheckedChange = { isConnected = it }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // 11 Agents & Deliverables 21-40
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "🤖 Red de 11 Agentes Empresariales",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        val enterpriseAgents = listOf(
                            "1. Meeting Agent" to "Transcripción, resumen y acta de reunión",
                            "2. HR Agent" to "Evaluación de clima y requisiciones de personal",
                            "3. Sales Agent" to "Seguimiento de clientes, ofertas y Telmex pipeline",
                            "4. Marketing Agent" to "Estrategias de contenido y campañas corporativas",
                            "5. Finance Agent" to "Análisis de presupuestos, ROI y costos",
                            "6. Legal Agent" to "Revisión de contratos y cláusulas normativas",
                            "7. Document Agent" to "Exportación a PDF, Word, Excel y PowerPoint",
                            "8. Memory Agent" to "Indexación semántica e historial corporativo",
                            "9. Automation Agent" to "Ejecución de flujos en Gmail, Slack y Teams",
                            "10. Supervisor Agent" to "Gobernanza de procesos y auditoría de accesos",
                            "11. Visual Intelligence Agent" to "Arquitecto de Software, UX/UI & Generador de Recursos Visuales"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            enterpriseAgents.forEach { (agentTitle, agentRole) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (agentTitle.contains("11")) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (agentTitle.contains("11")) CyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (agentTitle.contains("11")) Icons.Default.Palette else Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = if (agentTitle.contains("11")) CyanPrimary else IndigoSecondary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = agentTitle,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = agentRole,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📦 Entregables Visuales (21 al 40)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        val deliverables = listOf(
                            "21. Diagramas UML automáticos", "22. Diagramas BPMN", "23. Diagramas C4",
                            "24. Arquitecturas de software", "25. Whiteboards colaborativos", "26. Mapas mentales",
                            "27. Mapas conceptuales", "28. Infografías ejecutivas", "29. Wireframes",
                            "30. Mockups", "31. Prototipos UI/UX", "32. Presentaciones automáticas",
                            "33. Storyboards de reuniones", "34. Knowledge Graph visual", "35. Dashboards visuales",
                            "36. Diagramas de infraestructura Cloud", "37. Diseños exportables a Figma",
                            "38. Diseños exportables a Canva", "39. Templates reutilizables", "40. Biblioteca visual empresarial"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            deliverables.forEach { del ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = del,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to preview Visual Asset render
    if (selectedAssetForPreview != null) {
        val asset = selectedAssetForPreview!!
        AlertDialog(
            onDismissRequest = { selectedAssetForPreview = null },
            title = { Text(asset.title) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tipo: ${asset.assetType} • Modelo: ${asset.modelUsed}", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                    Text(asset.description, style = MaterialTheme.typography.bodySmall)

                    // Vector Canvas Visualizer Simulation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            VisualAssetCanvasRenderer(asset = asset)
                        }
                    }

                    Text("Formatos de exportación: ${asset.exportFormats}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.executeVisualSkill("Exportar ${asset.title}", asset.assetType, asset.mcpSource, asset.modelUsed)
                    selectedAssetForPreview = null
                }) {
                    Text("Exportar vía ${asset.mcpSource}")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAssetForPreview = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun IndicatorChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CodexDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CodexWhite)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = CodexGrayLight, fontSize = 9.sp)
        }
    }
}

@Composable
fun VisualAssetCard(
    asset: VisualAssetEntity,
    onPreview: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CodexDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visual_asset_card_${asset.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CodexBorder
                ) {
                    Text(
                        text = asset.assetType,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CodexWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = asset.mcpSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = CodexGrayLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = asset.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CodexWhite
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = asset.description,
                style = MaterialTheme.typography.bodySmall,
                color = CodexGrayLight,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CodexBorder)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = CodexWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previsualizar", style = MaterialTheme.typography.labelSmall, color = CodexWhite)
                }
                Button(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CodexWhite, contentColor = CodexBlack)
                ) {
                    Icon(imageVector = Icons.Default.IosShare, contentDescription = null, tint = CodexBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar", style = MaterialTheme.typography.labelSmall, color = CodexBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VisualAssetCanvasRenderer(asset: VisualAssetEntity) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val w = size.width
        val h = size.height

        // Draw node lines
        drawLine(
            color = CodexWhite,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.5f, h * 0.3f),
            strokeWidth = 2f
        )
        drawLine(
            color = CodexWhite,
            start = Offset(w * 0.5f, h * 0.3f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = 2f
        )
        drawLine(
            color = CodexWhite,
            start = Offset(w * 0.5f, h * 0.3f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = 2f
        )

        // Draw nodes
        drawCircle(color = CodexWhite, radius = 16f, center = Offset(w * 0.2f, h * 0.5f))
        drawCircle(color = CodexWhite, radius = 22f, center = Offset(w * 0.5f, h * 0.3f))
        drawCircle(color = CodexGrayLight, radius = 16f, center = Offset(w * 0.8f, h * 0.5f))
        drawCircle(color = CodexGrayLight, radius = 16f, center = Offset(w * 0.5f, h * 0.8f))
    }
}
