package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HfModelEntity
import com.example.domain.DocumentAnalysisReport
import com.example.domain.KVCacheStats
import com.example.domain.WebSearchCitation
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanBright
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateTextSecondary
import java.nio.charset.StandardCharsets
import java.util.Locale

data class ZAIChatMessage(
    val id: Long,
    val isUser: Boolean,
    val senderName: String,
    val modelBadge: String,
    val text: String,
    val timestamp: String,
    val citations: List<WebSearchCitation> = emptyList(),
    val actionExecutedBadge: String? = null
)

@Composable
fun ZAIChatScreen(
    messages: List<ZAIChatMessage>,
    activeModel: HfModelEntity?,
    isWebSearchEnabled: Boolean,
    isSearchingWeb: Boolean,
    isListeningVoice: Boolean,
    isLiveAiCallActive: Boolean,
    liveCallDurationSeconds: Int,
    isLiveCallMuted: Boolean,
    liveCallStatusText: String,
    kvCacheStats: KVCacheStats = KVCacheStats(),
    currentDocumentReport: DocumentAnalysisReport? = null,
    onAnalyzeDocumentFile: (String, String) -> Unit = { _, _ -> },
    onCreateTasksFromDocument: (DocumentAnalysisReport) -> Unit = {},
    onDismissDocumentReport: () -> Unit = {},
    onSendChatMessage: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onToggleWebSearch: (Boolean) -> Unit,
    onToggleVoiceMic: () -> Unit,
    onToggleLiveAiCall: () -> Unit,
    onToggleLiveCallMute: () -> Unit,
    onDialRealPhoneNumber: (String) -> Unit,
    onOpenPermissionsCenter: () -> Unit,
    onSpeakMessageAloud: (String) -> Unit,
    onOpenCitationUrl: (String) -> Unit,
    onNavigateToCodeStudio: () -> Unit,
    onNavigateToWorkspaceReminders: () -> Unit
) {
    val context = LocalContext.current
    var draftPrompt by rememberSaveable { mutableStateOf("") }
    var showRealPhoneDialerBar by rememberSaveable { mutableStateOf(false) }
    var phoneNumberToDial by rememberSaveable { mutableStateOf("+15550192834") }

    // Real SAF Document Picker for AI Document & File Analyzer
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                var displayName = "selected_document.txt"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText().take(60_000)
                }.orEmpty()
                onAnalyzeDocumentFile(displayName, content)
            }
        }
    }

    // Real SAF Markdown Report Exporter
    val exportReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        val report = currentDocumentReport
        if (uri != null && report != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(report.exportMarkdownContent.toByteArray(StandardCharsets.UTF_8))
                    out.flush()
                }
            }
        }
    }

    // Runtime permission launcher for Microphone + Notifications when starting Live Call or Voice
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onToggleLiveAiCall()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("zai_chat_screen")
    ) {
        // 1. Streamlined Midnight Navy & Cyan Header Bar with Live AI Call, KV-Cache Telemetry & File Analyzer
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricCyan, NeuralViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Z AI Chat Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Z AI Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = buildString {
                                    append("${activeModel?.displayName ?: "Gemma 2 2B Instruct"} • ${activeModel?.quantization ?: "Q4_K_M"}")
                                    if (kvCacheStats.isEnabled) {
                                        append(" • ⚡ KV-Cache (${kvCacheStats.hitCount} hits)")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyanBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isLiveAiCallActive) {
                                    val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    callPermissionsLauncher.launch(perms.toTypedArray())
                                } else {
                                    onToggleLiveAiCall()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveAiCallActive) CrimsonAlert else EmeraldEnclave,
                                contentColor = if (isLiveAiCallActive) Color.White else Color(0xFF022C22)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("start_live_ai_call_button")
                        ) {
                            Icon(
                                imageVector = if (isLiveAiCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                                contentDescription = "Live AI Voice Call",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isLiveAiCallActive) "End Call" else "AI Call",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onStartNewChat,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("new_chat_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "New Chat",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Quick Capability & Document Analyzer Pills
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = currentDocumentReport != null,
                            onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Analyze Document File",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text("Analyze File", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.testTag("chat_analyze_document_button"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = isWebSearchEnabled,
                            onClick = { onToggleWebSearch(!isWebSearchEnabled) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Toggle Web Grounding",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (isWebSearchEnabled) "Web Search: ON" else "Local Mode",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("chat_web_search_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = showRealPhoneDialerBar,
                            onClick = { showRealPhoneDialerBar = !showRealPhoneDialerBar },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneForwarded,
                                    contentDescription = "Phone Call Dialer",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text("Phone Dialer", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.testTag("toggle_phone_dialer_chip")
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onOpenPermissionsCenter,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "App Permissions",
                                    tint = AmberConflict,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text("Permissions", style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.testTag("chat_open_permissions_chip")
                        )
                    }
                }
            }
        }

        if (isSearchingWeb) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = ElectricCyan
            )
        }

        // 1B. Live AI Voice Call Interactive Banner (When in a Live Voice Call with the Model)
        AnimatedVisibility(visible = isLiveAiCallActive) {
            LiveAIVoiceCallCard(
                modelName = activeModel?.displayName ?: "Gemma 2 2B Instruct",
                callDurationSeconds = liveCallDurationSeconds,
                isMuted = isLiveCallMuted,
                statusText = liveCallStatusText,
                onToggleMute = onToggleLiveCallMute,
                onTriggerVoiceListen = onToggleVoiceMic,
                onSendQuickSpokenPrompt = { prompt ->
                    onSendChatMessage(prompt)
                },
                onOpenPhoneDialer = { showRealPhoneDialerBar = true },
                onEndCall = onToggleLiveAiCall
            )
        }

        // 1C. Quick Direct Phone Call Bar (When user wants Z AI to place a real phone call)
        AnimatedVisibility(visible = showRealPhoneDialerBar) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phoneNumberToDial,
                        onValueChange = { phoneNumberToDial = it },
                        label = { Text("Phone Number to Call", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_phone_number_input")
                    )
                    Button(
                        onClick = { onDialRealPhoneNumber(phoneNumberToDial) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldEnclave,
                            contentColor = Color(0xFF022C22)
                        ),
                        modifier = Modifier.testTag("chat_place_phone_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dial Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Chat Message Stream + Document Analysis Card + One-Tap Action Sections
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("zai_chat_messages_list"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Real AI Document & File Analysis Report Card
            if (currentDocumentReport != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, ElectricCyan.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .testTag("document_analysis_report_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0E1A2F)
                        )
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Document Analysis: ${currentDocumentReport.fileName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${currentDocumentReport.fileTypeLabel} • ${currentDocumentReport.wordCount} words • ${currentDocumentReport.lineCount} lines • ~${currentDocumentReport.estimatedTokens} tokens",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyanBright
                                        )
                                    }
                                }
                                TextButton(onClick = onDismissDocumentReport) {
                                    Text("Hide", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                                }
                            }

                            if (currentDocumentReport.extractedActionItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Extracted Action Items (${currentDocumentReport.extractedActionItems.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldEnclave,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    currentDocumentReport.extractedActionItems.take(4).forEach { item ->
                                        Text(
                                            text = "• $item",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onCreateTasksFromDocument(currentDocumentReport) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = Color(0xFF042F2E)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doc_create_tasks_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Tasks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val base = currentDocumentReport.fileName.substringBeforeLast(".").ifEmpty { "document" }
                                        exportReportLauncher.launch("${base}_analysis_report.md")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("doc_export_report_button")
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export .md", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // One-Tap Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val starterCards = listOf(
                            Triple(
                                "☀️ Plan My Day",
                                "Give me my morning executive briefing, prioritize my top tasks, and check calendar conflicts",
                                Icons.Default.WbSunny
                            ),
                            Triple(
                                "📄 Analyze Sprint Doc",
                                "ANALYZE_SAMPLE_DOC",
                                Icons.Default.Description
                            ),
                            Triple(
                                "📋 Add Task To-Do",
                                "Remind me to finish my top priority project deliverable in 30 minutes with a ringing alarm",
                                Icons.Default.CheckCircle
                            ),
                            Triple(
                                "🔥 Habit Reminder",
                                "Set a 30 minute habit reminder to drink water and stretch with auto-repeat",
                                Icons.Default.LocalFireDepartment
                            ),
                            Triple(
                                "📅 Fix Schedule",
                                "Analyze my calendar conflicts, rank tasks by deadline, and suggest optimal meeting slots",
                                Icons.Default.Schedule
                            ),
                            Triple(
                                "💻 Build App / Game",
                                "Build a playable 2D space arcade game and interactive form in Code Studio",
                                Icons.Default.Code
                            )
                        )
                        items(starterCards) { (title, fullPrompt, icon) ->
                            ChatStarterPromptCard(
                                title = title,
                                prompt = if (fullPrompt == "ANALYZE_SAMPLE_DOC") {
                                    "Extract key headings, metrics, and action items from sprint specification"
                                } else {
                                    fullPrompt
                                },
                                icon = icon,
                                onClick = {
                                    if (fullPrompt == "ANALYZE_SAMPLE_DOC") {
                                        onAnalyzeDocumentFile(
                                            "sprint_architecture_spec.md",
                                            "# Q4 Mobile AI Architecture Specification\n\n" +
                                                "## 1. Inference Pipeline & KV-Cache\n" +
                                                "Enable prefix-hash KV-cache reuse across multi-turn chats to cut prefill latency by 65%.\n" +
                                                "- [ ] Verify GPU Vulkan delegate layer offloading on Android devices\n" +
                                                "- [ ] Benchmark KV-cache hit rate on multi-turn prompts\n\n" +
                                                "## 2. Code Studio Deliverables\n" +
                                                "TODO: Ship Recent Files (last 5 opened files) navigation bar and ZIP/HTML archive exporter.\n" +
                                                "ACTION: Schedule final release QA review before Friday."
                                        )
                                    } else {
                                        onSendChatMessage(fullPrompt)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ZAIChatMessageBubble(
                    message = msg,
                    onSpeakAloud = { onSpeakMessageAloud(msg.text) },
                    onOpenCitation = onOpenCitationUrl,
                    onOpenCodeStudio = onNavigateToCodeStudio,
                    onOpenReminders = onNavigateToWorkspaceReminders
                )
            }
        }

        // 3. Bottom Gemini / ChatGPT Style Multimodal Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleVoiceMic,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListeningVoice) AmberConflict.copy(alpha = 0.24f)
                            else ElectricCyan.copy(alpha = 0.14f)
                        )
                        .testTag("chat_voice_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListeningVoice) AmberConflict else ElectricCyan
                    )
                }

                OutlinedTextField(
                    value = draftPrompt,
                    onValueChange = { draftPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask Z AI or tap any card above (no typing needed)...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_prompt_input"),
                    shape = RoundedCornerShape(18.dp)
                )

                IconButton(
                    onClick = {
                        if (draftPrompt.isNotBlank()) {
                            onSendChatMessage(draftPrompt)
                            draftPrompt = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricCyan, NeuralViolet)
                            )
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAIVoiceCallCard(
    modelName: String,
    callDurationSeconds: Int,
    isMuted: Boolean,
    statusText: String,
    onToggleMute: () -> Unit,
    onTriggerVoiceListen: () -> Unit,
    onSendQuickSpokenPrompt: (String) -> Unit,
    onOpenPhoneDialer: () -> Unit,
    onEndCall: () -> Unit
) {
    val mins = callDurationSeconds / 60
    val secs = callDurationSeconds % 60
    val formattedTimer = String.format(Locale.US, "%02d:%02d", mins, secs)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(1.5.dp, EmeraldEnclave, RoundedCornerShape(20.dp))
            .testTag("live_ai_voice_call_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldEnclave.copy(alpha = 0.22f))
                            .border(2.dp, EmeraldEnclave, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Live Call Active",
                            tint = EmeraldEnclave,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIVE AI CALL • $modelName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldEnclave
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = EmeraldEnclave.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "🟢 $formattedTimer",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldEnclave,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Instant Voice Prompts during Call (No Typing Needed)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickVoiceTopics = listOf(
                    "👋 What can you help me with today?",
                    "⏰ Set a 5 minute habit alarm",
                    "📅 Read my schedule & tasks aloud",
                    "💡 Give me a quick productivity coaching tip"
                )
                items(quickVoiceTopics) { topic ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clickable { onSendQuickSpokenPrompt(topic) }
                    ) {
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Call Control Bar: Mute, Speak Now, Dial Phone Contact, End Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleMute,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("live_call_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute or Unmute",
                        tint = if (isMuted) AmberConflict else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMuted) "Muted" else "Mic Live",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onTriggerVoiceListen,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("live_call_push_to_talk_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Push to Talk",
                        tint = EmeraldEnclave,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Speak Now", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onOpenPhoneDialer,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneForwarded,
                        contentDescription = "Dial Real Phone",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dial Contact", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonAlert,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("live_call_hangup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang Up",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hang Up", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChatStarterPromptCard(
    title: String,
    prompt: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(205.dp)
            .border(
                1.dp,
                ElectricCyan.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ZAIChatMessageBubble(
    message: ZAIChatMessage,
    onSpeakAloud: () -> Unit,
    onOpenCitation: (String) -> Unit,
    onOpenCodeStudio: () -> Unit,
    onOpenReminders: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) {
                        ElectricCyan.copy(alpha = 0.6f)
                    } else {
                        NeuralViolet.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    ElectricCyan.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!message.isUser) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldEnclave,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${message.senderName} • ${message.modelBadge}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (message.isUser) ElectricCyan else EmeraldEnclave,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = message.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!message.isUser) {
                            IconButton(
                                onClick = onSpeakAloud,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak Response Aloud",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Message Content
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Action Badge (if Z AI performed an autonomous workspace action)
                val badge = message.actionExecutedBadge
                if (!badge.isNullOrBlank()) {
                    Surface(
                        color = EmeraldEnclave.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            if (badge.contains("Code", true) || badge.contains("Synthesized", true)) {
                                onOpenCodeStudio()
                            } else {
                                onOpenReminders()
                            }
                        }
                    ) {
                        Text(
                            text = "⚡ $badge (Tap to View)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldEnclave,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Web Search Citations (if grounded)
                AnimatedVisibility(visible = message.citations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Sources & Citations:",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                        message.citations.forEach { cit ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCitation(cit.url) }
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${cit.title} (${cit.sourceDomain})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = cit.snippet,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open Citation",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
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
