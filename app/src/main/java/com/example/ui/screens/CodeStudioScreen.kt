package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CodeSnippetEntity
import com.example.data.HfModelEntity
import com.example.domain.AgentBuildStep
import com.example.domain.AgentExecutionEvent
import com.example.domain.AgentProjectType
import com.example.domain.CodingAgentPermissionConfig
import com.example.domain.CodingAgentProject
import com.example.domain.CodingChatTurn
import com.example.domain.DiffLine
import com.example.domain.DiffLineType
import com.example.domain.FileActionStepPill
import com.example.domain.ModelCapabilities
import com.example.domain.ProjectBuildReport
import com.example.domain.ProjectVirtualFile
import com.example.domain.RecentCodeFileEntry
import com.example.domain.TerminalCommandResult
import com.example.domain.UniversalHfModelProvider
import com.example.domain.WorkspaceCheckpoint
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanBright
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.NeuralVioletBright
import com.example.ui.theme.SlateTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeStudioScreen(
    snippets: List<CodeSnippetEntity>,
    selectedSnippetId: Int?,
    editorTitle: String,
    editorLanguage: String,
    editorCode: String,
    consoleOutput: String,
    aiPrompt: String,
    activeModel: HfModelEntity?,
    // Autonomous Coding Agent IDE state & callbacks
    agentProjects: List<CodingAgentProject> = emptyList(),
    activeAgentProject: CodingAgentProject? = null,
    selectedProjectFilePath: String = "index.html",
    recentCodeFiles: List<RecentCodeFileEntry> = emptyList(),
    isAgentBuildingProject: Boolean = false,
    agentBuildSteps: List<AgentBuildStep> = emptyList(),
    codingChatTurns: List<CodingChatTurn> = emptyList(),
    agentPermissionConfig: CodingAgentPermissionConfig = CodingAgentPermissionConfig(),
    // Upgraded Model-Independent Studio state & callbacks
    agentExecutionEvents: List<AgentExecutionEvent> = emptyList(),
    latestDiffLines: List<DiffLine> = emptyList(),
    workspaceCheckpoints: List<WorkspaceCheckpoint> = emptyList(),
    terminalHistory: List<TerminalCommandResult> = emptyList(),
    lastBuildReport: ProjectBuildReport? = null,
    modelCapabilities: ModelCapabilities = UniversalHfModelProvider().getCapabilities(activeModel),
    onSelectAgentProject: (CodingAgentProject) -> Unit = {},
    onSelectProjectFile: (String) -> Unit = {},
    onOpenRecentFile: (RecentCodeFileEntry) -> Unit = {},
    onClearRecentFiles: () -> Unit = {},
    onAddNewProjectFile: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteProjectFile: (String) -> Unit = {},
    onRenameProjectFile: (String, String) -> Unit = { _, _ -> },
    onBuildAutonomousProject: (String, AgentProjectType?) -> Unit = { _, _ -> },
    onCancelAgentTask: () -> Unit = {},
    onFixProjectBugsWithAgent: () -> Unit = {},
    onRestoreCheckpoint: (String) -> Unit = {},
    onExecuteTerminalCommand: (String) -> Unit = {},
    onStopTerminalProcess: () -> Unit = {},
    onImportProjectFromBrowserFiles: (String, List<ProjectVirtualFile>) -> Unit = { _, _ -> },
    onImportProjectFromLink: (String) -> Unit = {},
    onUpdateAgentPermissionConfig: (CodingAgentPermissionConfig) -> Unit = {},
    onBuildProjectZipBytes: () -> ByteArray = { ByteArray(0) },
    onBuildProjectHtmlBytes: () -> ByteArray = { ByteArray(0) },
    onNotifyExportSuccess: (String) -> Unit = {},
    onSelectSnippet: (CodeSnippetEntity) -> Unit,
    onLanguageChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onAiPromptChange: (String) -> Unit,
    onGenerateCodeWithAi: () -> Unit,
    onRunCodeInSandbox: () -> Unit,
    onSaveNewSnippet: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val syntaxTransformation = remember { SyntaxHighlightTransformation() }
    val lineCount = editorCode.lines().size.coerceAtLeast(1)

    var selectedStudioTab by rememberSaveable { mutableStateOf("AGENT_BUILDER") }
    var selectedProjectType by rememberSaveable { mutableStateOf<AgentProjectType?>(null) }
    var showImportLinkDialog by rememberSaveable { mutableStateOf(false) }
    var showNewFileDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameFileDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchReplaceBar by rememberSaveable { mutableStateOf(false) }
    var showDiffViewer by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var replaceQuery by rememberSaveable { mutableStateOf("") }
    var previewReloadNonce by rememberSaveable { mutableStateOf(0) }
    var previewViewportMode by rememberSaveable { mutableStateOf("DESKTOP") } // "MOBILE", "TABLET", "DESKTOP"
    var isUnsavedDirty by rememberSaveable { mutableStateOf(false) }

    // Undo / Redo Stacks for Multi-File Editor
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    val webConsoleMessages = remember { mutableStateListOf<String>() }

    // Real SAF ZIP Archive Exporter
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = onBuildProjectZipBytes()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                onNotifyExportSuccess("${activeAgentProject?.name ?: "Project"} (.zip • ${bytes.size / 1024} KB)")
            }
        }
    }

    // Real SAF Standalone HTML Bundle Exporter
    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = onBuildProjectHtmlBytes()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                onNotifyExportSuccess("${activeAgentProject?.name ?: "Bundle"} (.html • ${bytes.size} bytes)")
            }
        }
    }

    // Zero-Permission Android Storage Access Framework File Browser Launcher
    val fileBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val loadedFiles = uris.mapIndexedNotNull { idx, uri ->
                try {
                    var displayName = "imported_file_${idx + 1}.txt"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0 && cursor.moveToFirst()) {
                            displayName = cursor.getString(nameIdx) ?: displayName
                        }
                    }
                    val textContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText().take(25_000)
                    } ?: "// Imported empty file"

                    val lang = when {
                        displayName.endsWith(".html", true) -> "HTML"
                        displayName.endsWith(".css", true) -> "CSS"
                        displayName.endsWith(".js", true) || displayName.endsWith(".ts", true) -> "JavaScript"
                        displayName.endsWith(".kt", true) -> "Kotlin"
                        displayName.endsWith(".py", true) -> "Python"
                        displayName.endsWith(".sql", true) -> "SQL"
                        else -> "Markdown"
                    }
                    ProjectVirtualFile(path = displayName, language = lang, content = textContent)
                } catch (_: Exception) {
                    null
                }
            }
            if (loadedFiles.isNotEmpty()) {
                onImportProjectFromBrowserFiles(
                    "Imported Project (${loadedFiles.first().path})",
                    loadedFiles
                )
                selectedStudioTab = "MULTI_FILE_EDITOR"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("code_studio_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Streamlined Midnight Navy & Cyan Header + Export ZIP/HTML + 3-Mode Studio Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricCyan, NeuralViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Code Studio",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Code Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${modelCapabilities.displayName} • ${activeAgentProject?.name ?: editorTitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyanBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val slug = (activeAgentProject?.name ?: "code_studio_project")
                                    .lowercase()
                                    .replace(Regex("[^a-z0-9]+"), "_")
                                    .trim('_')
                                    .ifEmpty { "project" }
                                exportZipLauncher.launch("${slug}_archive.zip")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("export_project_zip_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Export Project ZIP", modifier = Modifier.size(15.dp), tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ZIP", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                val slug = (activeAgentProject?.name ?: "bundle")
                                    .lowercase()
                                    .replace(Regex("[^a-z0-9]+"), "_")
                                    .trim('_')
                                    .ifEmpty { "bundle" }
                                exportHtmlLauncher.launch("${slug}_standalone.html")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("export_project_html_button")
                        ) {
                            Icon(Icons.Default.Web, contentDescription = "Export Standalone HTML", modifier = Modifier.size(15.dp), tint = EmeraldEnclave)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HTML", style = MaterialTheme.typography.labelSmall)
                        }
                        if (workspaceCheckpoints.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    workspaceCheckpoints.firstOrNull()?.let { onRestoreCheckpoint(it.id) }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("rollback_checkpoint_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Rollback Checkpoint", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                isUnsavedDirty = false
                                onSaveNewSnippet(editorTitle)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("save_snippet_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save File", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isUnsavedDirty) "Save •" else "Save", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Streamlined Studio Mode Bar: AI Builder | Code Editor | Live Preview | Permissions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf(
                        Triple("AGENT_BUILDER", "AI Builder (${codingChatTurns.size})", Icons.Default.Psychology),
                        Triple("MULTI_FILE_EDITOR", "Code Editor (${activeAgentProject?.files?.size ?: 1})", Icons.Default.Description),
                        Triple("LIVE_PREVIEW", "Live Preview", Icons.Default.Preview),
                        Triple("AGENT_PERMISSIONS", "Permissions", Icons.Default.Security)
                    )
                    items(modes) { (modeKey, modeLabel, icon) ->
                        FilterChip(
                            selected = selectedStudioTab == modeKey,
                            onClick = { selectedStudioTab = modeKey },
                            leadingIcon = {
                                Icon(icon, contentDescription = modeLabel, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(modeLabel, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.testTag("studio_mode_$modeKey"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }
            }
        }

        // PANEL A: AI CHAT PANEL (Streaming, Stop/Regenerate, Copy, DeepSeek Thinking, File Pills)
        if (selectedStudioTab == "AGENT_BUILDER") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            ElectricCyan.copy(alpha = 0.55f),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("autonomous_agent_builder_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Studio Build Stream & DeepSeek Thinking",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Arch: ${modelCapabilities.architecture} • ${modelCapabilities.hardwareNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onFixProjectBugsWithAgent,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("agent_fix_bugs_button")
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = "Find & Fix Bugs", tint = AmberConflict, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fix Bugs", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Workspace Projects Switcher Carousel
                        if (agentProjects.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(agentProjects, key = { it.id }) { proj ->
                                    val isSelected = activeAgentProject?.id == proj.id
                                    Surface(
                                        color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .width(235.dp)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectAgentProject(proj) }
                                            .testTag("agent_project_card_${proj.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = NeuralViolet.copy(alpha = 0.22f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = proj.projectType.badgeText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${proj.files.size} files",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldEnclave
                                                )
                                            }
                                            Text(
                                                text = proj.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = proj.originSource,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Conversational Coding Turns with Copy, Regenerate, Thinking Trace & File Pills
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            codingChatTurns.forEach { turn ->
                                DeepSeekAndAiStudioCodingTurnCard(
                                    turn = turn,
                                    onCopySummary = {
                                        clipboardManager.setText(AnnotatedString(turn.assistantSummary))
                                    },
                                    onRegenerateTurn = {
                                        onBuildAutonomousProject(turn.userPrompt, selectedProjectType)
                                    },
                                    onClickFilePill = { pill ->
                                        if (pill.actionVerb.equals("Compiled", true)) {
                                            selectedStudioTab = "LIVE_PREVIEW"
                                        } else {
                                            onSelectProjectFile(pill.filePath)
                                            selectedStudioTab = "MULTI_FILE_EDITOR"
                                        }
                                    },
                                    onOpenLivePreview = {
                                        selectedStudioTab = "LIVE_PREVIEW"
                                    },
                                    onOpenCodeEditor = {
                                        selectedStudioTab = "MULTI_FILE_EDITOR"
                                    }
                                )
                            }
                        }

                        // Live Autonomous Agent Execution Trace Card
                        AnimatedVisibility(visible = agentBuildSteps.isNotEmpty()) {
                            Surface(
                                color = Color(0xFF0B101B),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, NeuralViolet.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .testTag("agent_build_trace_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Psychology,
                                                contentDescription = null,
                                                tint = NeuralVioletBright,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isAgentBuildingProject) {
                                                    "Autonomous Agent Running Tools..."
                                                } else {
                                                    "Last Build Execution Pipeline (Verified)"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = ElectricCyan
                                            )
                                        }
                                        if (isAgentBuildingProject) {
                                            TextButton(
                                                onClick = onCancelAgentTask,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("cancel_agent_task_button")
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = null, tint = CrimsonAlert, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Stop", style = MaterialTheme.typography.labelMedium, color = CrimsonAlert)
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { selectedStudioTab = "LIVE_PREVIEW" },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("open_live_preview_from_trace_button")
                                            ) {
                                                Text("Open Live Preview →", style = MaterialTheme.typography.labelMedium, color = EmeraldEnclave)
                                            }
                                        }
                                    }

                                    if (isAgentBuildingProject) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = ElectricCyan
                                        )
                                    }

                                    agentBuildSteps.forEach { step ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (step.isCompleted) EmeraldEnclave else SlateTextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = step.actionTitle,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (step.isCompleted) Color.White else SlateTextSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = step.detailNote,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SlateTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Project Target Type Filter Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedProjectType == null,
                                    onClick = { selectedProjectType = null },
                                    label = { Text("Auto-Detect Target", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            items(AgentProjectType.entries) { pType ->
                                FilterChip(
                                    selected = selectedProjectType == pType,
                                    onClick = { selectedProjectType = pType },
                                    label = { Text(pType.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("agent_project_type_${pType.name.lowercase()}")
                                )
                            }
                        }

                        // Quick One-Tap AI Studio Prompt Templates
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val quickTemplates = listOf(
                                "Create a responsive HTML website with interactive analytics" to AgentProjectType.WEB_APP_AND_FORM,
                                "Find and fix a bug in my existing project" to null,
                                "Build a Playable Neon Space Shooter 2D Arcade Game" to AgentProjectType.HTML5_CANVAS_GAME,
                                "Build a Jetpack Compose Sprint & Habit Tracker Android App" to AgentProjectType.ANDROID_COMPOSE_APP
                            )
                            items(quickTemplates) { (tplPrompt, tplType) ->
                                SuggestionChip(
                                    onClick = {
                                        onAiPromptChange(tplPrompt)
                                        selectedProjectType = tplType
                                        onBuildAutonomousProject(tplPrompt, tplType)
                                    },
                                    label = { Text(tplPrompt, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        // Bottom AI Chat Composer + Stop/Send + File Browser & Link Import
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = aiPrompt,
                                        onValueChange = onAiPromptChange,
                                        placeholder = {
                                            Text(
                                                "Message Z AI Coding Agent (create website, fix bug, run tests, build app)...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_code_prompt_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (isAgentBuildingProject) {
                                        Button(
                                            onClick = onCancelAgentTask,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CrimsonAlert,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("ai_stop_generation_button")
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = "Stop Generation", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Stop", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                onBuildAutonomousProject(aiPrompt, selectedProjectType)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricCyan,
                                                contentColor = Color(0xFF042F2E)
                                            ),
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("ai_generate_code_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send Coding Prompt",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Build",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (agentPermissionConfig.allowFileBrowserProjectImport) {
                                                fileBrowserLauncher.launch(arrayOf("*/*"))
                                            }
                                        },
                                        enabled = agentPermissionConfig.allowFileBrowserProjectImport,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("import_project_file_browser_button")
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = "Import via File Browser", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add via File Browser", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { showImportLinkDialog = true },
                                        enabled = agentPermissionConfig.allowLinkAndRepoImport,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("import_project_link_button")
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = "Import via Link", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Project via Link", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PANEL B: REAL-TIME AGENT PROGRESS PANEL (All 10 lifecycle phases & 14 tools)
        if (selectedStudioTab == "AGENT_PROGRESS") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeuralViolet.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .testTag("agent_progress_panel_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1D))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Autonomous Agent Execution Event Stream",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "14 Standardized Agent Tools • Real-Time Backend Events",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                            if (isAgentBuildingProject) {
                                Button(
                                    onClick = onCancelAgentTask,
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAlert)
                                ) {
                                    Text("Cancel Task", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (agentExecutionEvents.isEmpty()) {
                            Text(
                                text = "No agent tool events recorded yet in this session. Submit a build or bug-fix request in Chat & Thinking to stream live tool execution.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        } else {
                            agentExecutionEvents.forEach { ev ->
                                Surface(
                                    color = Color(0xFF111A2E),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (ev.isSuccess) EmeraldEnclave.copy(alpha = 0.4f) else CrimsonAlert.copy(alpha = 0.5f),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "• ${ev.phase.title}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (ev.isSuccess) ElectricCyan else CrimsonAlert,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = ev.toolUsed?.toolName ?: ev.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeuralVioletBright
                                            )
                                        }
                                        Text(
                                            text = ev.detailMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                        if (ev.toolOutputSnippet.isNotBlank()) {
                                            Text(
                                                text = ev.toolOutputSnippet,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SlateTextSecondary
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

        // PANEL D: LIVE INTERACTIVE PREVIEW + VIEWPORT RESIZER + CONSOLE & BUILD ERROR INSPECTOR
        if (selectedStudioTab == "LIVE_PREVIEW") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmeraldEnclave.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                        .testTag("live_interactive_preview_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF080C14))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF111827))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (activeAgentProject?.projectType == AgentProjectType.HTML5_CANVAS_GAME) {
                                        Icons.Default.SportsEsports
                                    } else {
                                        Icons.Default.Web
                                    },
                                    contentDescription = "Live Interactive Preview",
                                    tint = EmeraldEnclave,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = activeAgentProject?.name ?: "Live App & Game Preview",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (lastBuildReport?.isSuccess == false) {
                                            "BUILD FAILED (${lastBuildReport.diagnostics.size} errors)"
                                        } else {
                                            "Build Verified • Interactive Runtime Active"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (lastBuildReport?.isSuccess == false) CrimsonAlert else EmeraldEnclave
                                    )
                                }
                            }

                            // Viewport Resizer & Reload Controls
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("MOBILE" to "375w", "TABLET" to "520w", "DESKTOP" to "100%").forEach { (mode, label) ->
                                    TextButton(
                                        onClick = { previewViewportMode = mode },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (previewViewportMode == mode) ElectricCyan else SlateTextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { previewReloadNonce++ },
                                    modifier = Modifier.size(32.dp).testTag("reload_live_preview_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload Preview",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                        }

                        // Real Build Error Banner if lastBuildReport has errors
                        if (lastBuildReport != null && !lastBuildReport.isSuccess) {
                            Surface(
                                color = Color(0xFF28111B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Compiler & Syntax Diagnostics (${lastBuildReport.diagnostics.size})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CrimsonAlert,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = onFixProjectBugsWithAgent,
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF042F2E)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Auto-Fix with Agent", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    lastBuildReport.diagnostics.forEach { d ->
                                        Text(
                                            text = "✗ ${d.filePath}:${d.lineNumber} — ${d.message}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }

                        if (!agentPermissionConfig.allowLiveInteractivePreview) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Live WebView Sandbox is disabled in Agent Permissions. Enable 'Allow Live Interactive Preview' to run generated websites and games.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CrimsonAlert
                                )
                            }
                        } else {
                            val htmlToRender = activeAgentProject?.bundledPreviewHtml
                                ?: "<html><body style='background:#090D16;color:#fff;padding:16px;'><h2>Ready to Preview</h2></body></html>"

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val viewportModifier = when (previewViewportMode) {
                                    "MOBILE" -> Modifier.width(360.dp).height(420.dp)
                                    "TABLET" -> Modifier.width(520.dp).height(420.dp)
                                    else -> Modifier.fillMaxWidth().height(420.dp)
                                }
                                OfflineInteractiveProjectWebView(
                                    htmlContent = htmlToRender,
                                    reloadNonce = previewReloadNonce,
                                    onConsoleLog = { msg ->
                                        if (webConsoleMessages.size > 25) webConsoleMessages.removeAt(0)
                                        webConsoleMessages.add(msg)
                                    },
                                    modifier = viewportModifier
                                )
                            }

                            if (webConsoleMessages.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF060911))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "WebView JS Console & Runtime Output:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                    webConsoleMessages.takeLast(5).forEach { logLine ->
                                        Text(
                                            text = logLine,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



        // 4. CODING AGENT ENVIRONMENT PERMISSIONS TAB
        if (selectedStudioTab == "AGENT_PERMISSIONS") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .testTag("coding_agent_permissions_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Coding Agent Permissions",
                                tint = EmeraldEnclave
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Coding Agent Environment & Security Permissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Control what ${activeModel?.displayName ?: "Gemma 2B"} can build, edit, import, and execute",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AgentPermissionToggleRow(
                            title = "Require Approval Before Agent Builds or Modifies Project",
                            subtitle = "Prompts you in the Zero-Trust Gate before Z AI generates or overwrites project files",
                            checked = agentPermissionConfig.requireApprovalBeforeAgentBuild,
                            onCheckedChange = {
                                onUpdateAgentPermissionConfig(
                                    agentPermissionConfig.copy(requireApprovalBeforeAgentBuild = it)
                                )
                            }
                        )

                        AgentPermissionToggleRow(
                            title = "Allow Android File Browser Project Import",
                            subtitle = "Permits importing personal source files (.html, .js, .kt, .py) via system document picker",
                            checked = agentPermissionConfig.allowFileBrowserProjectImport,
                            onCheckedChange = {
                                onUpdateAgentPermissionConfig(
                                    agentPermissionConfig.copy(allowFileBrowserProjectImport = it)
                                )
                            }
                        )

                        AgentPermissionToggleRow(
                            title = "Allow Project Link & GitHub Repository Import",
                            subtitle = "Permits importing personal projects via GitHub URL, Gist, or web link",
                            checked = agentPermissionConfig.allowLinkAndRepoImport,
                            onCheckedChange = {
                                onUpdateAgentPermissionConfig(
                                    agentPermissionConfig.copy(allowLinkAndRepoImport = it)
                                )
                            }
                        )

                        AgentPermissionToggleRow(
                            title = "Allow Live Interactive WebView & Game Canvas",
                            subtitle = "Enables live HTML5/JS/Canvas execution for generated websites, forms, and games",
                            checked = agentPermissionConfig.allowLiveInteractivePreview,
                            onCheckedChange = {
                                onUpdateAgentPermissionConfig(
                                    agentPermissionConfig.copy(allowLiveInteractivePreview = it)
                                )
                            }
                        )
                    }
                }
            }
        }

        // PANEL C: RECENT FILES HISTORY + MULTI-FILE PROJECT EXPLORER + DIFF VIEWER + SEARCH/REPLACE + UNDO/REDO + CODE EDITOR
        if (selectedStudioTab == "AGENT_BUILDER" || selectedStudioTab == "MULTI_FILE_EDITOR") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Recent Files History (Last 5 Opened Files Saved Locally)
                    if (recentCodeFiles.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF0E182B),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .testTag("recent_files_section")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Recent Files History",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Recent Files (${recentCodeFiles.take(5).size}/5)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyanBright,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(
                                        onClick = onClearRecentFiles,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("clear_recent_files_button")
                                    ) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("recent_files_row")
                                ) {
                                    items(
                                        items = recentCodeFiles.take(5),
                                        key = { it.uniqueKey }
                                    ) { recentEntry ->
                                        val isCurrentlyOpen = if (recentEntry.snippetId != null) {
                                            selectedSnippetId == recentEntry.snippetId
                                        } else {
                                            selectedSnippetId == null &&
                                                activeAgentProject?.id == recentEntry.projectId &&
                                                selectedProjectFilePath.equals(recentEntry.filePath, ignoreCase = true)
                                        }
                                        FilterChip(
                                            selected = isCurrentlyOpen,
                                            onClick = {
                                                isUnsavedDirty = false
                                                onOpenRecentFile(recentEntry)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (isCurrentlyOpen) ElectricCyan else SlateTextSecondary
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "${recentEntry.filePath} • ${recentEntry.projectName.take(16)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag(
                                                "recent_file_chip_${recentEntry.filePath.replace(".", "_")}"
                                            ),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Project Virtual File Tree Explorer Bar
                    if (activeAgentProject != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Project Files (${activeAgentProject.name}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { showNewFileDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("add_new_project_file_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New File", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(
                                    onClick = { showRenameFileDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("rename_project_file_button")
                                ) {
                                    Text("Rename", style = MaterialTheme.typography.labelSmall)
                                }
                                if (activeAgentProject.files.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteProjectFile(selectedProjectFilePath) },
                                        modifier = Modifier.size(28.dp).testTag("delete_project_file_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = CrimsonAlert, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeAgentProject.files, key = { "vfs_${it.path}" }) { vFile ->
                                val isCurrentTab = selectedProjectFilePath == vFile.path
                                val tabLabel = if (isCurrentTab && isUnsavedDirty) {
                                    "• ${vFile.path} (${vFile.language})"
                                } else {
                                    "${vFile.path} (${vFile.language})"
                                }
                                FilterChip(
                                    selected = isCurrentTab,
                                    onClick = {
                                        isUnsavedDirty = false
                                        onSelectProjectFile(vFile.path)
                                    },
                                    label = { Text(tabLabel) },
                                    modifier = Modifier.testTag("vfs_file_chip_${vFile.path.replace(".", "_")}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeuralViolet.copy(alpha = 0.28f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Saved SQLite Snippets Carousel
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(snippets, key = { "snip_${it.id}" }) { snip ->
                            FilterChip(
                                selected = selectedSnippetId == snip.id,
                                onClick = { onSelectSnippet(snip) },
                                label = { Text("${snip.title} (${snip.language})") },
                                modifier = Modifier.testTag("snippet_chip_${snip.id}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Language Selector Row + Editor Utility Toolbar (Undo, Redo, Find/Replace, Diff Viewer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val languages = listOf("Kotlin", "Python", "JavaScript", "HTML", "CSS", "SQL", "Bash")
                            items(languages) { lang ->
                                FilterChip(
                                    selected = editorLanguage == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("lang_chip_$lang")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    if (undoStack.isNotEmpty()) {
                                        val prev = undoStack.removeAt(undoStack.lastIndex)
                                        redoStack.add(editorCode)
                                        onCodeChange(prev)
                                    }
                                },
                                enabled = undoStack.isNotEmpty(),
                                modifier = Modifier.size(32.dp).testTag("editor_undo_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (redoStack.isNotEmpty()) {
                                        val next = redoStack.removeAt(redoStack.lastIndex)
                                        undoStack.add(editorCode)
                                        onCodeChange(next)
                                    }
                                },
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(32.dp).testTag("editor_redo_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { showSearchReplaceBar = !showSearchReplaceBar },
                                modifier = Modifier.size(32.dp).testTag("editor_search_replace_toggle")
                            ) {
                                Icon(Icons.Default.FindReplace, contentDescription = "Search & Replace", tint = ElectricCyan, modifier = Modifier.size(17.dp))
                            }
                            IconButton(
                                onClick = { showDiffViewer = !showDiffViewer },
                                modifier = Modifier.size(32.dp).testTag("editor_diff_viewer_toggle")
                            ) {
                                Icon(Icons.Default.Difference, contentDescription = "Toggle Diff Viewer", tint = NeuralVioletBright, modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    // Collapsible Search & Replace Bar
                    AnimatedVisibility(visible = showSearchReplaceBar) {
                        Surface(
                            color = Color(0xFF10192B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = replaceQuery,
                                    onValueChange = { replaceQuery = it },
                                    label = { Text("Replace") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (searchQuery.isNotEmpty() && editorCode.contains(searchQuery)) {
                                            undoStack.add(editorCode)
                                            onCodeChange(editorCode.replace(searchQuery, replaceQuery))
                                        }
                                    }
                                ) {
                                    Text("Replace All", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Collapsible Unified Diff Viewer
                    AnimatedVisibility(visible = showDiffViewer) {
                        Surface(
                            color = Color(0xFF080D18),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, NeuralViolet.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                .testTag("diff_viewer_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Unified File Diff Viewer (${latestDiffLines.count { it.type == DiffLineType.ADDED }} additions, ${latestDiffLines.count { it.type == DiffLineType.REMOVED }} deletions)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeuralVioletBright,
                                    fontWeight = FontWeight.Bold
                                )
                                if (latestDiffLines.isEmpty()) {
                                    Text(
                                        text = "No diff lines recorded yet. Edit code or run an Agent build/patch to inspect line-by-line additions and removals.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateTextSecondary
                                    )
                                } else {
                                    latestDiffLines.take(28).forEach { dl ->
                                        val prefix = when (dl.type) {
                                            DiffLineType.ADDED -> "+ "
                                            DiffLineType.REMOVED -> "- "
                                            DiffLineType.UNCHANGED -> "  "
                                        }
                                        val lineColor = when (dl.type) {
                                            DiffLineType.ADDED -> EmeraldEnclave
                                            DiffLineType.REMOVED -> CrimsonAlert
                                            DiffLineType.UNCHANGED -> SlateTextSecondary
                                        }
                                        Text(
                                            text = "$prefix${dl.content}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = lineColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Syntax-Highlighted Code Editor with Line Numbers
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0B101B)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Editor Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141D2F))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isUnsavedDirty) "• " else ""}$editorTitle • $lineCount lines • Autosave ON",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyanBright
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { selectedStudioTab = "LIVE_PREVIEW" },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preview_project_button")
                                ) {
                                    Icon(Icons.Default.Preview, contentDescription = "Live Preview", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = onRunCodeInSandbox,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldEnclave,
                                        contentColor = Color(0xFF022C22)
                                    ),
                                    modifier = Modifier.testTag("run_snippet_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Snippet", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run & Compile", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Line Numbers + Syntax Highlighted BasicTextField
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .width(26.dp)
                            ) {
                                for (lineNum in 1..lineCount) {
                                    Text(
                                        text = "$lineNum",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = SlateTextSecondary.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                BasicTextField(
                                    value = editorCode,
                                    onValueChange = { updated ->
                                        if (undoStack.size > 30) undoStack.removeAt(0)
                                        undoStack.add(editorCode)
                                        redoStack.clear()
                                        isUnsavedDirty = true
                                        onCodeChange(updated)
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFFF8FAFC)
                                    ),
                                    cursorBrush = SolidColor(ElectricCyanBright),
                                    visualTransformation = syntaxTransformation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .testTag("code_editor_text_field")
                                )
                            }
                        }
                    }
                }
            }
        }



        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showImportLinkDialog) {
        ImportProjectFromLinkDialog(
            onDismiss = { showImportLinkDialog = false },
            onConfirmUrl = { linkUrl ->
                onImportProjectFromLink(linkUrl)
                showImportLinkDialog = false
            }
        )
    }

    if (showNewFileDialog) {
        AddVirtualProjectFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirmFile = { path, lang, content ->
                onAddNewProjectFile(path, lang, content)
                showNewFileDialog = false
            }
        )
    }

    if (showRenameFileDialog) {
        var newFileName by remember(selectedProjectFilePath) { mutableStateOf(selectedProjectFilePath) }
        AlertDialog(
            onDismissRequest = { showRenameFileDialog = false },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("New File Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameProjectFile(selectedProjectFilePath, newFileName)
                        showRenameFileDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFileDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeepSeekAndAiStudioCodingTurnCard(
    turn: CodingChatTurn,
    onCopySummary: () -> Unit,
    onRegenerateTurn: () -> Unit,
    onClickFilePill: (FileActionStepPill) -> Unit,
    onOpenLivePreview: () -> Unit,
    onOpenCodeEditor: () -> Unit
) {
    var isThinkingExpanded by rememberSaveable(turn.id) { mutableStateOf(true) }

    Surface(
        color = Color(0xFF0B101B),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeuralViolet.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("coding_chat_turn_${turn.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // User Prompt Header + Copy & Regenerate Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "YOU → ${turn.userPrompt}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopySummary,
                        modifier = Modifier.size(28.dp).testTag("copy_turn_button_${turn.id}")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy response", tint = SlateTextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = onRegenerateTurn,
                        modifier = Modifier.size(28.dp).testTag("regenerate_turn_button_${turn.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate response", tint = ElectricCyan, modifier = Modifier.size(15.dp))
                    }
                }
            }

            // DeepSeek R1 / Grok 3 Expandable "Thought for X.Xs" Reasoning Box
            Surface(
                color = Color(0xFF111827),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { isThinkingExpanded = !isThinkingExpanded }
                    .testTag("thinking_trace_toggle_${turn.id}")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "DeepSeek / Grok Thinking Trace",
                                tint = NeuralVioletBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thought for ${turn.thinkingDurationSeconds} • ${turn.modelName} Reasoning Trace",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeuralVioletBright,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand or Collapse Thinking",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isThinkingExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            turn.thinkingMonologueLines.forEach { line ->
                                Text(
                                    text = line,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // Google AI Studio Live File Creation & Edit Step Pills
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "File Operations & Build Actions (Tap any file pill to inspect):",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldEnclave,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    turn.fileActionPills.forEach { pill ->
                        val isCompile = pill.actionVerb.equals("Compiled", true)
                        val pillColor = if (isCompile) EmeraldEnclave else ElectricCyan
                        Surface(
                            color = pillColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(1.dp, pillColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .clickable { onClickFilePill(pill) }
                                .testTag("file_step_pill_${pill.filePath.replace(".", "_")}_${pill.actionVerb.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCompile) Icons.Default.CheckCircle else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = pillColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${pill.actionVerb} ${pill.filePath}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• ${pill.detailBadge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pillColor
                                )
                            }
                        }
                    }
                }
            }

            // Assistant Architectural Summary
            Text(
                text = turn.assistantSummary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )

            // Quick Action Row: Open Code Editor | Launch Live Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCodeEditor,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inspect Files in Editor", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onOpenLivePreview,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldEnclave,
                        contentColor = Color(0xFF022C22)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run in Live Preview", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OfflineInteractiveProjectWebView(
    htmlContent: String,
    reloadNonce: Int,
    onConsoleLog: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            onConsoleLog("[${consoleMessage.messageLevel()}] line ${consoleMessage.lineNumber()}: ${consoleMessage.message()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                setBackgroundColor(android.graphics.Color.parseColor("#090D16"))
            }
        },
        update = { webView ->
            val taggedHtml = "$htmlContent\n<!-- nonce:$reloadNonce -->"
            webView.loadDataWithBaseURL(
                "https://local.zai.enclave/",
                taggedHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

@Composable
private fun AgentPermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ImportProjectFromLinkDialog(
    onDismiss: () -> Unit,
    onConfirmUrl: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("https://github.com/developer/personal-portfolio-app") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Personal Project via Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a GitHub repository URL, Gist link, or project link to import its files into Code Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Repository or Project URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_project_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmUrl(urlInput) },
                modifier = Modifier.testTag("confirm_import_project_link_button")
            ) {
                Text("Import & Open in IDE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddVirtualProjectFileDialog(
    onDismiss: () -> Unit,
    onConfirmFile: (String, String, String) -> Unit
) {
    var fileName by remember { mutableStateOf("components.js") }
    var fileLanguage by remember { mutableStateOf("JavaScript") }
    var initialCode by remember { mutableStateOf("// New project module created in Z AI IDE\n") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File in Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (e.g. form.html, game.js, Screen.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("HTML", "CSS", "JavaScript", "Kotlin", "Python", "Markdown")) { lang ->
                        FilterChip(
                            selected = fileLanguage == lang,
                            onClick = { fileLanguage = lang },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = initialCode,
                    onValueChange = { initialCode = it },
                    label = { Text("Initial Code") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmFile(fileName, fileLanguage, initialCode) }
            ) {
                Text("Create File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Regex-driven syntax highlighter for Kotlin, Python, JS, HTML, CSS, SQL, and Bash inside BasicTextField.
 */
private class SyntaxHighlightTransformation : VisualTransformation {
    private val keywordRegex = Regex(
        """\b(fun|val|var|def|return|if|else|when|for|while|class|import|SELECT|FROM|WHERE|AND|ORDER|BY|ASC|DESC|function|const|let|echo|true|false|Int|String|Boolean|document|window|canvas|export)\b"""
    )
    private val stringRegex = Regex(""""[^"\n]*"|'[^'\n]*'""")
    private val numberRegex = Regex("""\b\d+(\.\d+)?\b""")
    private val commentRegex = Regex("""(//.*$|#.*$|--.*$)""", RegexOption.MULTILINE)

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)

            keywordRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(color = NeuralVioletBright, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }
            numberRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(color = AmberConflict),
                    match.range.first,
                    match.range.last + 1
                )
            }
            stringRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(color = EmeraldEnclave),
                    match.range.first,
                    match.range.last + 1
                )
            }
            commentRegex.findAll(raw).forEach { match ->
                addStyle(
                    SpanStyle(color = SlateTextSecondary),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}
