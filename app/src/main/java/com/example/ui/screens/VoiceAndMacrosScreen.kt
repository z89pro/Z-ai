package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.WorkflowActionStepType
import com.example.data.WorkflowMacroEntity
import com.example.data.WorkflowTriggerEventType
import com.example.domain.WakeWordSessionState
import com.example.domain.ZAICustomSkill
import com.example.ui.MacroExecutionStepState
import com.example.ui.SecurityAuditLogEntry
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet
import java.util.Locale

@Composable
fun VoiceAndMacrosScreen(
    macros: List<WorkflowMacroEntity>,
    customSkills: List<ZAICustomSkill>,
    universalSystemPrompt: String,
    requireApprovalForHighRisk: Boolean,
    isAccessibilityBridgeEnabled: Boolean,
    securityAuditLogs: List<SecurityAuditLogEntry>,
    isListening: Boolean,
    isContinuousHandsFree: Boolean,
    liveTranscript: String,
    activeMacroTitle: String?,
    activeMacroSteps: List<MacroExecutionStepState>,
    draftedMeetingAgenda: String,
    lastVoiceCodeSnippet: String,
    lastVoiceCodeOutput: String,
    // Wake Word Listener Service state & callbacks
    wakeWordSessionState: WakeWordSessionState = WakeWordSessionState.STANDBY_WAKE_WORD,
    activeWakeWords: Set<String> = setOf("Jarvis", "Janu", "Friday", "Edith", "Ultron"),
    lastDetectedWakeWord: String = "Jarvis",
    microphoneDecibelDb: Float = -42f,
    wakeWordSilenceRemainingSec: Int = 60,
    onToggleWakeWordListenerService: (Boolean) -> Unit = {},
    onToggleWakeWordPhrase: (String) -> Unit = {},
    onSimulateWakeWordTrigger: (String, String) -> Unit = { _, _ -> },
    onToggleVoiceMic: (Boolean) -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onSubmitVoiceCommandText: (String) -> Unit,
    onOpenCodeStudio: () -> Unit,
    onRunMacro: (WorkflowMacroEntity) -> Unit,
    onCreateCustomMacro: (String, String, String, String, String, String, Boolean) -> Unit,
    onToggleMacroAutoTrigger: (WorkflowMacroEntity) -> Unit = {},
    onSimulateWorkflowEvent: (String, String) -> Unit = { _, _ -> },
    onToggleCustomSkill: (String) -> Unit,
    onAddCustomSkill: (String, String, String, Boolean) -> Unit,
    onToggleHighRiskFirewall: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDirectPhoneCall: (String) -> Unit,
    onDirectEmail: (String) -> Unit,
    onDirectAlarm: (Int, Int) -> Unit,
    onDirectSystemCalendar: () -> Unit
) {
    val context = LocalContext.current
    var commandInput by rememberSaveable { mutableStateOf("") }
    var showCreateMacroDialog by rememberSaveable { mutableStateOf(false) }
    var showAddSkillDialog by rememberSaveable { mutableStateOf(false) }
    var showSystemPromptPreview by rememberSaveable { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleWakeWordListenerService(true)
            onToggleVoiceMic(isContinuousHandsFree)
        } else {
            onSubmitVoiceCommandText("morning standup")
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.18f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 550 else 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_macros_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Continuous Microphone Wake Word Listener Service Card (Jarvis, Janu, Friday, Edith, Ultron)
        item {
            val isWakeServiceActive = wakeWordSessionState != WakeWordSessionState.DISABLED
            val isCommandSessionOpen = wakeWordSessionState == WakeWordSessionState.ACTIVE_COMMAND_SESSION
            val dbProgress = ((microphoneDecibelDb + 55f) / 53f).coerceIn(0.06f, 1f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isCommandSessionOpen) EmeraldEnclave else ElectricCyan.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("wake_word_listener_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                        if (isCommandSessionOpen) EmeraldEnclave.copy(alpha = 0.22f)
                                        else ElectricCyan.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Wake Word Listener Service",
                                    tint = if (isCommandSessionOpen) EmeraldEnclave else ElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Continuous Microphone Wake Word Service",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${wakeWordSessionState.badgeLabel} • Last Wake: $lastDetectedWakeWord",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCommandSessionOpen) EmeraldEnclave else ElectricCyan
                                )
                            }
                        }

                        Switch(
                            checked = isWakeServiceActive,
                            onCheckedChange = { enabled ->
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm || !enabled) {
                                    onToggleWakeWordListenerService(enabled)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("wake_word_service_switch")
                        )
                    }

                    // Wake Word Persona Chips: Jarvis, Janu, Friday, Edith, Ultron
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Active Wake Word Triggers (Tap to toggle):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val allPersonas = listOf("Jarvis", "Janu", "Friday", "Edith", "Ultron")
                            items(allPersonas) { persona ->
                                val isEnabledPersona = activeWakeWords.any { it.equals(persona, true) }
                                FilterChip(
                                    selected = isEnabledPersona,
                                    onClick = { onToggleWakeWordPhrase(persona) },
                                    label = {
                                        Text(
                                            text = "\"Hey $persona\"",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier.testTag("wake_word_chip_${persona.lowercase()}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldEnclave.copy(alpha = 0.22f),
                                        selectedLabelColor = EmeraldEnclave
                                    )
                                )
                            }
                        }
                    }

                    // Live Microphone Decibel (dB) Meter + 60-Second Silence Auto-Sleep Timer
                    Surface(
                        color = Color(0xFF0B101B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                Text(
                                    text = String.format(Locale.US, "Mic Acoustic Level: %.1f dB", microphoneDecibelDb),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (isCommandSessionOpen) EmeraldEnclave.copy(alpha = 0.2f) else NeuralViolet.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isCommandSessionOpen) {
                                            "Auto-Sleep in ${wakeWordSilenceRemainingSec}s"
                                        } else {
                                            "60s Auto-Sleep Armed"
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCommandSessionOpen) EmeraldEnclave else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                            .testTag("wake_word_silence_timer_badge")
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { dbProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (isCommandSessionOpen) EmeraldEnclave else ElectricCyan,
                                trackColor = ElectricCyan.copy(alpha = 0.16f)
                            )
                        }
                    }

                    // 1-Tap Wake Word + Continuous Multi-Step Trigger Buttons (for instant testing on emulators & phones)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Test Wake Word + Continuous Multi-Step Voice Session:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val testTriggers = listOf(
                                Triple("Jarvis", "Hey Jarvis: Design Sync + Tasks", "Schedule a meeting with the design team for next Tuesday afternoon, create a task for each attendee to prepare their latest mockups, and draft an agenda based on our ongoing project goals."),
                                Triple("Janu", "Hey Janu: Audit Sensitive Tasks", "Analyze my sensitive project tasks and prioritize deadlines"),
                                Triple("Friday", "Hey Friday: Morning Standup", "morning standup"),
                                Triple("Edith", "Hey Edith: Write & Run Code", "Write a Python script to calculate sprint velocity and execute it in the sandbox"),
                                Triple("Ultron", "Hey Ultron: Wake Session Only", "")
                            )
                            items(testTriggers) { (wakeWord, label, command) ->
                                OutlinedButton(
                                    onClick = { onSimulateWakeWordTrigger(wakeWord, command) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("simulate_wake_word_${wakeWord.lowercase()}")
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Hands-Free Voice Command Center & Animated Neural Orb
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Z AI Voice & Autonomous Control",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Operate phone intents & multi-step workflows with permission gates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Continuous Hands-Free Mode",
                                tint = if (isContinuousHandsFree) EmeraldEnclave else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isContinuousHandsFree,
                                onCheckedChange = { enabled ->
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm || !enabled) {
                                        onToggleHandsFree(enabled)
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.testTag("hands_free_switch")
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isListening) {
                                            listOf(ElectricCyan.copy(alpha = 0.55f), NeuralViolet.copy(alpha = 0.1f))
                                        } else {
                                            listOf(NeuralViolet.copy(alpha = 0.35f), Color.Transparent)
                                        }
                                    )
                                )
                        )
                        IconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    onToggleVoiceMic(isContinuousHandsFree)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isListening) {
                                            listOf(ElectricCyan, EmeraldEnclave)
                                        } else {
                                            listOf(NeuralViolet, ElectricCyan)
                                        }
                                    )
                                )
                                .testTag("voice_orb_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Toggle Voice Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isListening) {
                            "Listening... Say 'Morning Standup', 'Escalate Incident', or 'Optimize Schedule'"
                        } else if (liveTranscript.isNotBlank()) {
                            "Last Command: \"$liveTranscript\""
                        } else {
                            "Tap the neural orb or select a voice phrase below"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isListening) ElectricCyan else MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quickPhrases = listOf(
                            "Schedule a meeting with the design team for next Tuesday afternoon, create a task for each attendee to prepare their latest mockups, and draft an agenda based on our ongoing project goals.",
                            "Write a Python script to calculate sprint velocity and execute it in the sandbox",
                            "triage new task",
                            "escalate incident",
                            "optimize schedule and resolve conflicts"
                        )
                        items(quickPhrases) { phrase ->
                            SuggestionChip(
                                onClick = { onSubmitVoiceCommandText(phrase) },
                                label = {
                                    Text(
                                        text = if (phrase.length > 42) "\"${phrase.take(42)}...\"" else "\"$phrase\"",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("voice_chip_${phrase.take(16).replace(" ", "_")}")
                            )
                        }
                    }

                    // Featured Multi-Step Voice Command Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSubmitVoiceCommandText(
                                    "Schedule a meeting with the design team for next Tuesday afternoon, create a task for each attendee to prepare their latest mockups, and draft an agenda based on our ongoing project goals."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compound_voice_design_meeting_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Voice: Design Sync + Tasks + Agenda",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onSubmitVoiceCommandText(
                                    "Write a Python script to calculate sprint velocity and execute it in the sandbox"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compound_voice_code_exec_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Voice: Write & Run Python Code",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeuralViolet
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            placeholder = {
                                Text(
                                    "Speak or type multi-step command (schedule, attendee tasks, agenda, or code)...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_command_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                onSubmitVoiceCommandText(commandInput)
                                commandInput = ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricCyan)
                                .testTag("submit_voice_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Execute Command",
                                tint = Color(0xFF042F2E)
                            )
                        }
                    }
                }
            }
        }

        // 2. Live Multi-Step Autonomous Workflow Pipeline Telemetry
        item {
            AnimatedVisibility(visible = activeMacroSteps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("active_macro_pipeline_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pipeline: ${activeMacroTitle ?: "Autonomous Macro"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "${activeMacroSteps.count { it.isCompleted }}/${activeMacroSteps.size} Steps",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldEnclave
                            )
                        }

                        activeMacroSteps.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (step.isCompleted) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = step.label,
                                    tint = if (step.isCompleted) EmeraldEnclave else ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${step.stepIndex}. ${step.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = step.outputNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2b. Drafted Meeting Agenda Card (from Multi-Step Voice Command)
        item {
            AnimatedVisibility(visible = draftedMeetingAgenda.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldEnclave.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("drafted_meeting_agenda_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Voice-Drafted Meeting Agenda (Project Goals Aligned)",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldEnclave
                        )
                        Text(
                            text = draftedMeetingAgenda,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2c. Voice-Synthesized & Executed Code Output Card
        item {
            AnimatedVisibility(visible = lastVoiceCodeOutput.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeuralViolet.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .testTag("voice_code_execution_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Voice-Synthesized & Executed Code",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            TextButton(onClick = onOpenCodeStudio) {
                                Text("Open in Code Studio")
                            }
                        }
                        if (lastVoiceCodeSnippet.isNotBlank()) {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = lastVoiceCodeSnippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Text(
                            text = lastVoiceCodeOutput,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldEnclave
                        )
                    }
                }
            }
        }

        // 3. Accessibility & Zero-Trust High-Risk Security Firewall Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        EmeraldEnclave.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Zero-Trust Security Firewall",
                                tint = EmeraldEnclave
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Security Firewall & Accessibility Bridge",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "No model can execute high-risk actions without your approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require User Permission for High-Risk Ops",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Gates Phone Calls, Emails, Code Sandbox & Web Search",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = requireApprovalForHighRisk,
                            onCheckedChange = onToggleHighRiskFirewall,
                            modifier = Modifier.testTag("high_risk_firewall_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = "Accessibility Permission",
                                tint = if (isAccessibilityBridgeEnabled) EmeraldEnclave else ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Android Accessibility & Device Automation",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (isAccessibilityBridgeEnabled) {
                                        "Status: GRANTED (Admin Consent Active)"
                                    } else {
                                        "Status: Tap to grant Accessibility Permission"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAccessibilityBridgeEnabled) EmeraldEnclave else AmberConflict
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.testTag("accessibility_permission_button")
                        ) {
                            Text(
                                text = if (isAccessibilityBridgeEnabled) "Configure" else "Grant Access",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Recent Security Audit Log Preview
                    if (securityAuditLogs.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Recent Security Audit Log (${securityAuditLogs.size} events):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                securityAuditLogs.take(3).forEach { log ->
                                    val decColor = when (log.decision) {
                                        "APPROVED" -> EmeraldEnclave
                                        "DENIED_BY_USER" -> CrimsonAlert
                                        else -> AmberConflict
                                    }
                                    Text(
                                        text = "[${log.timeFormatted}] ${log.decision} • ${log.actionCategory}: ${log.targetSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = decColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Universal Z AI System Prompt & Custom Skills Registry
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Z AI Custom Skills",
                                tint = NeuralViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Z AI Persona & Custom Skills Matrix",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Injected into every loaded Hugging Face model",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { showSystemPromptPreview = !showSystemPromptPreview },
                                modifier = Modifier.testTag("toggle_system_prompt_button")
                            ) {
                                Text(if (showSystemPromptPreview) "Hide Prompt" else "Prompt")
                            }
                            OutlinedButton(
                                onClick = { showAddSkillDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("add_custom_skill_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Skill", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Skill", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSystemPromptPreview) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = universalSystemPrompt,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    customSkills.forEach { skill ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = skill.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (skill.requiresApprovalGate) {
                                            Surface(
                                                color = AmberConflict.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "GATED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AmberConflict,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = skill.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = skill.isEnabled,
                                    onCheckedChange = { onToggleCustomSkill(skill.id) },
                                    modifier = Modifier.testTag("skill_switch_${skill.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Direct Phone Operation Deck (Permission-Gated Native Android System Intents)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Direct Phone Operation Bridge (Permission-Gated)",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onDirectPhoneCall("+15550192834") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intent_call_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Dial On-Call", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dial Lead", style = MaterialTheme.typography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = { onDirectEmail("sec-ops@zai.local") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intent_email_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Send Status Email", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Email Report", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onDirectAlarm(9, 0) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intent_alarm_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = "Set System Alarm", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Alarm", style = MaterialTheme.typography.labelLarge)
                    }
                    FilledTonalButton(
                        onClick = onDirectSystemCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intent_calendar_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = "Open System Calendar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OS Calendar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 6. Event-Driven Multi-Step Autonomous Workflow Automation Header + Event Simulator Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeuralViolet.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .testTag("event_workflow_engine_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Autonomous Workflows",
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Event-Driven Autonomous Workflows",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-triggers on New Task, Meeting Scheduled, or Approaching Deadline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { showCreateMacroDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF042F2E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_create_macro_dialog_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Workflow", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Workflow", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Simulate Live Event Trigger (Test Autonomous Multi-Step Execution):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val simEvents = listOf(
                            Triple("ON_TASK_ADDED", "⚡ Trigger: New Task Added", "Deploy Enclave Patch v2.4"),
                            Triple("ON_MEETING_SCHEDULED", "📅 Trigger: Meeting Scheduled", "Architecture Sync"),
                            Triple("ON_DEADLINE_APPROACHING", "⏰ Trigger: Deadline ≤12h", "Audit Zero-Trust Keys (6h)")
                        )
                        items(simEvents) { (code, label, contextTitle) ->
                            OutlinedButton(
                                onClick = { onSimulateWorkflowEvent(code, contextTitle) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("simulate_workflow_event_${code.lowercase()}")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }

        items(macros, key = { "macro_${it.id}" }) { macro ->
            WorkflowMacroCard(
                macro = macro,
                onRun = { onRunMacro(macro) },
                onToggleAutoTrigger = { onToggleMacroAutoTrigger(macro) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showCreateMacroDialog) {
        CreateMacroDialog(
            onDismiss = { showCreateMacroDialog = false },
            onConfirm = { title, triggerPhrase, desc, steps, contact, eventType, autoEnabled ->
                onCreateCustomMacro(title, triggerPhrase, desc, steps, contact, eventType, autoEnabled)
                showCreateMacroDialog = false
            }
        )
    }

    if (showAddSkillDialog) {
        AddCustomSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onConfirm = { name, desc, instr, gated ->
                onAddCustomSkill(name, desc, instr, gated)
                showAddSkillDialog = false
            }
        )
    }
}

@Composable
private fun WorkflowMacroCard(
    macro: WorkflowMacroEntity,
    onRun: () -> Unit,
    onToggleAutoTrigger: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_card_${macro.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = macro.triggerEventDisplayLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = NeuralViolet.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "\"${macro.triggerPhrase}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDDD6FE),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (macro.isAutoTriggerEnabled) "Auto ON" else "Manual",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (macro.isAutoTriggerEnabled) EmeraldEnclave else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = macro.isAutoTriggerEnabled,
                        onCheckedChange = { onToggleAutoTrigger() },
                        modifier = Modifier.testTag("macro_auto_switch_${macro.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = macro.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Runs: ${macro.runCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldEnclave
                )
            }

            Text(
                text = macro.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val steps = macro.stepsList()
                items(steps.size) { index ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${steps[index].substringBefore(":")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = macro.lastRunSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRun,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier.testTag("run_macro_button_${macro.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Execute Workflow", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Execute", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddCustomSkillDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("Express capabilities clearly and assist with project architecture.") }
    var requiresGate by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Z AI Skill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill Name (e.g. 'PR Security Auditor')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_skill_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What this skill enables") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("System Prompt Instruction for Loaded Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require User Permission Gate", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = requiresGate, onCheckedChange = { requiresGate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, instruction, requiresGate) },
                modifier = Modifier.testTag("confirm_add_skill_button")
            ) {
                Text("Inject Skill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateMacroDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var triggerPhrase by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetContact by remember { mutableStateOf("team@zai.local") }
    var selectedEventTrigger by remember { mutableStateOf(WorkflowTriggerEventType.ON_TASK_ADDED) }
    var isAutoTriggerEnabled by remember { mutableStateOf(true) }

    var stepSendNotification by remember { mutableStateOf(true) }
    var stepUpdateTaskStatus by remember { mutableStateOf(true) }
    var stepRescheduleEvents by remember { mutableStateOf(true) }
    var stepStartCountdownTimer by remember { mutableStateOf(false) }
    var stepSpeakBriefing by remember { mutableStateOf(true) }
    var stepEmailOrCall by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Define Autonomous Multi-Step Workflow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workflow Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("macro_title_input")
                )
                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    label = { Text("Voice Trigger Phrase (e.g. 'night deploy')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("macro_trigger_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Triggering Event:",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WorkflowTriggerEventType.entries) { eventType ->
                        FilterChip(
                            selected = selectedEventTrigger == eventType,
                            onClick = { selectedEventTrigger = eventType },
                            label = {
                                Text(eventType.label, style = MaterialTheme.typography.labelSmall)
                            },
                            modifier = Modifier.testTag("trigger_event_chip_${eventType.code.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Autonomous Multi-Step Actions:",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldEnclave,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WorkflowActionStepType.SEND_NOTIFICATION.label, style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepSendNotification, onCheckedChange = { stepSendNotification = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WorkflowActionStepType.UPDATE_TASK_STATUS.label, style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepUpdateTaskStatus, onCheckedChange = { stepUpdateTaskStatus = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WorkflowActionStepType.RESCHEDULE_EVENTS.label, style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepRescheduleEvents, onCheckedChange = { stepRescheduleEvents = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WorkflowActionStepType.CREATE_COUNTDOWN_TIMER.label, style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepStartCountdownTimer, onCheckedChange = { stepStartCountdownTimer = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(WorkflowActionStepType.SPEAK_BRIEFING.label, style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepSpeakBriefing, onCheckedChange = { stepSpeakBriefing = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger Email/Call Intent", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = stepEmailOrCall, onCheckedChange = { stepEmailOrCall = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = mutableListOf<String>()
                    if (stepSendNotification) steps.add("SEND_NOTIFICATION")
                    if (stepUpdateTaskStatus) steps.add("UPDATE_TASK_STATUS")
                    if (stepRescheduleEvents) steps.add("RESCHEDULE_EVENTS")
                    if (stepStartCountdownTimer) steps.add("CREATE_COUNTDOWN_TIMER")
                    if (stepEmailOrCall) {
                        if (targetContact.contains("@")) {
                            steps.add("SEND_EMAIL:$targetContact")
                        } else {
                            steps.add("DIAL_NUMBER:$targetContact")
                        }
                    }
                    if (stepSpeakBriefing || steps.isEmpty()) steps.add("SPEAK_BRIEFING")
                    onConfirm(
                        title,
                        triggerPhrase,
                        description,
                        steps.joinToString("|"),
                        targetContact,
                        selectedEventTrigger.code,
                        isAutoTriggerEnabled
                    )
                },
                modifier = Modifier.testTag("confirm_create_macro_button")
            ) {
                Text("Save Workflow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

