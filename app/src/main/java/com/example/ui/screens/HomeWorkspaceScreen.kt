package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HfModelEntity
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.LocalZaiTokens

@Composable
fun HomeWorkspaceScreen(
    activeModel: HfModelEntity?,
    models: List<HfModelEntity>,
    isGenerating: Boolean,
    isListeningVoice: Boolean,
    liveVoiceTranscript: String,
    onSendPromptFromHome: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onSelectModel: (HfModelEntity) -> Unit,
    onToggleVoiceMic: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenTaskManager: () -> Unit,
    onOpenCodeStudio: () -> Unit,
    onOpenHfModelAdvisor: () -> Unit,
    onOpenHfModelHub: () -> Unit,
    onOpenVoiceAssistant: () -> Unit
) {
    val tokens = LocalZaiTokens.current
    val context = LocalContext.current
    var promptText by rememberSaveable { mutableStateOf("") }
    var attachedFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var attachedFileSnippet by rememberSaveable { mutableStateOf("") }
    var showModelSelectorDialog by remember { mutableStateOf(false) }

    // Sync live voice transcript if microphone was tapped
    if (isListeningVoice && liveVoiceTranscript.isNotBlank() && promptText != liveVoiceTranscript) {
        promptText = liveVoiceTranscript
    }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName = "attached_file.txt"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(idx) ?: displayName
                }
            }
            val textSnippet = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText().take(1500)
                }.orEmpty()
            }.getOrDefault("")
            attachedFileName = displayName
            attachedFileSnippet = textSnippet
        }
    }

    val shortModelLabel = remember(activeModel) {
        val raw = activeModel?.displayName ?: "Gemma 2B"
        when {
            raw.contains("Gemma 2 2B", ignoreCase = true) || raw.contains("gemma-2-2b", ignoreCase = true) -> "Gemma 2B"
            raw.length > 14 -> raw.take(14).trim()
            else -> raw
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.mainBackground)
            .testTag("home_workspace_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 4. LARGE WELCOME WORKSPACE CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = tokens.primaryCyan.copy(alpha = 0.14f),
                        spotColor = tokens.secondaryBlue.copy(alpha = 0.18f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tokens.welcomeCardTop,
                                tokens.welcomeCardBottom
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tokens.primaryCyan.copy(alpha = 0.35f),
                                tokens.border
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .testTag("welcome_workspace_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "WELCOME BACK",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.1.sp,
                            color = tokens.secondaryText
                        ),
                        modifier = Modifier.testTag("welcome_back_label")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your AI Workspace",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = tokens.primaryText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("welcome_workspace_heading")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "What would you like to work on today?",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = tokens.secondaryText
                        ),
                        modifier = Modifier.testTag("welcome_workspace_subtitle")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. AI INPUT COMPOSER INSIDE THE WELCOME CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.inputSurface)
                            .border(
                                width = 1.dp,
                                color = tokens.border,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("welcome_ai_input_composer")
                    ) {
                        if (attachedFileName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.welcomeCardTop)
                                    .border(1.dp, tokens.border, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attached file",
                                    tint = tokens.primaryCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = attachedFileName.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tokens.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    tint = tokens.secondaryText,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable {
                                            attachedFileName = null
                                            attachedFileSnippet = ""
                                        }
                                )
                            }
                        }

                        BasicTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            textStyle = TextStyle(
                                color = tokens.primaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(tokens.primaryCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val fullPrompt = buildSubmittedPrompt(promptText, attachedFileName, attachedFileSnippet)
                                    if (fullPrompt.isNotBlank()) {
                                        onSendPromptFromHome(fullPrompt)
                                        promptText = ""
                                        attachedFileName = null
                                        attachedFileSnippet = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 32.dp)
                                .testTag("home_ai_composer_input"),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (promptText.isEmpty()) {
                                        Text(
                                            text = "Ask anything...",
                                            style = TextStyle(
                                                color = tokens.secondaryText.copy(alpha = 0.78f),
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom control row: LEFT Attachment, CENTER-LEFT Model Chip, CENTER-RIGHT Mic, RIGHT Send/Stop
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // LEFT: Attachment / paperclip icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        attachmentLauncher.launch(arrayOf("*/*"))
                                    }
                                    .testTag("composer_attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach file",
                                    tint = tokens.secondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // CENTER-LEFT: Selected model chip showing "Gemma 2B" with a small downward arrow
                            Surface(
                                color = if (tokens.isDark) Color(0xFF0B213F) else Color(0xFFE0EEFF),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = tokens.secondaryBlue.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { showModelSelectorDialog = true }
                                    .testTag("composer_model_selector_chip")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = shortModelLabel,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = tokens.secondaryBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select AI Model",
                                        tint = tokens.secondaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // CENTER-RIGHT: Microphone icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListeningVoice) tokens.primaryCyan.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .clickable { onToggleVoiceMic() }
                                    .testTag("composer_microphone_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MicNone,
                                    contentDescription = "Voice input",
                                    tint = if (isListeningVoice) tokens.primaryCyan else tokens.secondaryText,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // RIGHT: Circular cyan-blue Send button (or Stop button while generating)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isGenerating) {
                                            CrimsonAlert.copy(alpha = 0.22f)
                                        } else {
                                            tokens.primaryCyan.copy(alpha = 0.16f)
                                        }
                                    )
                                    .border(
                                        width = 1.6.dp,
                                        color = if (isGenerating) CrimsonAlert else tokens.primaryCyan,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (isGenerating) {
                                            onStopGeneration()
                                        } else {
                                            val fullPrompt = buildSubmittedPrompt(
                                                promptText,
                                                attachedFileName,
                                                attachedFileSnippet
                                            ).ifBlank {
                                                "Give me a quick workspace briefing and suggest my top priorities today"
                                            }
                                            onSendPromptFromHome(fullPrompt)
                                            promptText = ""
                                            attachedFileName = null
                                            attachedFileSnippet = ""
                                        }
                                    }
                                    .testTag("composer_send_button")
                            ) {
                                Icon(
                                    imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.ArrowUpward,
                                    contentDescription = if (isGenerating) "Stop generation" else "Send prompt",
                                    tint = if (isGenerating) CrimsonAlert else tokens.primaryCyan,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. SIX MAIN FEATURE CARDS (STRICT 2-COLUMN x 3-ROW GRID)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_six_feature_cards_grid"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ROW 1: LEFT = AI Chat, RIGHT = Task Manager
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "AI Chat",
                        description = "Chat with your AI",
                        testTag = "feature_card_ai_chat",
                        onClick = onOpenAiChat,
                        modifier = Modifier.weight(1f)
                    )
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.EventAvailable,
                        title = "Task Manager",
                        description = "Tasks & Reminders",
                        testTag = "feature_card_task_manager",
                        onClick = onOpenTaskManager,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 2: LEFT = Code Studio, RIGHT = HF Model Advisor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.Code,
                        title = "Code Studio",
                        description = "Autonomous coding",
                        testTag = "feature_card_code_studio",
                        onClick = onOpenCodeStudio,
                        modifier = Modifier.weight(1f)
                    )
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.Memory,
                        title = "HF Model Advisor",
                        description = "Device compatibility",
                        testTag = "feature_card_hf_model_advisor",
                        onClick = onOpenHfModelAdvisor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ROW 3: LEFT = HF Model Hub, RIGHT = Voice Assistant
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.Hub,
                        title = "HF Model Hub",
                        description = "Discover & download",
                        testTag = "feature_card_hf_model_hub",
                        onClick = onOpenHfModelHub,
                        modifier = Modifier.weight(1f)
                    )
                    ZaiFeatureCardItem(
                        icon = Icons.Outlined.MicNone,
                        title = "Voice Assistant",
                        description = "Hands-free AI",
                        testTag = "feature_card_voice_assistant",
                        onClick = onOpenVoiceAssistant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showModelSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showModelSelectorDialog = false },
            containerColor = tokens.headerSurface,
            titleContentColor = tokens.primaryText,
            textContentColor = tokens.secondaryText,
            title = {
                Text(
                    text = "Select Active AI Model",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose the Hugging Face model brain powering Z AI Chat and Autonomous Code Studio:",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.secondaryText
                    )
                    models.forEach { item ->
                        val isSelected = item.modelId == activeModel?.modelId || item.isActive
                        Surface(
                            color = if (isSelected) tokens.welcomeCardTop else tokens.featureCardSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) tokens.primaryCyan else tokens.border,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectModel(item)
                                    showModelSelectorDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.primaryText,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${item.format} • ${item.quantization} • ${item.sizeMb} MB",
                                        color = tokens.secondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active model",
                                        tint = tokens.successGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showModelSelectorDialog = false
                        onOpenHfModelHub()
                    }
                ) {
                    Text("Open HF Model Hub", color = tokens.primaryCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelSelectorDialog = false }) {
                    Text("Close", color = tokens.secondaryText)
                }
            }
        )
    }
}

@Composable
private fun ZaiFeatureCardItem(
    icon: ImageVector,
    title: String,
    description: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalZaiTokens.current

    Box(
        modifier = modifier
            .heightIn(min = 148.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(tokens.featureCardSurface)
            .border(
                width = 1.dp,
                color = tokens.border,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cyan line-style icon near upper-left corner
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tokens.primaryCyan,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bold white title + subtle right-facing chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.primaryText,
                        lineHeight = 25.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = tokens.secondaryText.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Muted light-blue subtitle
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = tokens.secondaryText
                )
            )
        }
    }
}

private fun buildSubmittedPrompt(
    rawPrompt: String,
    attachedFileName: String?,
    attachedFileSnippet: String
): String {
    val clean = rawPrompt.trim()
    return if (attachedFileName != null) {
        val prefix = "[Attached File: $attachedFileName]\n${attachedFileSnippet.take(600)}\n\n"
        if (clean.isEmpty()) "${prefix}Analyze this attached file." else "$prefix$clean"
    } else {
        clean
    }
}
