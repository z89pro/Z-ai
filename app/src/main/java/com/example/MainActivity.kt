package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OmniDestination
import com.example.ui.OmniWorkspaceViewModel
import com.example.ui.PendingSecurityRequest
import com.example.ui.screens.CodeStudioScreen
import com.example.ui.screens.HfModelsScreen
import com.example.ui.screens.HomeWorkspaceScreen
import com.example.ui.screens.VoiceAndMacrosScreen
import com.example.ui.screens.WorkspaceHubScreen
import com.example.ui.screens.ZAIChatScreen
import com.example.ui.screens.ZaiSettingsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.LocalZaiTokens
import com.example.ui.theme.OmniWorkspaceTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: OmniWorkspaceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OmniWorkspaceTheme(
                darkTheme = uiState.isDarkMode,
                themeMode = uiState.themeMode
            ) {
                ZAIWorkspaceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ZAIWorkspaceApp(viewModel: OmniWorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val snippets by viewModel.snippets.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val tokens = LocalZaiTokens.current

    var showDrawerDialog by remember { mutableStateOf(false) }

    // System Back returns to the Home Screen when on any secondary tab
    if (uiState.currentTab != OmniDestination.HOME) {
        BackHandler {
            viewModel.selectTab(OmniDestination.HOME)
        }
    }

    // Auto-dismiss status toast after 3.5 seconds
    LaunchedEffect(uiState.statusToastMessage) {
        if (uiState.statusToastMessage != null) {
            delay(3500)
            viewModel.clearToast()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.mainBackground)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            containerColor = tokens.mainBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                ZaiExactTopHeaderBar(
                    isModelOnline = true,
                    activeModelName = activeModel?.displayName ?: "Gemma 2B",
                    themeMode = uiState.themeMode,
                    onClickHamburger = { showDrawerDialog = true },
                    onClickModelOnlinePill = { viewModel.selectTab(OmniDestination.HF_MODELS) },
                    onClickNewChatCompose = {
                        viewModel.startNewChatSession()
                        viewModel.selectTab(OmniDestination.ZAI_CHAT)
                    },
                    onSelectThemeMode = { viewModel.setThemeMode(it) },
                    onOpenVoiceAssistant = { viewModel.selectTab(OmniDestination.VOICE_AND_MACROS) },
                    onOpenSettings = { viewModel.selectTab(OmniDestination.SETTINGS) }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    ZaiExactFiveItemBottomBar(
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.mainBackground)
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    ZAINavigationRail(
                        currentTab = uiState.currentTab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (uiState.currentTab) {
                        OmniDestination.HOME -> {
                            HomeWorkspaceScreen(
                                activeModel = activeModel,
                                models = models,
                                isGenerating = uiState.isAgentBuildingProject || uiState.isSearchingWeb,
                                isListeningVoice = uiState.isListeningVoice,
                                liveVoiceTranscript = uiState.liveVoiceTranscript,
                                onSendPromptFromHome = { prompt ->
                                    viewModel.sendPromptFromHome(prompt)
                                },
                                onStopGeneration = {
                                    viewModel.cancelAgentTask()
                                },
                                onSelectModel = { selectedModel ->
                                    viewModel.setActiveLocalModel(selectedModel)
                                },
                                onToggleVoiceMic = {
                                    viewModel.toggleVoiceRecognition(!uiState.isListeningVoice)
                                },
                                onOpenAiChat = {
                                    viewModel.selectTab(OmniDestination.ZAI_CHAT)
                                },
                                onOpenTaskManager = {
                                    viewModel.selectTab(OmniDestination.WORKSPACE_HUB)
                                },
                                onOpenCodeStudio = {
                                    viewModel.selectTab(OmniDestination.CODE_STUDIO)
                                },
                                onOpenHfModelAdvisor = {
                                    viewModel.rescanDeviceHardware()
                                    viewModel.selectTab(OmniDestination.HF_MODELS)
                                },
                                onOpenHfModelHub = {
                                    viewModel.selectTab(OmniDestination.HF_MODELS)
                                },
                                onOpenVoiceAssistant = {
                                    viewModel.selectTab(OmniDestination.VOICE_AND_MACROS)
                                }
                            )
                        }

                        OmniDestination.WORKSPACE_HUB -> {
                            WorkspaceHubScreen(
                                tasks = tasks,
                                events = events,
                                activeModel = activeModel,
                                aiBriefing = uiState.aiBriefingBanner,
                                suggestedSlots = uiState.suggestedMeetingSlots,
                                isWebSearchEnabled = uiState.isWebSearchEnabled,
                                isSearchingWeb = uiState.isSearchingWeb,
                                webCitations = uiState.webSearchCitations,
                                countdownReminders = uiState.countdownReminders,
                                activeReminderAlertMessage = uiState.activeReminderAlertMessage,
                                sensitiveAuditReport = uiState.sensitiveAuditReport,
                                onExecuteSensitiveInsightAction = { viewModel.executeSensitiveInsightAction(it) },
                                onAddCountdownReminder = { title, cat, preset, freq ->
                                    viewModel.addCountdownReminder(title, cat, preset, freq)
                                },
                                onTogglePauseReminder = { viewModel.togglePauseCountdownReminder(it) },
                                onTriggerReminderAlertNow = { viewModel.triggerReminderAlertImmediately(it) },
                                onSnoozeReminderFiveMin = { viewModel.snoozeActiveReminderFiveMinutes() },
                                onDismissReminderAlert = { viewModel.dismissActiveReminderAlert() },
                                onDeleteReminder = { viewModel.deleteCountdownReminder(it) },
                                onToggleWebSearch = { viewModel.toggleWebSearchCapability(it) },
                                onSearchWebNow = { viewModel.triggerWebSearchWithPermission(it) },
                                onOpenCitationUrl = { viewModel.openCitationUrl(it) },
                                onRunAutoScheduler = { viewModel.runSmartAutoScheduler() },
                                onRunPrioritizeOnly = { viewModel.runAutoPrioritizeTasksOnly() },
                                onBookSuggestedSlot = { viewModel.bookSuggestedMeetingSlot(it) },
                                onAskLocalAi = { viewModel.refreshLocalBriefing(it) },
                                onAdvanceTaskStatus = { viewModel.advanceTaskStatus(it) },
                                onCycleTaskImportance = { t, stars, hrs ->
                                    viewModel.updateTaskImportanceAndDeadline(t, stars, hrs)
                                },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onCreateTask = { title, desc, tag, prio, mins, sens, deadH, stars ->
                                    viewModel.createNewTask(title, desc, tag, prio, mins, sens, deadH, stars)
                                },
                                onCreateEvent = { title, h, m, dur, att, cat ->
                                    viewModel.createNewCalendarEvent(title, h, m, dur, att, cat)
                                },
                                onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                                onSyncSystemCalendar = { viewModel.triggerDirectSystemCalendar() }
                            )
                        }

                        OmniDestination.ZAI_CHAT -> {
                            ZAIChatScreen(
                                messages = uiState.chatMessages,
                                activeModel = activeModel,
                                isWebSearchEnabled = uiState.isWebSearchEnabled,
                                isSearchingWeb = uiState.isSearchingWeb,
                                isListeningVoice = uiState.isListeningVoice,
                                isLiveAiCallActive = uiState.isLiveAiCallActive,
                                liveCallDurationSeconds = uiState.liveCallDurationSeconds,
                                isLiveCallMuted = uiState.isLiveCallMuted,
                                liveCallStatusText = uiState.liveCallStatusText,
                                kvCacheStats = uiState.kvCacheStats,
                                currentDocumentReport = uiState.currentDocumentReport,
                                onAnalyzeDocumentFile = { fileName, content ->
                                    viewModel.analyzeDocumentFile(fileName, content)
                                },
                                onCreateTasksFromDocument = { report ->
                                    viewModel.createTasksFromDocumentReport(report)
                                },
                                onDismissDocumentReport = {
                                    viewModel.dismissCurrentDocumentReport()
                                },
                                onSendChatMessage = { viewModel.sendChatMessage(it) },
                                onStartNewChat = { viewModel.startNewChatSession() },
                                onToggleWebSearch = { viewModel.toggleWebSearchCapability(it) },
                                onToggleVoiceMic = { viewModel.toggleVoiceRecognition(true) },
                                onToggleLiveAiCall = { viewModel.toggleLiveAiCall() },
                                onToggleLiveCallMute = { viewModel.toggleLiveCallMute() },
                                onDialRealPhoneNumber = { viewModel.triggerDirectPhoneCall(it) },
                                onOpenPermissionsCenter = { viewModel.selectTab(OmniDestination.VOICE_AND_MACROS) },
                                onSpeakMessageAloud = { viewModel.speakChatText(it) },
                                onOpenCitationUrl = { viewModel.openCitationUrl(it) },
                                onNavigateToCodeStudio = { viewModel.selectTab(OmniDestination.CODE_STUDIO) },
                                onNavigateToWorkspaceReminders = { viewModel.selectTab(OmniDestination.WORKSPACE_HUB) }
                            )
                        }

                        OmniDestination.VOICE_AND_MACROS -> {
                            VoiceAndMacrosScreen(
                                macros = macros,
                                customSkills = uiState.customSkills,
                                universalSystemPrompt = uiState.universalSystemPrompt,
                                requireApprovalForHighRisk = uiState.requireApprovalForHighRisk,
                                isAccessibilityBridgeEnabled = uiState.isAccessibilityBridgeEnabled,
                                securityAuditLogs = uiState.securityAuditLogs,
                                isListening = uiState.isListeningVoice,
                                isContinuousHandsFree = uiState.isContinuousHandsFree,
                                liveTranscript = uiState.liveVoiceTranscript,
                                activeMacroTitle = uiState.activeMacroRunningTitle,
                                activeMacroSteps = uiState.activeMacroSteps,
                                draftedMeetingAgenda = uiState.draftedMeetingAgenda,
                                lastVoiceCodeSnippet = uiState.lastVoiceCodeSnippet,
                                lastVoiceCodeOutput = uiState.lastVoiceCodeOutput,
                                wakeWordSessionState = uiState.wakeWordSessionState,
                                activeWakeWords = uiState.activeWakeWords,
                                lastDetectedWakeWord = uiState.lastDetectedWakeWord,
                                microphoneDecibelDb = uiState.microphoneDecibelDb,
                                wakeWordSilenceRemainingSec = uiState.wakeWordSilenceRemainingSec,
                                onToggleWakeWordListenerService = { viewModel.toggleWakeWordListenerService(it) },
                                onToggleWakeWordPhrase = { viewModel.toggleWakeWordPhrase(it) },
                                onSimulateWakeWordTrigger = { wake, cmd ->
                                    viewModel.simulateWakeWordTrigger(wake, cmd)
                                },
                                onToggleVoiceMic = { viewModel.toggleVoiceRecognition(it) },
                                onToggleHandsFree = { viewModel.setContinuousHandsFree(it) },
                                onSubmitVoiceCommandText = { viewModel.processSpokenOrTypedVoiceCommand(it) },
                                onOpenCodeStudio = { viewModel.selectTab(OmniDestination.CODE_STUDIO) },
                                onRunMacro = { viewModel.executeWorkflowMacro(it) },
                                onCreateCustomMacro = { title, trig, desc, steps, contact, eventType, autoEnabled ->
                                    viewModel.createCustomMacro(title, trig, desc, steps, contact, eventType, autoEnabled)
                                },
                                onToggleMacroAutoTrigger = { viewModel.toggleMacroAutoTrigger(it) },
                                onSimulateWorkflowEvent = { evType, ctxTitle ->
                                    viewModel.triggerAutonomousWorkflowsForEvent(evType, ctxTitle)
                                },
                                onToggleCustomSkill = { viewModel.toggleCustomSkill(it) },
                                onAddCustomSkill = { name, desc, instr, gated ->
                                    viewModel.addCustomSkill(name, desc, instr, gated)
                                },
                                onToggleHighRiskFirewall = { viewModel.toggleHighRiskApprovalFirewall(it) },
                                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettingsAndToggleBridge() },
                                onDirectPhoneCall = { viewModel.triggerDirectPhoneCall(it) },
                                onDirectEmail = { viewModel.triggerDirectEmail(it) },
                                onDirectAlarm = { h, m -> viewModel.triggerDirectAlarm(h, m) },
                                onDirectSystemCalendar = { viewModel.triggerDirectSystemCalendar() }
                            )
                        }

                        OmniDestination.CODE_STUDIO -> {
                            CodeStudioScreen(
                                snippets = snippets,
                                selectedSnippetId = uiState.selectedSnippetId,
                                editorTitle = uiState.editorTitle,
                                editorLanguage = uiState.editorLanguage,
                                editorCode = uiState.editorCodeBuffer,
                                consoleOutput = uiState.editorConsoleOutput,
                                aiPrompt = uiState.aiCodePrompt,
                                activeModel = activeModel,
                                agentProjects = uiState.agentProjects,
                                activeAgentProject = uiState.activeAgentProject,
                                selectedProjectFilePath = uiState.selectedProjectFilePath,
                                recentCodeFiles = uiState.recentCodeFiles,
                                isAgentBuildingProject = uiState.isAgentBuildingProject,
                                agentBuildSteps = uiState.agentBuildSteps,
                                codingChatTurns = uiState.codingChatTurns,
                                agentPermissionConfig = uiState.agentPermissionConfig,
                                agentExecutionEvents = uiState.agentExecutionEvents,
                                latestDiffLines = uiState.latestDiffLines,
                                workspaceCheckpoints = uiState.workspaceCheckpoints,
                                terminalHistory = uiState.terminalHistory,
                                lastBuildReport = uiState.lastBuildReport,
                                modelCapabilities = uiState.modelCapabilities,
                                onSelectAgentProject = { viewModel.selectAgentProject(it) },
                                onSelectProjectFile = { viewModel.selectProjectVirtualFile(it) },
                                onOpenRecentFile = { viewModel.openRecentCodeFile(it) },
                                onClearRecentFiles = { viewModel.clearRecentCodeFiles() },
                                onAddNewProjectFile = { path, lang, content ->
                                    viewModel.addNewVirtualProjectFile(path, lang, content)
                                },
                                onDeleteProjectFile = { viewModel.deleteVirtualProjectFile(it) },
                                onRenameProjectFile = { oldP, newP ->
                                    viewModel.renameVirtualProjectFile(oldP, newP)
                                },
                                onBuildAutonomousProject = { prompt, pType ->
                                    viewModel.buildAutonomousProjectWithAgent(prompt, pType)
                                },
                                onCancelAgentTask = { viewModel.cancelAgentTask() },
                                onFixProjectBugsWithAgent = { viewModel.fixProjectBugsWithAgent() },
                                onRestoreCheckpoint = { viewModel.restoreWorkspaceCheckpoint(it) },
                                onExecuteTerminalCommand = { viewModel.executeWorkspaceTerminalCommand(it) },
                                onStopTerminalProcess = { viewModel.stopWorkspaceTerminalProcess() },
                                onImportProjectFromBrowserFiles = { projName, vFiles ->
                                    viewModel.importPersonalProjectFromBrowser(projName, vFiles)
                                },
                                onImportProjectFromLink = { viewModel.importPersonalProjectFromLink(it) },
                                onUpdateAgentPermissionConfig = { viewModel.updateCodingAgentPermissions(it) },
                                onBuildProjectZipBytes = { viewModel.buildActiveProjectZipBytes() },
                                onBuildProjectHtmlBytes = { viewModel.buildActiveProjectHtmlBytes() },
                                onNotifyExportSuccess = { viewModel.notifyProjectExportSuccess(it) },
                                onSelectSnippet = { viewModel.selectCodeSnippet(it) },
                                onLanguageChange = { viewModel.updateEditorLanguage(it) },
                                onCodeChange = { viewModel.updateEditorCode(it) },
                                onAiPromptChange = { viewModel.updateAiCodePrompt(it) },
                                onGenerateCodeWithAi = { viewModel.generateCodeWithLocalAi() },
                                onRunCodeInSandbox = { viewModel.runCurrentCodeInSandbox() },
                                onSaveNewSnippet = { viewModel.saveAsNewCodeSnippet(it) }
                            )
                        }

                        OmniDestination.HF_MODELS -> {
                            HfModelsScreen(
                                models = models,
                                activeModel = activeModel,
                                deviceHardware = uiState.deviceHardware,
                                hfSearchQuery = uiState.hfSearchQuery,
                                isSearchingHub = uiState.isSearchingHfHub,
                                hubSearchResults = uiState.hfHubResults,
                                downloadingModelId = uiState.downloadingModelId,
                                downloadProgress = uiState.downloadProgressPercent,
                                downloadStatusNote = uiState.downloadStatusNote,
                                onEvaluateHardwareFit = { sizeMb, quant ->
                                    viewModel.evaluateModelHardwareFit(sizeMb, quant)
                                },
                                onRescanHardware = { viewModel.rescanDeviceHardware() },
                                onSearchQueryChange = { viewModel.updateHfSearchQuery(it) },
                                onSearchHfHub = { viewModel.searchHuggingFaceHub(it) },
                                onActivateOrDownloadModel = { viewModel.setActiveLocalModel(it) },
                                onDownloadHubResult = { viewModel.downloadSearchResultModel(it) },
                                onAddCustomHfModel = { repo, name, quant, fmt, size, spec ->
                                    viewModel.addCustomHuggingFaceModel(repo, name, quant, fmt, size, spec)
                                },
                                onCancelDownload = { viewModel.cancelModelDownload() },
                                onDeleteModel = { viewModel.deleteDownloadedModel(it) },
                                onImportLocalModelUri = { uri, fileName ->
                                    viewModel.importLocalModelFromUri(uri, fileName)
                                }
                            )
                        }

                        OmniDestination.SETTINGS -> {
                            ZaiSettingsScreen(
                                themeMode = uiState.themeMode,
                                activeModel = activeModel,
                                modelCapabilities = uiState.modelCapabilities,
                                deviceHardware = uiState.deviceHardware,
                                accelerationConfig = uiState.accelerationConfig,
                                kvCacheStats = uiState.kvCacheStats,
                                lastBenchmarkResult = uiState.lastBenchmarkResult,
                                isBenchmarkingAccelerator = uiState.isBenchmarkingAccelerator,
                                isStrictOfflineEnclave = uiState.isStrictOfflineEnclave,
                                requireApprovalForHighRisk = uiState.requireApprovalForHighRisk,
                                isWakeWordServiceEnabled = uiState.isContinuousHandsFree,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
                                onToggleHardwareAcceleration = { viewModel.toggleHardwareAcceleration(it) },
                                onSelectAcceleratorDelegate = { viewModel.setHardwareAccelerator(it) },
                                onToggleFp16Relaxation = { viewModel.toggleFp16Relaxation(it) },
                                onToggleKvCacheReuse = { viewModel.toggleKvCacheReuse(it) },
                                onClearKvCache = { viewModel.clearKvCache() },
                                onRunAcceleratorBenchmark = { viewModel.runHardwareAcceleratorBenchmark() },
                                onToggleStrictOfflineEnclave = { viewModel.toggleStrictOfflineEnclave() },
                                onToggleHighRiskFirewall = { viewModel.toggleHighRiskApprovalFirewall(it) },
                                onToggleWakeWordService = { viewModel.toggleWakeWordListenerService(it) },
                                onOpenHfModelAdvisor = { viewModel.selectTab(OmniDestination.HF_MODELS) },
                                onOpenHfModelHub = { viewModel.selectTab(OmniDestination.HF_MODELS) },
                                onOpenVoiceAssistant = { viewModel.selectTab(OmniDestination.VOICE_AND_MACROS) }
                            )
                        }
                    }

                    // Floating Toast Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.statusToastMessage != null,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Surface(
                            color = tokens.headerSurface,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .border(1.dp, tokens.primaryCyan, RoundedCornerShape(14.dp))
                                .testTag("status_toast_banner")
                        ) {
                            Text(
                                text = uiState.statusToastMessage.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.primaryText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Navigation Drawer Modal (Opened via Hamburger Menu Button)
        if (showDrawerDialog) {
            AlertDialog(
                onDismissRequest = { showDrawerDialog = false },
                containerColor = tokens.headerSurface,
                titleContentColor = tokens.primaryText,
                textContentColor = tokens.secondaryText,
                title = {
                    Text("Z AI Navigation & Modules", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val navChoices = listOf(
                            "Home Workspace" to OmniDestination.HOME,
                            "AI Chat" to OmniDestination.ZAI_CHAT,
                            "Code Studio (Autonomous IDE)" to OmniDestination.CODE_STUDIO,
                            "Task Manager & Reminders" to OmniDestination.WORKSPACE_HUB,
                            "HF Model Advisor & Hub" to OmniDestination.HF_MODELS,
                            "Voice Assistant & Wake Words" to OmniDestination.VOICE_AND_MACROS,
                            "Workspace Settings & Theme" to OmniDestination.SETTINGS
                        )
                        navChoices.forEach { (label, dest) ->
                            Surface(
                                color = if (uiState.currentTab == dest) tokens.welcomeCardTop else tokens.featureCardSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectTab(dest)
                                        showDrawerDialog = false
                                    }
                            ) {
                                Text(
                                    text = label,
                                    color = tokens.primaryText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDrawerDialog = false }) {
                        Text("Close", color = tokens.primaryCyan)
                    }
                }
            )
        }

        // Zero-Trust High-Risk Action Security Permission Gate Modal
        val pendingRequest = uiState.pendingSecurityRequest
        if (pendingRequest != null) {
            ZeroTrustPermissionGateDialog(
                request = pendingRequest,
                onApprove = { viewModel.approvePendingSecurityRequest() },
                onDeny = { viewModel.denyPendingSecurityRequest() }
            )
        }
    }
}

/**
 * 3. TOP APPLICATION HEADER (Exact Left-to-Right Order):
 * 1. Hamburger menu button (dark navy rounded-square surface)
 * 2. Circular cyan AI brain logo (glowing cyan brain symbol inside dark blue circular container)
 * 3. Application title: "Z AI" (bold white typography)
 * 4. Green online-status indicator with text "Model Online" (subtle dark-green pill)
 * 5. New Chat / Compose button (white/cyan pencil icon inside rounded dark surface)
 * 6. Vertical three-dot More Options button
 */
@Composable
private fun ZaiExactTopHeaderBar(
    isModelOnline: Boolean,
    activeModelName: String,
    themeMode: AppThemeMode,
    onClickHamburger: () -> Unit,
    onClickModelOnlinePill: () -> Unit,
    onClickNewChatCompose: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tokens = LocalZaiTokens.current
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.headerSurface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left group: 1. Hamburger + 2. Circular Cyan Brain Logo + 3. "Z AI"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Hamburger menu button: dark navy rounded-square surface
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tokens.featureCardSurface)
                        .border(1.dp, tokens.border, RoundedCornerShape(11.dp))
                        .clickable { onClickHamburger() }
                        .testTag("header_hamburger_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open menu",
                        tint = tokens.primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Circular cyan AI brain logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tokens.welcomeCardTop)
                        .border(1.dp, tokens.primaryCyan.copy(alpha = 0.55f), CircleShape)
                        .testTag("header_ai_brain_logo")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = "Z AI Brain Logo",
                        tint = tokens.primaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. Application title: "Z AI"
                Text(
                    text = "Z AI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.primaryText,
                    modifier = Modifier.testTag("header_zai_title")
                )
            }

            // Right group: 4. Green "Model Online" pill + 5. Compose button + 6. Three-dot More Options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4. Green online-status indicator with text "Model Online"
                Surface(
                    color = tokens.successGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = tokens.successGreen.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onClickModelOnlinePill() }
                        .testTag("top_bar_model_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(tokens.successGreen)
                        )
                        Text(
                            text = if (isModelOnline) "Model Online" else activeModelName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.successGreen
                        )
                    }
                }

                // 5. New Chat / Compose button: white/cyan pencil icon inside rounded dark surface
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.featureCardSurface)
                        .border(1.dp, tokens.border, RoundedCornerShape(10.dp))
                        .clickable { onClickNewChatCompose() }
                        .testTag("header_compose_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Chat / Compose",
                        tint = tokens.primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 6. Vertical three-dot More Options button
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showMoreMenu = true }
                            .testTag("header_more_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = tokens.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Theme: Dark Mode ${if (themeMode == AppThemeMode.DARK) "✓" else ""}") },
                            onClick = {
                                onSelectThemeMode(AppThemeMode.DARK)
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Theme: Light Mode ${if (themeMode == AppThemeMode.LIGHT) "✓" else ""}") },
                            onClick = {
                                onSelectThemeMode(AppThemeMode.LIGHT)
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Theme: System Default ${if (themeMode == AppThemeMode.SYSTEM) "✓" else ""}") },
                            onClick = {
                                onSelectThemeMode(AppThemeMode.SYSTEM)
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voice Assistant & Wake Words") },
                            onClick = {
                                showMoreMenu = false
                                onOpenVoiceAssistant()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Workspace Settings") },
                            onClick = {
                                showMoreMenu = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = tokens.border.copy(alpha = 0.65f))
    }
}

/**
 * 7. BOTTOM NAVIGATION BAR (Exact 5 Equally Distributed Items, Left to Right):
 * 1. Home
 * 2. Chat
 * 3. Code Studio
 * 4. Tasks
 * 5. Settings
 */
@Composable
private fun ZaiExactFiveItemBottomBar(
    currentTab: OmniDestination,
    onSelectTab: (OmniDestination) -> Unit
) {
    val tokens = LocalZaiTokens.current

    data class BottomNavItemSpec(
        val destination: OmniDestination,
        val label: String,
        val icon: ImageVector,
        val tagSuffix: String
    )

    val items = listOf(
        BottomNavItemSpec(OmniDestination.HOME, "Home", Icons.Outlined.Home, "home"),
        BottomNavItemSpec(OmniDestination.ZAI_CHAT, "Chat", Icons.Outlined.ChatBubbleOutline, "zai_chat"),
        BottomNavItemSpec(OmniDestination.CODE_STUDIO, "Code Studio", Icons.Outlined.Code, "code_studio"),
        BottomNavItemSpec(OmniDestination.WORKSPACE_HUB, "Tasks", Icons.Outlined.Layers, "workspace_hub"),
        BottomNavItemSpec(OmniDestination.SETTINGS, "Settings", Icons.Outlined.Settings, "settings")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.headerSurface)
            .navigationBarsPadding()
            .testTag("zai_bottom_navigation_bar")
    ) {
        HorizontalDivider(thickness = 1.dp, color = tokens.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentTab == item.destination
                val itemTint = if (isSelected) tokens.primaryCyan else tokens.secondaryText

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectTab(item.destination) }
                        .testTag("nav_tab_${item.tagSuffix}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) tokens.primaryCyan.copy(alpha = 0.14f) else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = itemTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemTint,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Thin cyan indicator line near the bottom when selected
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) tokens.primaryCyan else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZAINavigationRail(
    currentTab: OmniDestination,
    onSelectTab: (OmniDestination) -> Unit
) {
    val tokens = LocalZaiTokens.current
    NavigationRail(
        containerColor = tokens.headerSurface
    ) {
        val items = listOf(
            Triple(OmniDestination.HOME, "Home", Icons.Outlined.Home),
            Triple(OmniDestination.ZAI_CHAT, "Chat", Icons.Outlined.ChatBubbleOutline),
            Triple(OmniDestination.CODE_STUDIO, "Code Studio", Icons.Outlined.Code),
            Triple(OmniDestination.WORKSPACE_HUB, "Tasks", Icons.Outlined.Layers),
            Triple(OmniDestination.SETTINGS, "Settings", Icons.Outlined.Settings)
        )
        items.forEach { (dest, label, icon) ->
            NavigationRailItem(
                selected = currentTab == dest,
                onClick = { onSelectTab(dest) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.testTag("rail_tab_${dest.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun ZeroTrustPermissionGateDialog(
    request: PendingSecurityRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "High-Risk Action Permission Gate",
                tint = CrimsonAlert,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = CrimsonAlert.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.riskLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Requesting Model: ${request.requestingModelName} (Z AI Assistant)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )

                Text(
                    text = "Action Category: ${request.actionCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = request.targetSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Text(
                    text = "Zero-Trust Policy: No AI model can execute calls, emails, code scripts, or web queries without your explicit authorization.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldEnclave,
                    contentColor = Color(0xFF022C22)
                ),
                modifier = Modifier.testTag("approve_security_gate_button")
            ) {
                Text("Approve & Execute", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.testTag("deny_security_gate_button")
            ) {
                Text("Deny & Block", color = CrimsonAlert)
            }
        }
    )
}
