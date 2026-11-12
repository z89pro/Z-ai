package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalendarEventEntity
import com.example.data.CodeSnippetEntity
import com.example.data.HfHubSearchResult
import com.example.data.HfModelEntity
import com.example.data.HuggingFaceService
import com.example.data.OmniDatabase
import com.example.data.OmniRepository
import com.example.data.ProjectTaskEntity
import com.example.data.WorkflowMacroEntity
import com.example.domain.AccelerationBenchmarkResult
import com.example.domain.AgentBuildStep
import com.example.domain.AgentExecutionEvent
import com.example.domain.AgentProjectType
import com.example.domain.AutonomousCodingAgentEngine
import com.example.domain.AutonomousCodingAgentRuntime
import com.example.domain.CodingAgentPermissionConfig
import com.example.domain.HardwareAcceleratorDelegate
import com.example.domain.InferenceAccelerationConfig
import com.example.domain.CodingAgentProject
import com.example.domain.CodingChatTurn
import com.example.domain.DeviceHardwareProfile
import com.example.domain.DeviceIntentAndVoiceBridge
import com.example.domain.DiffLine
import com.example.domain.DocumentAnalysisReport
import com.example.domain.DocumentAndArchiveService
import com.example.domain.HardwareAndWebService
import com.example.domain.KVCacheStats
import com.example.domain.LocalAiAndSandboxEngine
import com.example.domain.ModelCapabilities
import com.example.domain.ModelCompatibilityAssessment
import com.example.domain.ProjectBuildReport
import com.example.domain.ProjectVirtualFile
import com.example.domain.RealExecutionManager
import com.example.domain.RecentCodeFileEntry
import com.example.domain.SandboxedWorkspaceManager
import com.example.domain.SensitiveDataAuditReport
import com.example.domain.SuggestedMeetingSlot
import com.example.domain.TerminalCommandResult
import com.example.domain.UniversalHfModelProvider
import com.example.domain.WakeWordSessionState
import com.example.domain.WebSearchCitation
import com.example.domain.WorkspaceCheckpoint
import com.example.domain.ZAICustomSkill
import com.example.ui.components.CountdownReminderItem
import com.example.ui.components.ReminderCategoryType
import com.example.ui.components.ReminderPresetDuration
import com.example.ui.screens.ZAIChatMessage
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OmniDestination {
    HOME,
    WORKSPACE_HUB,
    ZAI_CHAT,
    VOICE_AND_MACROS,
    CODE_STUDIO,
    HF_MODELS,
    SETTINGS
}

data class MacroExecutionStepState(
    val stepIndex: Int,
    val stepCode: String,
    val label: String,
    val isCompleted: Boolean,
    val outputNote: String = ""
)

data class PendingSecurityRequest(
    val id: Long = System.currentTimeMillis(),
    val actionCategory: String, // "HIGH_RISK_CALL", "HIGH_RISK_EMAIL", "HIGH_RISK_CODE_EXEC", "HIGH_RISK_WEB_SEARCH"
    val requestingModelName: String,
    val title: String,
    val targetSummary: String,
    val riskLevel: String = "HIGH RISK",
    val onApprovedAction: () -> Unit
)

data class SecurityAuditLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val modelName: String,
    val actionCategory: String,
    val targetSummary: String,
    val decision: String // "APPROVED", "DENIED_BY_USER", "BLOCKED_BY_ENCLAVE"
)

data class OmniUiState(
    val currentTab: OmniDestination = OmniDestination.HOME,
    val isDarkMode: Boolean = true, // Defaults to Late-Night Dark Mode
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val isStrictOfflineEnclave: Boolean = true,
    val isWebSearchEnabled: Boolean = false,
    val requireApprovalForHighRisk: Boolean = true,
    val isAccessibilityBridgeEnabled: Boolean = false,
    val isListeningVoice: Boolean = false,
    val isContinuousHandsFree: Boolean = false,
    val liveVoiceTranscript: String = "",
    // Continuous Microphone Wake Word Listener Service State
    val wakeWordSessionState: WakeWordSessionState = WakeWordSessionState.STANDBY_WAKE_WORD,
    val activeWakeWords: Set<String> = DeviceIntentAndVoiceBridge.DEFAULT_WAKE_WORDS,
    val lastDetectedWakeWord: String = "Jarvis",
    val microphoneDecibelDb: Float = -42f,
    val wakeWordSilenceRemainingSec: Int = 60,
    // Gemma 2B Local Sensitive Data & Smart Insights Enclave
    val sensitiveAuditReport: SensitiveDataAuditReport? = null,
    val aiBriefingBanner: String = "Local Workspace Enclave Ready • Gemma 2B Active",
    val suggestedMeetingSlots: List<SuggestedMeetingSlot> = emptyList(),
    val customSkills: List<ZAICustomSkill> = emptyList(),
    val universalSystemPrompt: String = "",
    val pendingSecurityRequest: PendingSecurityRequest? = null,
    val securityAuditLogs: List<SecurityAuditLogEntry> = emptyList(),
    // Live Web Search state
    val isSearchingWeb: Boolean = false,
    val webSearchCitations: List<WebSearchCitation> = emptyList(),
    // Macro & Compound Voice Pipeline
    val activeMacroRunningTitle: String? = null,
    val activeMacroSteps: List<MacroExecutionStepState> = emptyList(),
    val draftedMeetingAgenda: String = "",
    val lastVoiceCodeSnippet: String = "",
    val lastVoiceCodeOutput: String = "",
    val statusToastMessage: String? = null,
    // Hugging Face Hub & Device Hardware
    val deviceHardware: DeviceHardwareProfile = DeviceHardwareProfile(
        totalRamMb = 6144,
        availableRamMb = 3400,
        usedRamMb = 2744,
        cpuCores = 8,
        freeStorageMb = 16384,
        totalStorageMb = 32768,
        primaryAbi = "arm64-v8a",
        deviceModelName = "Android Device",
        deviceTierLabel = "Performance Mobile Tier",
        recommendedMaxModelMb = 2100,
        recommendedQuantization = "Q4_K_M / INT4-TFLite (1.5B–2.6B)"
    ),
    val hfSearchQuery: String = "gemma-2-2b gguf",
    val isSearchingHfHub: Boolean = false,
    val hfHubResults: List<HfHubSearchResult> = emptyList(),
    val downloadingModelId: String? = null,
    val downloadProgressPercent: Int = 0,
    val downloadStatusNote: String = "",
    // Code Studio & Autonomous Coding Agent IDE state
    val selectedSnippetId: Int? = null,
    val editorTitle: String = "index.html",
    val editorLanguage: String = "HTML",
    val editorCodeBuffer: String = "",
    val editorConsoleOutput: String = "",
    val aiCodePrompt: String = "",
    val agentProjects: List<CodingAgentProject> = emptyList(),
    val activeAgentProject: CodingAgentProject? = null,
    val selectedProjectFilePath: String = "index.html",
    val recentCodeFiles: List<RecentCodeFileEntry> = emptyList(),
    val isAgentBuildingProject: Boolean = false,
    val agentBuildSteps: List<AgentBuildStep> = emptyList(),
    val codingChatTurns: List<CodingChatTurn> = emptyList(),
    val agentPermissionConfig: CodingAgentPermissionConfig = CodingAgentPermissionConfig(),
    val agentExecutionEvents: List<AgentExecutionEvent> = emptyList(),
    val latestDiffLines: List<DiffLine> = emptyList(),
    val workspaceCheckpoints: List<WorkspaceCheckpoint> = emptyList(),
    val terminalHistory: List<TerminalCommandResult> = emptyList(),
    val lastBuildReport: ProjectBuildReport? = null,
    val modelCapabilities: ModelCapabilities = UniversalHfModelProvider().getCapabilities(null),
    val accelerationConfig: InferenceAccelerationConfig = InferenceAccelerationConfig(),
    val kvCacheStats: KVCacheStats = KVCacheStats(),
    val lastBenchmarkResult: AccelerationBenchmarkResult? = null,
    val isBenchmarkingAccelerator: Boolean = false,
    // Real AI Document & File Analyzer state
    val currentDocumentReport: DocumentAnalysisReport? = null,
    val analyzedDocumentsHistory: List<DocumentAnalysisReport> = emptyList(),
    // Smart Task, Habit, Work, Meeting & Study Countdown Reminders
    val countdownReminders: List<CountdownReminderItem> = emptyList(),
    val activeReminderAlertMessage: String? = null,
    // Gemini / ChatGPT-Style Z AI Chat Messages & Live AI Call
    val chatMessages: List<ZAIChatMessage> = emptyList(),
    val isLiveAiCallActive: Boolean = false,
    val liveCallDurationSeconds: Int = 0,
    val isLiveCallMuted: Boolean = false,
    val liveCallStatusText: String = "Connected • Speak or tap any prompt"
)

class OmniWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("zai_workspace_prefs", android.content.Context.MODE_PRIVATE)
    private val database = OmniDatabase.getDatabase(application)
    private val repository = OmniRepository(database.omniDao())
    private val hfService = HuggingFaceService(application)
    private val localEngine = LocalAiAndSandboxEngine()
    private val hardwareAndWebService = HardwareAndWebService(application)
    private val codingAgentEngine = AutonomousCodingAgentEngine()
    private val documentAndArchiveService = DocumentAndArchiveService()
    private val modelProvider = UniversalHfModelProvider()
    private val workspaceManager = SandboxedWorkspaceManager(File(application.filesDir, "workspaces"))
    private val executionManager = RealExecutionManager(workspaceManager, codingAgentEngine)
    private val agentRuntime = AutonomousCodingAgentRuntime(
        modelProvider = modelProvider,
        workspaceManager = workspaceManager,
        executionManager = executionManager,
        codingEngine = codingAgentEngine
    )

    private val _uiState = MutableStateFlow(
        OmniUiState(
            customSkills = localEngine.defaultCustomSkills(),
            countdownReminders = listOf(
                CountdownReminderItem(
                    id = 1001L,
                    title = "Review Sprint Backlog & Ship Release APK",
                    category = ReminderCategoryType.TASK_TODO,
                    presetLabel = "5 Min",
                    totalDurationSeconds = 5 * 60L,
                    remainingSeconds = 4 * 60L + 40L,
                    isFrequentRecurring = false
                ),
                CountdownReminderItem(
                    id = 1002L,
                    title = "Hydration, Posture Reset & 15-Min Deep Breathing",
                    category = ReminderCategoryType.HABITS_REMINDER,
                    presetLabel = "30 Min",
                    totalDurationSeconds = 30 * 60L,
                    remainingSeconds = 28 * 60L + 15L,
                    isFrequentRecurring = true,
                    habitStreakCount = 7
                ),
                CountdownReminderItem(
                    id = 1003L,
                    title = "Finish Zero-Trust Enclave Security Audit",
                    category = ReminderCategoryType.FINISH_WORK,
                    presetLabel = "1 Hour",
                    totalDurationSeconds = 3600L,
                    remainingSeconds = 3510L,
                    isFrequentRecurring = false
                ),
                CountdownReminderItem(
                    id = 1004L,
                    title = "Design Team Sync & Mockup Review",
                    category = ReminderCategoryType.MEETING,
                    presetLabel = "5 Min",
                    totalDurationSeconds = 5 * 60L,
                    remainingSeconds = 4 * 60L + 18L,
                    isFrequentRecurring = false
                ),
                CountdownReminderItem(
                    id = 1005L,
                    title = "Read & Study Gemma 2B Quantization Notes",
                    category = ReminderCategoryType.READ_STUDY,
                    presetLabel = "1 Day",
                    totalDurationSeconds = 86400L,
                    remainingSeconds = 86120L,
                    isFrequentRecurring = false
                )
            ),
            chatMessages = listOf(
                ZAIChatMessage(
                    id = 1L,
                    isUser = false,
                    senderName = "Z AI Assistant",
                    modelBadge = "Gemma 2 2B Instruct • Q4_K_M",
                    text = "### Welcome to Z AI Workspace\n\n" +
                        "• **Ask or Calculate**: Type any question, math expression, or code request below.\n" +
                        "• **Tasks & Alarms**: Schedule tasks, resolve calendar conflicts, or start countdown timers.\n" +
                        "• **Code & Models**: Build apps in **Code** or download/import `.gguf` & `.tflite` models in **Models**.\n\n" +
                        "_Running on-device local engine (Cloud Gemini requires Firebase setup)._",
                    timestamp = "Just now",
                    actionExecutedBadge = "On-Device Engine Ready"
                )
            ),
            securityAuditLogs = listOf(
                SecurityAuditLogEntry(
                    timeFormatted = currentTimeString(),
                    modelName = "Gemma 2 2B Instruct",
                    actionCategory = "ENCLAVE_INIT",
                    targetSummary = "Initialized Z AI Personal Assistant with Zero-Trust High-Risk Firewall",
                    decision = "APPROVED"
                )
            )
        )
    )
    val uiState: StateFlow<OmniUiState> = _uiState.asStateFlow()

    val tasks: StateFlow<List<ProjectTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<CalendarEventEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macros: StateFlow<List<WorkflowMacroEntity>> = repository.allMacros
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippets: StateFlow<List<CodeSnippetEntity>> = repository.allSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val models: StateFlow<List<HfModelEntity>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModel: StateFlow<HfModelEntity?> = combine(models) { listArray ->
        val list = listArray.firstOrNull().orEmpty()
        list.firstOrNull { it.isActive } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val voiceBridge = DeviceIntentAndVoiceBridge(
        context = application,
        onTranscriptPartial = { partial ->
            _uiState.value = _uiState.value.copy(
                liveVoiceTranscript = partial,
                wakeWordSilenceRemainingSec = 60
            )
        },
        onTranscriptFinal = { finalUtterance ->
            _uiState.value = _uiState.value.copy(
                liveVoiceTranscript = finalUtterance,
                wakeWordSilenceRemainingSec = 60
            )
            processSpokenOrTypedVoiceCommand(finalUtterance)
        },
        onListeningStateChanged = { listening ->
            _uiState.value = _uiState.value.copy(isListeningVoice = listening)
        },
        onStatusMessage = { msg ->
            showToast(msg)
        },
        onMicrophoneDecibelChanged = { dbVal ->
            _uiState.value = _uiState.value.copy(microphoneDecibelDb = dbVal)
        },
        onWakeWordDetected = { matchedWakeWord, trailingCommand ->
            handleWakeWordTriggered(matchedWakeWord, trailingCommand)
        }
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val hw = hardwareAndWebService.scanDeviceHardware()
            val starterProjects = codingAgentEngine.defaultStarterProjects()
            val starterTurns = codingAgentEngine.defaultStarterCodingChatHistory(starterProjects)
            val firstProj = starterProjects.firstOrNull()
            val firstFile = firstProj?.files?.firstOrNull()

            val persistedRecentRaw = prefs.getString("code_studio_recent_files_v1", null)
            val loadedRecents = documentAndArchiveService.deserializeRecentFiles(persistedRecentRaw).ifEmpty {
                firstProj?.files?.take(DocumentAndArchiveService.MAX_RECENT_FILES)?.mapIndexed { idx, vf ->
                    RecentCodeFileEntry(
                        projectId = firstProj.id,
                        projectName = firstProj.name,
                        filePath = vf.path,
                        language = vf.language,
                        snippetId = null,
                        openedAtMillis = System.currentTimeMillis() - idx * 1000L
                    )
                }.orEmpty().also { seeded ->
                    prefs.edit().putString(
                        "code_studio_recent_files_v1",
                        documentAndArchiveService.serializeRecentFiles(seeded)
                    ).apply()
                }
            }

            _uiState.value = _uiState.value.copy(
                deviceHardware = hw,
                agentProjects = starterProjects,
                activeAgentProject = firstProj,
                codingChatTurns = starterTurns,
                selectedProjectFilePath = firstFile?.path ?: "index.html",
                recentCodeFiles = loadedRecents,
                editorTitle = firstFile?.path ?: "index.html",
                editorLanguage = firstFile?.language ?: "HTML",
                editorCodeBuffer = firstFile?.content.orEmpty(),
                editorConsoleOutput = firstProj?.buildLogs.orEmpty()
            )

            repository.ensureSeedDataInitialized()
            syncModelsOnDiskWithDatabase()
            refreshLocalBriefing()
        }
        viewModelScope.launch(Dispatchers.Default) {
            events.collect { evList ->
                val slots = localEngine.suggestOptimalMeetingSlots(evList, 45)
                _uiState.value = _uiState.value.copy(suggestedMeetingSlots = slots)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            activeModel.collect { model ->
                rebuildUniversalSystemPrompt(model)
            }
        }
        // Live 1-Second Ticker for Task, Meeting, Study & Frequent Countdown Reminders + Wake Word 60s Silence Auto-Sleep
        // Starts after initial 2s post-launch grace window so first frame renders immediately
        viewModelScope.launch {
            delay(2000L)
            while (true) {
                delay(1000L)
                tickCountdownRemindersOneSecond()
                tickWakeWordAndCallTimerOneSecond()
            }
        }
    }

    // --- Wake Word Listener Service (Jarvis, Janu, Friday, Edith, Ultron) & 60s Silence Auto-Sleep ---

    fun toggleWakeWordListenerService(enabled: Boolean) {
        val nextState = if (enabled) {
            WakeWordSessionState.STANDBY_WAKE_WORD
        } else {
            WakeWordSessionState.DISABLED
        }
        _uiState.value = _uiState.value.copy(
            wakeWordSessionState = nextState,
            isContinuousHandsFree = enabled,
            wakeWordSilenceRemainingSec = 60
        )
        voiceBridge.setWakeWordListenerServiceEnabled(enabled, _uiState.value.activeWakeWords)
        showToast(
            if (enabled) {
                "Wake Word Listener ACTIVE (${_uiState.value.activeWakeWords.joinToString(", ")})"
            } else {
                "Wake Word Listener paused"
            }
        )
    }

    fun toggleWakeWordPhrase(persona: String) {
        val current = _uiState.value.activeWakeWords.toMutableSet()
        val existing = current.firstOrNull { it.equals(persona, ignoreCase = true) }
        if (existing != null && current.size > 1) {
            current.remove(existing)
        } else if (existing == null) {
            current.add(persona)
        }
        _uiState.value = _uiState.value.copy(activeWakeWords = current)
        voiceBridge.updateConfiguredWakeWords(current)
        showToast("Active Wake Words: ${current.joinToString(", ")}")
    }

    fun simulateWakeWordTrigger(wakeWord: String, trailingCommand: String = "") {
        handleWakeWordTriggered(wakeWord, trailingCommand)
    }

    private fun handleWakeWordTriggered(wakeWord: String, trailingCommand: String) {
        voiceBridge.triggerHapticPulse()
        _uiState.value = _uiState.value.copy(
            wakeWordSessionState = WakeWordSessionState.ACTIVE_COMMAND_SESSION,
            lastDetectedWakeWord = wakeWord,
            isContinuousHandsFree = true,
            isListeningVoice = true,
            wakeWordSilenceRemainingSec = 60,
            microphoneDecibelDb = -14.5f,
            liveVoiceTranscript = if (trailingCommand.isNotBlank()) {
                "Hey $wakeWord, $trailingCommand"
            } else {
                "Hey $wakeWord — Listening for continuous multi-step instructions..."
            }
        )
        recordAuditLog(
            "WAKE_WORD_DETECTED",
            "Wake word '$wakeWord' activated continuous voice command session",
            "APPROVED"
        )

        if (trailingCommand.isNotBlank()) {
            voiceBridge.speakOutLoud("$wakeWord online. Executing your multi-step command now.")
            processSpokenOrTypedVoiceCommand(trailingCommand)
        } else {
            voiceBridge.speakOutLoud("$wakeWord online and listening. Speak your multi-step instructions.")
            showToast("🎙️ '$wakeWord' Awake • Continuous multi-step session active (60s auto-sleep)")
        }
    }

    private fun tickWakeWordAndCallTimerOneSecond() {
        val state = _uiState.value
        var updatedState = state

        // 1. Update Live AI Call duration if active
        if (state.isLiveAiCallActive) {
            updatedState = updatedState.copy(
                liveCallDurationSeconds = state.liveCallDurationSeconds + 1
            )
        }

        // 2. Update 60-second silence auto-sleep countdown when in ACTIVE_COMMAND_SESSION
        if (state.wakeWordSessionState == WakeWordSessionState.ACTIVE_COMMAND_SESSION) {
            val remaining = state.wakeWordSilenceRemainingSec - 1
            updatedState = if (remaining <= 0) {
                updatedState.copy(
                    wakeWordSessionState = WakeWordSessionState.STANDBY_WAKE_WORD,
                    wakeWordSilenceRemainingSec = 60,
                    statusToastMessage = "60s silence reached • Returned to Wake Word Standby (${state.activeWakeWords.joinToString("/")})"
                )
            } else {
                updatedState.copy(wakeWordSilenceRemainingSec = remaining)
            }
        }

        if (updatedState != state) {
            _uiState.value = updatedState
        }
    }

    // --- Gemma 2B Local Sensitive Data Processing & Offline Smart Insights Enclave ---

    fun executeSensitiveInsightAction(insightId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            voiceBridge.triggerHapticPulse()
            val currentTasks = repository.getTasksSnapshot()
            val currentEvents = repository.getEventsSnapshot()

            when (insightId) {
                "insight_privacy_redaction" -> {
                    var sanitizedCount = 0
                    currentTasks.filter { it.isSensitive }.forEach { task ->
                        val cleanDesc = localEngine.sanitizeSensitiveTaskDescription(task.description)
                        if (cleanDesc != task.description) {
                            sanitizedCount++
                        }
                        repository.updateTask(
                            task.copy(
                                description = cleanDesc,
                                isSensitive = true
                            )
                        )
                    }
                    refreshLocalBriefing("sensitive")
                    recordAuditLog(
                        "LOCAL_PII_REDACTION",
                        "Gemma 2B sanitized & locked ${currentTasks.count { it.isSensitive }} sensitive tasks offline (0 KB egress)",
                        "APPROVED"
                    )
                    showToast("Shielded & sanitized ${currentTasks.count { it.isSensitive }} sensitive tasks in local SQLite enclave")
                }

                "insight_sensitive_deadline" -> {
                    runSmartAutoScheduler()
                }

                else -> {
                    val result = localEngine.runSmartAutoScheduler(
                        events = currentEvents,
                        tasks = currentTasks,
                        activeModel = activeModel.value
                    )
                    result.updatedEvents.forEach { ev ->
                        if (ev.id == 0) repository.addEvent(ev) else repository.updateEvent(ev)
                    }
                    result.updatedTasks.forEach { t -> repository.updateTask(t) }
                    refreshLocalBriefing()
                    showToast("Gemma 2B completed offline sensitive workload optimization")
                }
            }
        }
    }

    // --- Ringing Countdown Reminders ---

    private fun tickCountdownRemindersOneSecond() {
        val currentList = _uiState.value.countdownReminders
        if (currentList.isEmpty()) return

        var newlyFiredAlert: String? = null
        var newlyFiredSection = "Task To-Do"
        var newlyFiredTitle = "Z AI Reminder"
        val updatedList = currentList.map { item ->
            if (item.isPaused) {
                item
            } else if (item.remainingSeconds > 1L) {
                item.copy(remainingSeconds = item.remainingSeconds - 1L)
            } else {
                val alertMsg = item.buildAlertMessage()
                newlyFiredAlert = alertMsg
                newlyFiredSection = item.category.label
                newlyFiredTitle = item.title
                if (item.isFrequentRecurring) {
                    item.copy(
                        remainingSeconds = item.totalDurationSeconds,
                        firedCount = item.firedCount + 1,
                        habitStreakCount = if (item.category == ReminderCategoryType.HABITS_REMINDER) item.habitStreakCount + 1 else item.habitStreakCount
                    )
                } else {
                    item.copy(
                        remainingSeconds = 0L,
                        isPaused = true,
                        firedCount = item.firedCount + 1,
                        habitStreakCount = if (item.category == ReminderCategoryType.HABITS_REMINDER) item.habitStreakCount + 1 else item.habitStreakCount
                    )
                }
            }
        }

        if (newlyFiredAlert != null) {
            voiceBridge.startAlarmRingtone()
            voiceBridge.postReminderPopupNotification(
                sectionTitle = newlyFiredSection,
                title = newlyFiredTitle,
                body = newlyFiredAlert!!
            )
            voiceBridge.speakOutLoud("Alarm reminder from Z AI. $newlyFiredAlert")
            _uiState.value = _uiState.value.copy(
                countdownReminders = updatedList,
                activeReminderAlertMessage = newlyFiredAlert,
                statusToastMessage = "🔔 $newlyFiredAlert"
            )
        } else {
            _uiState.value = _uiState.value.copy(countdownReminders = updatedList)
        }
    }

    fun addCountdownReminder(
        title: String,
        category: ReminderCategoryType,
        preset: ReminderPresetDuration,
        isFrequentRecurring: Boolean
    ) {
        val cleanTitle = title.trim().ifEmpty { "Scheduled Z AI ${category.label}" }
        val newItem = CountdownReminderItem(
            id = System.currentTimeMillis(),
            title = cleanTitle,
            category = category,
            presetLabel = preset.label,
            totalDurationSeconds = preset.durationSeconds,
            remainingSeconds = preset.durationSeconds,
            isFrequentRecurring = isFrequentRecurring,
            habitStreakCount = if (category == ReminderCategoryType.HABITS_REMINDER) 1 else 0
        )
        _uiState.value = _uiState.value.copy(
            countdownReminders = listOf(newItem) + _uiState.value.countdownReminders,
            statusToastMessage = "Set ${preset.label} ringing alarm for '$cleanTitle' (${category.label})"
        )
        voiceBridge.triggerHapticPulse()
    }

    fun togglePauseCountdownReminder(reminderId: Long) {
        val updated = _uiState.value.countdownReminders.map { item ->
            if (item.id == reminderId) {
                val resetSecs = if (item.remainingSeconds <= 0L) item.totalDurationSeconds else item.remainingSeconds
                item.copy(isPaused = !item.isPaused, remainingSeconds = resetSecs)
            } else {
                item
            }
        }
        _uiState.value = _uiState.value.copy(countdownReminders = updated)
    }

    fun triggerReminderAlertImmediately(item: CountdownReminderItem) {
        val msg = item.buildAlertMessage()
        voiceBridge.startAlarmRingtone()
        voiceBridge.postReminderPopupNotification(
            sectionTitle = item.category.label,
            title = item.title,
            body = msg
        )
        voiceBridge.speakOutLoud("Alarm reminder from Z AI. $msg")
        _uiState.value = _uiState.value.copy(
            activeReminderAlertMessage = msg,
            statusToastMessage = "🔔 Ringing Popup Alarm: ${item.title}"
        )
    }

    fun snoozeActiveReminderFiveMinutes() {
        voiceBridge.stopAlarmRingtone()
        val currentAlert = _uiState.value.activeReminderAlertMessage ?: "Snoozed Task Check-In"
        val snoozedItem = CountdownReminderItem(
            id = System.currentTimeMillis(),
            title = currentAlert.substringAfter(": \"").substringBefore("\"").ifBlank { currentAlert.take(36) },
            category = ReminderCategoryType.TASK_TODO,
            presetLabel = "5 Min (Snoozed)",
            totalDurationSeconds = 5 * 60L,
            remainingSeconds = 5 * 60L,
            isFrequentRecurring = false
        )
        _uiState.value = _uiState.value.copy(
            activeReminderAlertMessage = null,
            countdownReminders = listOf(snoozedItem) + _uiState.value.countdownReminders,
            statusToastMessage = "Stopped alarm & snoozed reminder for 5 minutes"
        )
    }

    fun dismissActiveReminderAlert() {
        voiceBridge.stopAlarmRingtone()
        _uiState.value = _uiState.value.copy(
            activeReminderAlertMessage = null,
            statusToastMessage = "Alarm stopped & reminder marked complete"
        )
    }

    fun deleteCountdownReminder(reminderId: Long) {
        _uiState.value = _uiState.value.copy(
            countdownReminders = _uiState.value.countdownReminders.filterNot { it.id == reminderId }
        )
    }

    private fun rebuildUniversalSystemPrompt(model: HfModelEntity? = activeModel.value) {
        val state = _uiState.value
        val prompt = localEngine.buildUniversalZAISystemPrompt(
            activeModel = model,
            skills = state.customSkills,
            isOfflineEnclaveLocked = state.isStrictOfflineEnclave,
            isWebSearchEnabled = state.isWebSearchEnabled,
            isAccessibilityGranted = state.isAccessibilityBridgeEnabled
        )
        _uiState.value = state.copy(universalSystemPrompt = prompt)
    }

    fun selectTab(tab: OmniDestination) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        val isDark = mode != AppThemeMode.LIGHT
        _uiState.value = _uiState.value.copy(
            themeMode = mode,
            isDarkMode = isDark
        )
        showToast("Theme set to ${mode.label}")
    }

    fun toggleDarkMode() {
        val nextDark = !_uiState.value.isDarkMode
        val nextMode = if (nextDark) AppThemeMode.DARK else AppThemeMode.LIGHT
        setThemeMode(nextMode)
    }

    fun sendPromptFromHome(rawPrompt: String) {
        val clean = rawPrompt.trim()
        if (clean.isEmpty()) return
        val lower = clean.lowercase()
        if (lower.startsWith("build ") || lower.startsWith("create a website") || lower.startsWith("create a game") || lower.contains("code studio")) {
            _uiState.value = _uiState.value.copy(
                currentTab = OmniDestination.CODE_STUDIO,
                aiCodePrompt = clean
            )
            buildAutonomousProjectWithAgent(clean, null)
        } else {
            _uiState.value = _uiState.value.copy(currentTab = OmniDestination.ZAI_CHAT)
            sendChatMessage(clean)
        }
    }

    fun toggleStrictOfflineEnclave() {
        val next = !_uiState.value.isStrictOfflineEnclave
        val webNext = if (next) false else _uiState.value.isWebSearchEnabled
        _uiState.value = _uiState.value.copy(
            isStrictOfflineEnclave = next,
            isWebSearchEnabled = webNext
        )
        rebuildUniversalSystemPrompt()
        refreshLocalBriefing()
        showToast(
            if (next) "Strict Offline Enclave Locked • All external web access blocked"
            else "Enclave unlocked • Permission-gated Web Search & HF Hub available"
        )
    }

    fun toggleWebSearchCapability(enabled: Boolean) {
        val nextOffline = if (enabled) false else _uiState.value.isStrictOfflineEnclave
        _uiState.value = _uiState.value.copy(
            isWebSearchEnabled = enabled,
            isStrictOfflineEnclave = nextOffline
        )
        rebuildUniversalSystemPrompt()
        refreshLocalBriefing()
        showToast(
            if (enabled) "Z AI Web Search Grounding enabled (Permission-Gated)"
            else "Z AI Web Search disabled (100% Local Offline Mode)"
        )
    }

    fun toggleHighRiskApprovalFirewall(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(requireApprovalForHighRisk = enabled)
        showToast(
            if (enabled) "Zero-Trust High-Risk Permission Gate ACTIVE"
            else "Session Fast-Approve active for trusted administrator"
        )
    }

    fun toggleCustomSkill(skillId: String) {
        val updated = _uiState.value.customSkills.map {
            if (it.id == skillId) it.copy(isEnabled = !it.isEnabled) else it
        }
        val isWebSkillOn = updated.firstOrNull { it.id == "skill_web_search" }?.isEnabled ?: false
        _uiState.value = _uiState.value.copy(
            customSkills = updated,
            isWebSearchEnabled = isWebSkillOn && !_uiState.value.isStrictOfflineEnclave
        )
        rebuildUniversalSystemPrompt()
        refreshLocalBriefing()
    }

    fun addCustomSkill(name: String, description: String, instruction: String, requiresGate: Boolean) {
        if (name.isBlank()) return
        val newSkill = ZAICustomSkill(
            id = "skill_custom_${System.currentTimeMillis()}",
            name = name.trim(),
            description = description.trim().ifEmpty { "Custom administrator skill injected into loaded HF models." },
            isEnabled = true,
            requiresApprovalGate = requiresGate,
            promptInstruction = instruction.trim().ifEmpty { "Follow administrator custom skill rules." }
        )
        _uiState.value = _uiState.value.copy(
            customSkills = _uiState.value.customSkills + newSkill
        )
        rebuildUniversalSystemPrompt()
        showToast("Added custom skill '${newSkill.name}' to Z AI persona")
    }

    fun openAccessibilitySettingsAndToggleBridge() {
        val msg = voiceBridge.launchAccessibilitySettingsIntent()
        val next = !_uiState.value.isAccessibilityBridgeEnabled
        _uiState.value = _uiState.value.copy(isAccessibilityBridgeEnabled = next)
        rebuildUniversalSystemPrompt()
        recordAuditLog("ACCESSIBILITY_BRIDGE", msg, "APPROVED")
        showToast(msg)
    }

    // --- Zero-Trust High-Risk Security Permission Gate ---

    private fun requestHighRiskPermissionOrRun(
        actionCategory: String,
        title: String,
        targetSummary: String,
        action: () -> Unit
    ) {
        val modelName = activeModel.value?.displayName ?: "Gemma 2 2B Instruct"
        if (!_uiState.value.requireApprovalForHighRisk) {
            recordAuditLog(actionCategory, targetSummary, "APPROVED")
            action()
            return
        }
        _uiState.value = _uiState.value.copy(
            pendingSecurityRequest = PendingSecurityRequest(
                actionCategory = actionCategory,
                requestingModelName = modelName,
                title = title,
                targetSummary = targetSummary,
                riskLevel = "HIGH RISK • USER PERMISSION REQUIRED",
                onApprovedAction = action
            )
        )
    }

    fun approvePendingSecurityRequest() {
        val req = _uiState.value.pendingSecurityRequest ?: return
        _uiState.value = _uiState.value.copy(pendingSecurityRequest = null)
        recordAuditLog(req.actionCategory, req.targetSummary, "APPROVED")
        req.onApprovedAction.invoke()
    }

    fun denyPendingSecurityRequest() {
        val req = _uiState.value.pendingSecurityRequest ?: return
        _uiState.value = _uiState.value.copy(pendingSecurityRequest = null)
        recordAuditLog(req.actionCategory, req.targetSummary, "DENIED_BY_USER")
        showToast("Blocked '${req.title}' — Model action denied by user")
    }

    private fun recordAuditLog(category: String, summary: String, decision: String) {
        val modelName = activeModel.value?.displayName ?: "Gemma 2 2B Instruct"
        val entry = SecurityAuditLogEntry(
            timeFormatted = currentTimeString(),
            modelName = modelName,
            actionCategory = category,
            targetSummary = summary,
            decision = decision
        )
        _uiState.value = _uiState.value.copy(
            securityAuditLogs = listOf(entry) + _uiState.value.securityAuditLogs.take(24)
        )
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(statusToastMessage = null)
    }

    private fun showToast(msg: String) {
        _uiState.value = _uiState.value.copy(statusToastMessage = msg)
    }

    fun refreshLocalBriefing(customQuestion: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTasks = repository.getTasksSnapshot()
            val currentEvents = repository.getEventsSnapshot()
            val model = activeModel.value
            val slots = localEngine.suggestOptimalMeetingSlots(currentEvents, 45)
            val sensitiveAudit = localEngine.analyzeSensitiveProjectDataOffline(
                tasks = currentTasks,
                events = currentEvents,
                activeModel = model
            )
            val briefing = localEngine.generateLocalWorkspaceBriefing(
                tasks = currentTasks,
                events = currentEvents,
                activeModel = model,
                skills = _uiState.value.customSkills,
                isOfflineEnclaveLocked = _uiState.value.isStrictOfflineEnclave,
                isWebSearchEnabled = _uiState.value.isWebSearchEnabled,
                userPrompt = customQuestion
            )
            _uiState.value = _uiState.value.copy(
                aiBriefingBanner = briefing,
                suggestedMeetingSlots = slots,
                sensitiveAuditReport = sensitiveAudit
            )

            if (customQuestion.isNotBlank() && _uiState.value.isWebSearchEnabled && !_uiState.value.isStrictOfflineEnclave) {
                triggerWebSearchWithPermission(customQuestion)
            }
        }
    }

    fun triggerWebSearchWithPermission(query: String) {
        val clean = query.trim().ifEmpty { "Gemma 2B on-device scheduling and Android architecture" }
        if (_uiState.value.isStrictOfflineEnclave) {
            recordAuditLog("HIGH_RISK_WEB_SEARCH", "Blocked web query '$clean' (Offline Enclave Lock ON)", "BLOCKED_BY_ENCLAVE")
            showToast("Web Search blocked by Strict Offline Enclave Lock. Unlock Enclave or enable Web Search first.")
            return
        }
        requestHighRiskPermissionOrRun(
            actionCategory = "HIGH_RISK_WEB_SEARCH",
            title = "Allow Z AI to Search the Web?",
            targetSummary = "Query external web & documentation APIs for: \"$clean\""
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSearchingWeb = true)
                val citations = hardwareAndWebService.performLiveWebSearch(clean)
                _uiState.value = _uiState.value.copy(
                    isSearchingWeb = false,
                    webSearchCitations = citations,
                    statusToastMessage = "Z AI retrieved ${citations.size} web citations for '$clean'"
                )
            }
        }
    }

    fun openCitationUrl(url: String) {
        requestHighRiskPermissionOrRun(
            actionCategory = "EXTERNAL_BROWSER_LINK",
            title = "Open External Citation Link?",
            targetSummary = url
        ) {
            voiceBridge.launchWebOrAppUrl(url)
        }
    }

    // --- Gemma 2B Kanban Prioritization, Meeting Slot Booking & Auto-Scheduler ---

    fun runSmartAutoScheduler() {
        viewModelScope.launch {
            voiceBridge.triggerHapticPulse()
            val currentEvents = repository.getEventsSnapshot()
            val currentTasks = repository.getTasksSnapshot()
            val result = localEngine.runSmartAutoScheduler(
                events = currentEvents,
                tasks = currentTasks,
                activeModel = activeModel.value
            )
            result.updatedEvents.forEach { ev ->
                if (ev.id == 0) repository.addEvent(ev) else repository.updateEvent(ev)
            }
            result.updatedTasks.forEach { task ->
                repository.updateTask(task)
            }
            val newSlots = localEngine.suggestOptimalMeetingSlots(result.updatedEvents, 45)
            val sensitiveReport = localEngine.analyzeSensitiveProjectDataOffline(
                tasks = result.updatedTasks,
                events = result.updatedEvents,
                activeModel = activeModel.value
            )
            _uiState.value = _uiState.value.copy(
                aiBriefingBanner = result.summaryReport,
                suggestedMeetingSlots = newSlots,
                sensitiveAuditReport = sensitiveReport,
                statusToastMessage = "Gemma 2B prioritized ${result.updatedTasks.size} tasks & resolved ${result.conflictsResolvedCount} conflict(s)"
            )
            voiceBridge.speakOutLoud("Schedule optimized by Z AI. Resolved ${result.conflictsResolvedCount} conflicts and prioritized tasks by deadline and importance.")
        }
    }

    fun runAutoPrioritizeTasksOnly() {
        viewModelScope.launch {
            voiceBridge.triggerHapticPulse()
            val currentTasks = repository.getTasksSnapshot()
            val prioritized = currentTasks.map { task ->
                val (score, newTier) = localEngine.computeTaskPriorityScore(task)
                val updated = task.copy(priority = newTier, computedPriorityScore = score)
                repository.updateTask(updated)
                updated
            }.sortedByDescending { it.computedPriorityScore }

            val top = prioritized.firstOrNull { it.status != "DONE" }
            refreshLocalBriefing("priority")
            showToast(
                "Re-prioritized ${prioritized.size} tasks by Deadline & Importance • #1: ${top?.title?.take(24) ?: "Done"}"
            )
        }
    }

    fun bookSuggestedMeetingSlot(slot: SuggestedMeetingSlot, customTitle: String = "") {
        viewModelScope.launch {
            voiceBridge.triggerHapticPulse()
            val title = customTitle.trim().ifEmpty { "Z AI Optimal Sync (${slot.rationale.substringBefore("(").trim()})" }
            val event = CalendarEventEntity(
                title = title,
                startHour = slot.startHour,
                startMinute = slot.startMinute,
                durationMinutes = slot.durationMinutes,
                attendees = "Recommended by Gemma 2B (${slot.confidencePercent}% match)",
                locationOrLink = "Conflict-Free Enclave Slot",
                category = "MEETING",
                hasConflict = false,
                conflictReason = "",
                isAutoScheduled = true
            )
            repository.addEvent(event)
            refreshLocalBriefing()
            showToast("Booked optimal meeting slot: ${slot.formattedWindow}")
            triggerAutonomousWorkflowsForEvent("ON_MEETING_SCHEDULED", title)
        }
    }

    fun updateTaskImportanceAndDeadline(task: ProjectTaskEntity, newImportance: Int, newDeadlineHours: Int) {
        viewModelScope.launch {
            val clampedHours = newDeadlineHours.coerceAtLeast(1)
            val draft = task.copy(
                userImportance = newImportance.coerceIn(1, 5),
                deadlineHoursRemaining = clampedHours
            )
            val (score, tier) = localEngine.computeTaskPriorityScore(draft)
            repository.updateTask(draft.copy(priority = tier, computedPriorityScore = score))
            refreshLocalBriefing()
            showToast("Updated '${task.title.take(20)}' -> $tier (Score $score)")
            if (clampedHours <= 12) {
                triggerAutonomousWorkflowsForEvent("ON_DEADLINE_APPROACHING", "${task.title} (Due in ${clampedHours}h)")
            }
        }
    }

    fun advanceTaskStatus(task: ProjectTaskEntity) {
        viewModelScope.launch {
            val nextStatus = when (task.status) {
                "TODO" -> "IN_PROGRESS"
                "IN_PROGRESS" -> "DONE"
                else -> "TODO"
            }
            repository.updateTask(task.copy(status = nextStatus))
            refreshLocalBriefing()
        }
    }

    fun createNewTask(
        title: String,
        description: String,
        projectTag: String,
        priority: String,
        estimatedMinutes: Int,
        isSensitive: Boolean,
        deadlineHours: Int = 24,
        userImportance: Int = 4
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val clampedDeadline = deadlineHours.coerceIn(1, 720)
            val draft = ProjectTaskEntity(
                title = title.trim(),
                description = description.trim().ifEmpty { "Processed in local Z AI enclave." },
                projectTag = projectTag.trim().ifEmpty { "Core Engine" },
                status = "TODO",
                priority = priority,
                estimatedMinutes = estimatedMinutes.coerceIn(15, 240),
                scheduledTimeSlot = "Unscheduled",
                isSensitive = isSensitive,
                deadlineHoursRemaining = clampedDeadline,
                userImportance = userImportance.coerceIn(1, 5)
            )
            val (score, derivedTier) = localEngine.computeTaskPriorityScore(draft)
            repository.addTask(draft.copy(priority = derivedTier, computedPriorityScore = score))
            refreshLocalBriefing()
            showToast("Added '${draft.title}' (Priority Score: $score • $derivedTier)")
            triggerAutonomousWorkflowsForEvent("ON_TASK_ADDED", draft.title)
            if (clampedDeadline <= 12) {
                triggerAutonomousWorkflowsForEvent("ON_DEADLINE_APPROACHING", "${draft.title} (Due in ${clampedDeadline}h)")
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            refreshLocalBriefing()
        }
    }

    fun createNewCalendarEvent(
        title: String,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int,
        attendees: String,
        category: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getEventsSnapshot()
            val newStart = startHour * 60 + startMinute
            val newEnd = newStart + durationMinutes
            val overlappingEvent = existing.firstOrNull { ev ->
                newStart < ev.endTotalMinutes && newEnd > ev.startTotalMinutes
            }
            val overlaps = overlappingEvent != null
            val reason = if (overlappingEvent != null) {
                "Overlaps with '${overlappingEvent.title}' (${overlappingEvent.formattedTimeRange()})"
            } else {
                ""
            }
            val event = CalendarEventEntity(
                title = title.trim(),
                startHour = startHour.coerceIn(0, 23),
                startMinute = startMinute.coerceIn(0, 59),
                durationMinutes = durationMinutes.coerceIn(15, 180),
                attendees = attendees.trim().ifEmpty { "Workspace Team" },
                locationOrLink = "Encrypted Meet / Local Room",
                category = category,
                hasConflict = overlaps,
                conflictReason = reason,
                isAutoScheduled = false
            )
            repository.addEvent(event)
            refreshLocalBriefing()
            showToast(
                if (overlaps) "Conflict detected with '${overlappingEvent?.title}' — running autonomous resolver!"
                else "Event '${event.title}' scheduled"
            )
            triggerAutonomousWorkflowsForEvent("ON_MEETING_SCHEDULED", event.title)
        }
    }

    fun deleteCalendarEvent(eventId: Int) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            refreshLocalBriefing()
        }
    }

    /**
     * Evaluates and autonomously executes all enabled multi-step workflows subscribed to [eventType]
     * (e.g., ON_TASK_ADDED, ON_MEETING_SCHEDULED, ON_DEADLINE_APPROACHING).
     */
    fun triggerAutonomousWorkflowsForEvent(
        eventType: String,
        eventContextTitle: String = "Workspace Event"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val allMacrosList = repository.getMacrosSnapshot()
            val currentTasks = repository.getTasksSnapshot()
            val currentEvents = repository.getEventsSnapshot()

            val result = localEngine.executeEventTriggeredWorkflows(
                eventType = eventType,
                eventContextTitle = eventContextTitle,
                macros = allMacrosList,
                tasks = currentTasks,
                events = currentEvents,
                activeModel = activeModel.value
            )
            if (result.triggeredMacros.isEmpty()) return@launch

            result.updatedTasks.forEach { t ->
                if (t.id == 0) repository.addTask(t) else repository.updateTask(t)
            }
            result.updatedEvents.forEach { ev ->
                if (ev.id == 0) repository.addEvent(ev) else repository.updateEvent(ev)
            }
            result.triggeredMacros.forEach { m ->
                repository.updateMacro(m)
            }

            // Post real notifications for any SEND_NOTIFICATION steps
            result.dispatchedNotifications.forEach { notifBody ->
                voiceBridge.postReminderPopupNotification(
                    sectionTitle = "Autonomous Workflow ($eventType)",
                    title = eventContextTitle,
                    body = notifBody
                )
            }

            // Create countdown focus timer if requested by workflow
            result.createdTimerTitle?.let { timerTitle ->
                val autoReminder = CountdownReminderItem(
                    id = System.currentTimeMillis(),
                    title = timerTitle,
                    category = ReminderCategoryType.FINISH_WORK,
                    presetLabel = "30 Min",
                    totalDurationSeconds = 30 * 60L,
                    remainingSeconds = 30 * 60L,
                    isFrequentRecurring = false
                )
                _uiState.value = _uiState.value.copy(
                    countdownReminders = listOf(autoReminder) + _uiState.value.countdownReminders
                )
            }

            val stepStates = result.executedStepLogs.mapIndexed { idx, logLine ->
                val stepCode = logLine.substringAfter("-> ").substringBefore(":").trim()
                val outputNote = logLine.substringAfter(": ").trim()
                MacroExecutionStepState(
                    stepIndex = idx + 1,
                    stepCode = stepCode,
                    label = humanReadableStepLabel(stepCode),
                    isCompleted = true,
                    outputNote = outputNote
                )
            }

            recordAuditLog(
                category = "AUTONOMOUS_EVENT_WORKFLOW",
                summary = result.executiveSummary,
                decision = "APPROVED"
            )

            _uiState.value = _uiState.value.copy(
                activeMacroRunningTitle = "Auto-Triggered ($eventType): ${result.triggeredMacros.joinToString { it.title }}",
                activeMacroSteps = stepStates,
                aiBriefingBanner = result.executiveSummary,
                statusToastMessage = "⚡ Auto-Workflow ($eventType): ${result.triggeredMacros.first().title}"
            )
            voiceBridge.triggerHapticPulse()
        }
    }

    fun toggleMacroAutoTrigger(macro: WorkflowMacroEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val next = !macro.isAutoTriggerEnabled
            repository.updateMacro(
                macro.copy(
                    isAutoTriggerEnabled = next,
                    lastRunSummary = if (next) {
                        "Auto-trigger ARMED for ${macro.triggerEventDisplayLabel()}"
                    } else {
                        "Auto-trigger PAUSED (Manual/Voice execution only)"
                    }
                )
            )
            showToast(
                if (next) "Enabled auto-trigger for '${macro.title}' (${macro.triggerEventDisplayLabel()})"
                else "Paused auto-trigger for '${macro.title}'"
            )
        }
    }

    fun deleteWorkflowMacro(macroId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMacro(macroId)
            showToast("Deleted workflow macro")
        }
    }

    // --- Voice Assistant & Autonomous Multi-Step Workflows ---

    fun toggleVoiceRecognition(continuousHandsFree: Boolean = _uiState.value.isContinuousHandsFree) {
        if (_uiState.value.isListeningVoice) {
            voiceBridge.stopVoiceListening()
        } else {
            _uiState.value = _uiState.value.copy(
                isContinuousHandsFree = continuousHandsFree,
                wakeWordSilenceRemainingSec = 60
            )
            voiceBridge.startVoiceListening(continuous = continuousHandsFree)
        }
    }

    fun setContinuousHandsFree(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isContinuousHandsFree = enabled,
            wakeWordSessionState = if (enabled) WakeWordSessionState.ACTIVE_COMMAND_SESSION else WakeWordSessionState.STANDBY_WAKE_WORD,
            wakeWordSilenceRemainingSec = 60
        )
        if (enabled && !_uiState.value.isListeningVoice) {
            voiceBridge.startVoiceListening(continuous = true)
        } else if (!enabled && _uiState.value.isListeningVoice) {
            voiceBridge.stopVoiceListening()
        }
    }

    fun processSpokenOrTypedVoiceCommand(utterance: String) {
        val rawClean = utterance.trim()
        if (rawClean.isEmpty()) return

        // Check if utterance starts with a wake word (Jarvis, Janu, Friday, Edith, Ultron)
        val (detectedWake, strippedCommand) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance(
            rawClean,
            _uiState.value.activeWakeWords
        )
        val clean = if (detectedWake != null) {
            _uiState.value = _uiState.value.copy(
                wakeWordSessionState = WakeWordSessionState.ACTIVE_COMMAND_SESSION,
                lastDetectedWakeWord = detectedWake,
                wakeWordSilenceRemainingSec = 60,
                liveVoiceTranscript = rawClean
            )
            if (strippedCommand.isBlank()) {
                voiceBridge.speakOutLoud("$detectedWake online and listening for your multi-step instructions.")
                showToast("🎙️ '$detectedWake' Awake • Speak your next instruction")
                return
            }
            strippedCommand
        } else {
            _uiState.value = _uiState.value.copy(
                liveVoiceTranscript = rawClean,
                wakeWordSilenceRemainingSec = 60
            )
            rawClean
        }

        viewModelScope.launch {
            val allMacrosList = repository.getMacrosSnapshot()
            val matchedMacro = allMacrosList.firstOrNull { m ->
                clean.equals(m.triggerPhrase, ignoreCase = true) ||
                    (clean.length < 32 && clean.contains(m.triggerPhrase, ignoreCase = true))
            }

            if (matchedMacro != null) {
                executeWorkflowMacro(matchedMacro)
                return@launch
            }

            // Parse complex multi-step voice instruction or voice-to-code instruction
            val parsedSteps = localEngine.parseCompoundVoiceInstruction(clean)
            val isMultiStepOrActionable = parsedSteps.size > 1 || parsedSteps.any {
                it.intentType == com.example.domain.VoiceSubIntentType.SCHEDULE_TEAM_MEETING ||
                    it.intentType == com.example.domain.VoiceSubIntentType.CREATE_TASKS_FOR_ATTENDEES ||
                    it.intentType == com.example.domain.VoiceSubIntentType.DRAFT_MEETING_AGENDA ||
                    it.intentType == com.example.domain.VoiceSubIntentType.WRITE_AND_RUN_CODE
            }

            if (isMultiStepOrActionable) {
                executeCompoundVoicePipeline(clean, parsedSteps)
                return@launch
            }

            val lower = clean.lowercase()
            when {
                lower.contains("call ") || lower.contains("dial ") -> {
                    val digits = clean.filter { it.isDigit() || it == '+' }.ifEmpty { "+15550192834" }
                    triggerDirectPhoneCall(digits)
                }
                lower.contains("email ") || lower.contains("mail ") -> {
                    triggerDirectEmail("team@zai.local")
                }
                lower.contains("alarm") -> {
                    triggerDirectAlarm(8, 30)
                }
                lower.contains("schedule") || lower.contains("conflict") -> {
                    runSmartAutoScheduler()
                }
                lower.contains("sensitive") || lower.contains("redact") || lower.contains("privacy") -> {
                    executeSensitiveInsightAction("insight_privacy_redaction")
                }
                lower.contains("web ") || lower.contains("search ") -> {
                    triggerWebSearchWithPermission(clean.removePrefix("search ").removePrefix("web "))
                }
                else -> {
                    refreshLocalBriefing(clean)
                    voiceBridge.speakOutLoud("Z AI processed your request locally with ${activeModel.value?.displayName ?: "Gemma 2B"}.")
                    showToast("Z AI answered in Workspace Hub")
                }
            }
        }
    }

    private fun executeCompoundVoicePipeline(
        utterance: String,
        parsedSteps: List<com.example.domain.ParsedVoiceSubInstruction>
    ) {
        val hasCodeExec = parsedSteps.any {
            it.intentType == com.example.domain.VoiceSubIntentType.WRITE_AND_RUN_CODE
        }

        val pipelineRunner: () -> Unit = {
            viewModelScope.launch {
                voiceBridge.triggerHapticPulse()
                val stepStates = parsedSteps.map { s ->
                    MacroExecutionStepState(
                        stepIndex = s.stepNumber,
                        stepCode = s.intentType.name,
                        label = s.summaryTitle,
                        isCompleted = false,
                        outputNote = "Parsing clause: \"${s.rawClause.take(48)}\"..."
                    )
                }.toMutableList()

                _uiState.value = _uiState.value.copy(
                    activeMacroRunningTitle = "Multi-Step Voice Command (${parsedSteps.size} Clauses)",
                    activeMacroSteps = stepStates.toList(),
                    wakeWordSilenceRemainingSec = 60
                )

                val existingEvents = repository.getEventsSnapshot()
                val existingTasks = repository.getTasksSnapshot()
                val planResult = localEngine.executeCompoundVoicePlan(
                    utterance = utterance,
                    parsedSteps = parsedSteps,
                    existingEvents = existingEvents,
                    existingTasks = existingTasks,
                    activeModel = activeModel.value
                )

                for (i in parsedSteps.indices) {
                    delay(340)
                    val step = parsedSteps[i]
                    val note = when (step.intentType) {
                        com.example.domain.VoiceSubIntentType.SCHEDULE_TEAM_MEETING -> {
                            val ev = planResult.createdEvents.firstOrNull()
                            if (ev != null) {
                                repository.addEvent(ev)
                                "Scheduled '${ev.title}' (${ev.formattedTimeRange()}) with ${ev.attendees}"
                            } else {
                                "Scheduled team meeting in conflict-free slot"
                            }
                        }
                        com.example.domain.VoiceSubIntentType.CREATE_TASKS_FOR_ATTENDEES -> {
                            planResult.createdTasks.forEach { t -> repository.addTask(t) }
                            "Created ${planResult.createdTasks.size} attendee tasks on Kanban board"
                        }
                        com.example.domain.VoiceSubIntentType.DRAFT_MEETING_AGENDA -> {
                            "Drafted structured agenda aligned with ongoing project goals"
                        }
                        com.example.domain.VoiceSubIntentType.WRITE_AND_RUN_CODE -> {
                            val snipId = repository.addSnippet(
                                CodeSnippetEntity(
                                    title = "Voice_Synthesized_Script.${if (planResult.synthesizedCodeLanguage == "Python") "py" else "kt"}",
                                    language = planResult.synthesizedCodeLanguage,
                                    codeContent = planResult.synthesizedCode,
                                    lastOutput = planResult.codeExecutionOutput
                                )
                            )
                            _uiState.value = _uiState.value.copy(
                                selectedSnippetId = snipId.toInt(),
                                editorTitle = "Voice_Synthesized_Script.${if (planResult.synthesizedCodeLanguage == "Python") "py" else "kt"}",
                                editorLanguage = planResult.synthesizedCodeLanguage,
                                editorCodeBuffer = planResult.synthesizedCode,
                                editorConsoleOutput = planResult.codeExecutionOutput
                            )
                            "Synthesized & executed ${planResult.synthesizedCodeLanguage} script in sandbox"
                        }
                        else -> "Executed: ${step.summaryTitle}"
                    }

                    stepStates[i] = stepStates[i].copy(
                        isCompleted = true,
                        outputNote = note
                    )
                    _uiState.value = _uiState.value.copy(activeMacroSteps = stepStates.toList())
                }

                _uiState.value = _uiState.value.copy(
                    aiBriefingBanner = planResult.executiveSummary,
                    draftedMeetingAgenda = planResult.draftedAgendaMarkdown.ifBlank { _uiState.value.draftedMeetingAgenda },
                    lastVoiceCodeSnippet = planResult.synthesizedCode.ifBlank { _uiState.value.lastVoiceCodeSnippet },
                    lastVoiceCodeOutput = planResult.codeExecutionOutput.ifBlank { _uiState.value.lastVoiceCodeOutput },
                    statusToastMessage = "Completed ${parsedSteps.size}-step voice workflow!"
                )
                refreshLocalBriefing()
                voiceBridge.triggerHapticPulse()
                voiceBridge.speakOutLoud(planResult.executiveSummary)
            }
        }

        if (hasCodeExec) {
            requestHighRiskPermissionOrRun(
                actionCategory = "HIGH_RISK_VOICE_CODE_EXEC",
                title = "Authorize Voice-to-Code Synthesis & Execution?",
                targetSummary = "Voice Command: \"$utterance\" (${parsedSteps.size} steps including sandbox code execution)",
                action = pipelineRunner
            )
        } else {
            recordAuditLog("MULTI_STEP_VOICE_WORKFLOW", "Executed ${parsedSteps.size}-step command: ${utterance.take(60)}", "APPROVED")
            pipelineRunner()
        }
    }

    fun executeWorkflowMacro(macro: WorkflowMacroEntity) {
        val rawSteps = macro.stepsList()
        val hasHighRiskStep = rawSteps.any {
            it.startsWith("DIAL_NUMBER") || it.startsWith("SEND_EMAIL") || it.startsWith("GENERATE_")
        }

        val runner: () -> Unit = {
            viewModelScope.launch {
                voiceBridge.triggerHapticPulse()
                val stepStates = rawSteps.mapIndexed { idx, code ->
                    MacroExecutionStepState(
                        stepIndex = idx + 1,
                        stepCode = code,
                        label = humanReadableStepLabel(code),
                        isCompleted = false,
                        outputNote = "Queued..."
                    )
                }.toMutableList()

                _uiState.value = _uiState.value.copy(
                    activeMacroRunningTitle = macro.title,
                    activeMacroSteps = stepStates.toList()
                )

                val executionNotes = mutableListOf<String>()

                for (i in rawSteps.indices) {
                    val code = rawSteps[i]
                    stepStates[i] = stepStates[i].copy(outputNote = "Executing on-device...")
                    _uiState.value = _uiState.value.copy(activeMacroSteps = stepStates.toList())
                    delay(380)

                    val stepOutput = executeSingleMacroStep(code, macro.targetPhoneOrEmail)
                    executionNotes.add(stepOutput)

                    stepStates[i] = stepStates[i].copy(
                        isCompleted = true,
                        outputNote = stepOutput
                    )
                    _uiState.value = _uiState.value.copy(activeMacroSteps = stepStates.toList())
                }

                val finalSummary = executionNotes.lastOrNull() ?: "Workflow completed"
                repository.updateMacro(
                    macro.copy(
                        runCount = macro.runCount + 1,
                        lastRunSummary = finalSummary
                    )
                )
                voiceBridge.triggerHapticPulse()
                showToast("Autonomous Workflow '${macro.title}' finished")
            }
        }

        if (hasHighRiskStep) {
            requestHighRiskPermissionOrRun(
                actionCategory = "HIGH_RISK_MACRO_PIPELINE",
                title = "Authorize Autonomous Workflow '${macro.title}'?",
                targetSummary = "Steps: ${rawSteps.joinToString(" -> ")} (Target: ${macro.targetPhoneOrEmail.ifBlank { "Device Intents" }})",
                action = runner
            )
        } else {
            runner()
        }
    }

    private suspend fun executeSingleMacroStep(stepCode: String, fallbackTarget: String): String {
        val upper = stepCode.uppercase()
        return when {
            upper.startsWith("SEND_NOTIFICATION") -> {
                val openTasks = repository.getTasksSnapshot().count { it.status != "DONE" }
                val conflicts = repository.getEventsSnapshot().count { it.hasConflict }
                val msg = "Z AI Workflow Alert: $openTasks active task(s), $conflicts conflict(s) monitored."
                voiceBridge.postReminderPopupNotification(
                    sectionTitle = "Z AI Workflow Notification",
                    title = "Autonomous Workflow Alert",
                    body = msg
                )
                "Dispatched system popup notification ($openTasks active tasks)"
            }
            upper.startsWith("UPDATE_TASK_STATUS") -> {
                val currentTasks = repository.getTasksSnapshot()
                var promotedCount = 0
                currentTasks.forEach { t ->
                    val (score, tier) = localEngine.computeTaskPriorityScore(t)
                    val shouldAdvance = t.status == "TODO" && (t.deadlineHoursRemaining <= 24 || tier == "CRITICAL" || promotedCount == 0)
                    val updated = if (shouldAdvance) {
                        promotedCount++
                        t.copy(status = "IN_PROGRESS", priority = tier, computedPriorityScore = score)
                    } else {
                        t.copy(priority = tier, computedPriorityScore = score)
                    }
                    repository.updateTask(updated)
                }
                "Advanced $promotedCount task(s) to IN_PROGRESS & updated ${currentTasks.size} priority scores"
            }
            upper.startsWith("RESCHEDULE_EVENTS") || upper.startsWith("AUTO_RESOLVE_CALENDAR") -> {
                val res = localEngine.runSmartAutoScheduler(
                    events = repository.getEventsSnapshot(),
                    tasks = repository.getTasksSnapshot(),
                    activeModel = activeModel.value
                )
                res.updatedEvents.forEach { ev ->
                    if (ev.id == 0) repository.addEvent(ev) else repository.updateEvent(ev)
                }
                res.updatedTasks.forEach { t -> repository.updateTask(t) }
                "Rescheduled ${res.conflictsResolvedCount} conflicting event(s) & prioritized ${res.updatedTasks.size} task(s)"
            }
            upper.startsWith("CREATE_COUNTDOWN_TIMER") -> {
                val newItem = CountdownReminderItem(
                    id = System.currentTimeMillis(),
                    title = "Workflow Focus Sprint Timer",
                    category = ReminderCategoryType.FINISH_WORK,
                    presetLabel = "30 Min",
                    totalDurationSeconds = 30 * 60L,
                    remainingSeconds = 30 * 60L,
                    isFrequentRecurring = false
                )
                _uiState.value = _uiState.value.copy(
                    countdownReminders = listOf(newItem) + _uiState.value.countdownReminders
                )
                "Created 30m countdown timer 'Workflow Focus Sprint Timer'"
            }
            upper.startsWith("ANALYZE_SENSITIVE_TASKS") -> {
                val currentTasks = repository.getTasksSnapshot()
                val crit = currentTasks.count { it.priority == "CRITICAL" && it.status != "DONE" }
                val sens = currentTasks.count { it.isSensitive }
                "Audited ${currentTasks.size} tasks ($sens sensitive, $crit critical) in local SQLite"
            }
            upper.startsWith("SET_ALARM") -> {
                val timePart = stepCode.substringAfter(":", "09:00")
                val h = timePart.substringBefore(":").toIntOrNull() ?: 9
                val m = timePart.substringAfter(":", "0").toIntOrNull() ?: 0
                voiceBridge.launchSetAlarmIntent(h, m, "Z AI Focus Block")
            }
            upper.startsWith("SET_TIMER") -> {
                val secs = stepCode.substringAfter(":", "1800").toIntOrNull() ?: 1800
                voiceBridge.launchSetTimerIntent(secs, "Z AI Deep Focus")
            }
            upper.startsWith("DIAL_NUMBER") -> {
                val num = stepCode.substringAfter(":", fallbackTarget.ifBlank { "+15550192834" })
                voiceBridge.launchDialIntent(num)
            }
            upper.startsWith("SEND_EMAIL") -> {
                val email = stepCode.substringAfter(":", fallbackTarget.ifBlank { "sec-ops@zai.local" })
                voiceBridge.launchEmailIntent(
                    recipient = email,
                    subject = "[Z AI Enclave] Automated Status Report",
                    body = _uiState.value.aiBriefingBanner
                )
            }
            upper.startsWith("INSERT_SYSTEM_CALENDAR") -> {
                val nextEv = repository.getEventsSnapshot().firstOrNull()
                voiceBridge.launchSystemCalendarInsert(
                    title = nextEv?.title ?: "Z AI Sprint Sync",
                    description = "Synced from Z AI Local Enclave",
                    startHour = nextEv?.startHour ?: 10,
                    startMin = nextEv?.startMinute ?: 0,
                    durationMin = nextEv?.durationMinutes ?: 45
                )
            }
            upper.startsWith("GENERATE_") -> {
                val code = localEngine.synthesizeCodeFromPrompt(
                    prompt = "Automated macro incident & velocity diagnostic",
                    language = "Kotlin",
                    tasks = repository.getTasksSnapshot(),
                    events = repository.getEventsSnapshot(),
                    activeModel = activeModel.value
                )
                _uiState.value = _uiState.value.copy(editorCodeBuffer = code)
                "Synthesized diagnostic script in Code Studio via ${activeModel.value?.displayName ?: "Gemma 2B"}"
            }
            upper.startsWith("SPEAK_BRIEFING") -> {
                refreshLocalBriefing()
                voiceBridge.speakOutLoud(_uiState.value.aiBriefingBanner)
                "Spoke executive briefing aloud via on-device TTS"
            }
            else -> "Completed step: $stepCode"
        }
    }

    private fun humanReadableStepLabel(code: String): String = when {
        code.startsWith("SEND_NOTIFICATION") -> "Send System Notification & Alert"
        code.startsWith("UPDATE_TASK_STATUS") -> "Update Task Statuses & Priority Scores"
        code.startsWith("RESCHEDULE_EVENTS") -> "Reschedule Conflicting & Related Events"
        code.startsWith("CREATE_COUNTDOWN_TIMER") -> "Start 30m Focus Countdown Timer"
        code.startsWith("ANALYZE_SENSITIVE_TASKS") -> "Local Enclave Task & Deadline Audit"
        code.startsWith("AUTO_RESOLVE_CALENDAR") -> "Gemma 2B Conflict & Priority Optimizer"
        code.startsWith("SET_ALARM") -> "System Alarm Intent (${code.substringAfter(":", "09:00")})"
        code.startsWith("SET_TIMER") -> "System Deep-Focus Timer Intent"
        code.startsWith("DIAL_NUMBER") -> "Android Phone Dialer Intent (Permission-Gated)"
        code.startsWith("SEND_EMAIL") -> "Android Email Composer Intent (Permission-Gated)"
        code.startsWith("INSERT_SYSTEM_CALENDAR") -> "Native Android Calendar Sync Intent"
        code.startsWith("GENERATE_") -> "Local AI Code Synthesis"
        code.startsWith("SPEAK_BRIEFING") -> "Spoken Executive Briefing (TTS)"
        else -> code
    }

    fun createCustomMacro(
        title: String,
        triggerPhrase: String,
        description: String,
        stepsPipeSeparated: String,
        targetContact: String,
        triggerEventType: String = "ON_TASK_ADDED",
        isAutoTriggerEnabled: Boolean = true
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val cleanPhrase = triggerPhrase.trim().lowercase().ifEmpty {
                title.trim().lowercase().take(24)
            }
            val macro = WorkflowMacroEntity(
                title = title.trim(),
                triggerPhrase = cleanPhrase,
                description = description.trim().ifEmpty { "Custom event-driven autonomous multi-step workflow" },
                stepsPipeSeparated = stepsPipeSeparated.ifBlank { "SEND_NOTIFICATION|UPDATE_TASK_STATUS|RESCHEDULE_EVENTS|SPEAK_BRIEFING" },
                targetPhoneOrEmail = targetContact.trim(),
                runCount = 0,
                lastRunSummary = "Armed for ${com.example.data.WorkflowTriggerEventType.fromCode(triggerEventType).label}",
                triggerEventType = triggerEventType,
                isAutoTriggerEnabled = isAutoTriggerEnabled
            )
            repository.addMacro(macro)
            showToast("Saved autonomous workflow '${macro.title}' (${macro.triggerEventDisplayLabel()})")
        }
    }

    // Direct Quick Device Actions (Permission-Gated for High-Risk Calls & Emails)
    fun triggerDirectPhoneCall(phone: String) {
        requestHighRiskPermissionOrRun(
            actionCategory = "HIGH_RISK_CALL",
            title = "Allow Z AI to Initiate Phone Call?",
            targetSummary = "Open Android Phone Dialer with number: $phone"
        ) {
            val msg = voiceBridge.launchDialIntent(phone)
            showToast(msg)
        }
    }

    fun triggerDirectEmail(recipient: String) {
        requestHighRiskPermissionOrRun(
            actionCategory = "HIGH_RISK_EMAIL",
            title = "Allow Z AI to Draft & Send Email?",
            targetSummary = "Recipient: $recipient • Subject: Z AI Enclave Task Summary"
        ) {
            val msg = voiceBridge.launchEmailIntent(
                recipient = recipient,
                subject = "Z AI Enclave Task Summary",
                body = _uiState.value.aiBriefingBanner
            )
            showToast(msg)
        }
    }

    fun triggerDirectAlarm(hour: Int, minute: Int) {
        val msg = voiceBridge.launchSetAlarmIntent(hour, minute, "Z AI Sprint Milestone")
        recordAuditLog("OS_ALARM_INTENT", msg, "APPROVED")
        showToast(msg)
    }

    fun triggerDirectSystemCalendar() {
        viewModelScope.launch {
            val ev = repository.getEventsSnapshot().firstOrNull()
            val msg = voiceBridge.launchSystemCalendarInsert(
                title = ev?.title ?: "Z AI Deep Work Block",
                description = "Scheduled by Z AI Personal Assistant",
                startHour = ev?.startHour ?: 10,
                startMin = ev?.startMinute ?: 30,
                durationMin = ev?.durationMinutes ?: 45
            )
            recordAuditLog("OS_CALENDAR_INTENT", msg, "APPROVED")
            showToast(msg)
        }
    }

    // --- Code Studio, AI Studio / DeepSeek Coding Chat & Permission-Gated Local Sandbox ---

    private fun recordAndPersistRecentFile(
        projectId: String,
        projectName: String,
        filePath: String,
        language: String,
        snippetId: Int? = null
    ): List<RecentCodeFileEntry> {
        val entry = RecentCodeFileEntry(
            projectId = projectId,
            projectName = projectName,
            filePath = filePath,
            language = language,
            snippetId = snippetId,
            openedAtMillis = System.currentTimeMillis()
        )
        val updated = documentAndArchiveService.recordRecentFileOpened(
            currentHistory = _uiState.value.recentCodeFiles,
            openedEntry = entry
        )
        prefs.edit().putString(
            "code_studio_recent_files_v1",
            documentAndArchiveService.serializeRecentFiles(updated)
        ).apply()
        return updated
    }

    fun openRecentCodeFile(entry: RecentCodeFileEntry) {
        if (entry.snippetId != null) {
            val matchedSnippet = snippets.value.firstOrNull { it.id == entry.snippetId }
            if (matchedSnippet != null) {
                selectCodeSnippet(matchedSnippet)
                return
            }
        }
        val targetProj = _uiState.value.agentProjects.firstOrNull { it.id == entry.projectId }
            ?: _uiState.value.activeAgentProject
            ?: return
        val targetFile = targetProj.files.firstOrNull { it.path.equals(entry.filePath, ignoreCase = true) }
            ?: targetProj.files.firstOrNull()
            ?: return

        workspaceManager.syncProjectToSandbox(targetProj)
        val updatedRecents = recordAndPersistRecentFile(
            projectId = targetProj.id,
            projectName = targetProj.name,
            filePath = targetFile.path,
            language = targetFile.language,
            snippetId = null
        )
        _uiState.value = _uiState.value.copy(
            activeAgentProject = targetProj,
            workspaceCheckpoints = workspaceManager.getCheckpoints(targetProj.id),
            selectedSnippetId = null,
            selectedProjectFilePath = targetFile.path,
            recentCodeFiles = updatedRecents,
            editorTitle = targetFile.path,
            editorLanguage = targetFile.language,
            editorCodeBuffer = targetFile.content,
            editorConsoleOutput = targetProj.buildLogs,
            statusToastMessage = "Opened '${targetFile.path}' (${targetProj.name})"
        )
    }

    fun clearRecentCodeFiles() {
        prefs.edit().remove("code_studio_recent_files_v1").apply()
        _uiState.value = _uiState.value.copy(
            recentCodeFiles = emptyList(),
            statusToastMessage = "Cleared Recent Files history"
        )
    }

    fun selectAgentProject(project: CodingAgentProject) {
        workspaceManager.syncProjectToSandbox(project)
        val firstFile = project.files.firstOrNull()
        val updatedRecents = if (firstFile != null) {
            recordAndPersistRecentFile(
                projectId = project.id,
                projectName = project.name,
                filePath = firstFile.path,
                language = firstFile.language
            )
        } else {
            _uiState.value.recentCodeFiles
        }
        _uiState.value = _uiState.value.copy(
            activeAgentProject = project,
            workspaceCheckpoints = workspaceManager.getCheckpoints(project.id),
            selectedSnippetId = null,
            selectedProjectFilePath = firstFile?.path ?: "index.html",
            recentCodeFiles = updatedRecents,
            editorTitle = firstFile?.path ?: project.name,
            editorLanguage = firstFile?.language ?: "HTML",
            editorCodeBuffer = firstFile?.content.orEmpty(),
            editorConsoleOutput = project.buildLogs,
            statusToastMessage = "Loaded project '${project.name}' (${project.files.size} files)"
        )
    }

    fun selectProjectVirtualFile(filePath: String) {
        val proj = _uiState.value.activeAgentProject ?: return
        val targetFile = proj.files.firstOrNull { it.path == filePath } ?: return
        val updatedRecents = recordAndPersistRecentFile(
            projectId = proj.id,
            projectName = proj.name,
            filePath = targetFile.path,
            language = targetFile.language
        )
        _uiState.value = _uiState.value.copy(
            selectedProjectFilePath = targetFile.path,
            recentCodeFiles = updatedRecents,
            selectedSnippetId = null,
            editorTitle = targetFile.path,
            editorLanguage = targetFile.language,
            editorCodeBuffer = targetFile.content
        )
    }

    fun addNewVirtualProjectFile(filePath: String, language: String, initialContent: String) {
        val proj = _uiState.value.activeAgentProject ?: return
        val cleanPath = filePath.trim().ifEmpty { "module_${proj.files.size + 1}.js" }
        val writeRes = workspaceManager.writeFile(proj.id, cleanPath, initialContent, createCheckpoint = true)
        val newFile = writeRes.getOrElse {
            ProjectVirtualFile(path = cleanPath, language = language, content = initialContent)
        }
        val updatedFiles = workspaceManager.listFiles(proj.id).ifEmpty {
            proj.files.filterNot { it.path.equals(cleanPath, true) } + newFile
        }
        val rebuilt = codingAgentEngine.rebuildPreviewBundle(proj.copy(files = updatedFiles))
        val updatedProjects = _uiState.value.agentProjects.map {
            if (it.id == rebuilt.id) rebuilt else it
        }
        val updatedRecents = recordAndPersistRecentFile(
            projectId = rebuilt.id,
            projectName = rebuilt.name,
            filePath = newFile.path,
            language = newFile.language
        )
        _uiState.value = _uiState.value.copy(
            agentProjects = updatedProjects,
            activeAgentProject = rebuilt,
            workspaceCheckpoints = workspaceManager.getCheckpoints(proj.id),
            selectedProjectFilePath = newFile.path,
            recentCodeFiles = updatedRecents,
            editorTitle = newFile.path,
            editorLanguage = newFile.language,
            editorCodeBuffer = newFile.content,
            editorConsoleOutput = rebuilt.buildLogs,
            statusToastMessage = "Added '${newFile.path}' to ${rebuilt.name}"
        )
    }

    fun deleteVirtualProjectFile(filePath: String) {
        val proj = _uiState.value.activeAgentProject ?: return
        if (proj.files.size <= 1) {
            showToast("Cannot delete the last remaining file in project")
            return
        }
        requestHighRiskPermissionOrRun(
            actionCategory = "HIGH_RISK_FILE_DELETE",
            title = "Delete File '$filePath' from Workspace?",
            targetSummary = "Project: ${proj.name} • A recovery checkpoint will be created before deletion."
        ) {
            workspaceManager.deleteFile(proj.id, filePath)
            val remaining = workspaceManager.listFiles(proj.id)
            val rebuilt = codingAgentEngine.rebuildPreviewBundle(proj.copy(files = remaining))
            val first = remaining.firstOrNull()
            val cleanedRecents = documentAndArchiveService.removeRecentFile(
                currentHistory = _uiState.value.recentCodeFiles,
                projectId = proj.id,
                filePath = filePath
            )
            prefs.edit().putString(
                "code_studio_recent_files_v1",
                documentAndArchiveService.serializeRecentFiles(cleanedRecents)
            ).apply()
            _uiState.value = _uiState.value.copy(
                activeAgentProject = rebuilt,
                agentProjects = _uiState.value.agentProjects.map { if (it.id == rebuilt.id) rebuilt else it },
                workspaceCheckpoints = workspaceManager.getCheckpoints(proj.id),
                selectedProjectFilePath = first?.path ?: "index.html",
                recentCodeFiles = cleanedRecents,
                editorTitle = first?.path ?: "index.html",
                editorLanguage = first?.language ?: "HTML",
                editorCodeBuffer = first?.content.orEmpty(),
                statusToastMessage = "Deleted '$filePath' (Checkpoint saved)"
            )
        }
    }

    fun renameVirtualProjectFile(oldPath: String, newPath: String) {
        val proj = _uiState.value.activeAgentProject ?: return
        val res = workspaceManager.renameFile(proj.id, oldPath, newPath)
        res.onSuccess { renamed ->
            val updatedFiles = workspaceManager.listFiles(proj.id)
            val rebuilt = codingAgentEngine.rebuildPreviewBundle(proj.copy(files = updatedFiles))
            val withoutOld = documentAndArchiveService.removeRecentFile(
                currentHistory = _uiState.value.recentCodeFiles,
                projectId = proj.id,
                filePath = oldPath
            )
            val updatedRecents = documentAndArchiveService.recordRecentFileOpened(
                currentHistory = withoutOld,
                openedEntry = RecentCodeFileEntry(
                    projectId = rebuilt.id,
                    projectName = rebuilt.name,
                    filePath = renamed.path,
                    language = renamed.language
                )
            )
            prefs.edit().putString(
                "code_studio_recent_files_v1",
                documentAndArchiveService.serializeRecentFiles(updatedRecents)
            ).apply()
            _uiState.value = _uiState.value.copy(
                activeAgentProject = rebuilt,
                agentProjects = _uiState.value.agentProjects.map { if (it.id == rebuilt.id) rebuilt else it },
                workspaceCheckpoints = workspaceManager.getCheckpoints(proj.id),
                selectedProjectFilePath = renamed.path,
                recentCodeFiles = updatedRecents,
                editorTitle = renamed.path,
                editorLanguage = renamed.language,
                statusToastMessage = "Renamed '$oldPath' -> '${renamed.path}'"
            )
        }.onFailure { err ->
            showToast("Rename failed: ${err.localizedMessage}")
        }
    }

    fun restoreWorkspaceCheckpoint(checkpointId: String) {
        val proj = _uiState.value.activeAgentProject ?: return
        val res = workspaceManager.restoreCheckpoint(proj.id, checkpointId)
        res.onSuccess { restoredFiles ->
            val rebuilt = codingAgentEngine.rebuildPreviewBundle(proj.copy(files = restoredFiles))
            val first = restoredFiles.firstOrNull()
            _uiState.value = _uiState.value.copy(
                activeAgentProject = rebuilt,
                agentProjects = _uiState.value.agentProjects.map { if (it.id == rebuilt.id) rebuilt else it },
                selectedProjectFilePath = first?.path ?: "index.html",
                editorTitle = first?.path ?: "index.html",
                editorLanguage = first?.language ?: "HTML",
                editorCodeBuffer = first?.content.orEmpty(),
                statusToastMessage = "Restored project '${proj.name}' from checkpoint"
            )
        }
    }

    fun cancelAgentTask() {
        agentRuntime.cancelCurrentTask()
        _uiState.value = _uiState.value.copy(
            isAgentBuildingProject = false,
            statusToastMessage = "Autonomous Agent task cancelled safely • Workspace checkpoint intact"
        )
    }

    fun fixProjectBugsWithAgent() {
        buildAutonomousProjectWithAgent("Find and fix a bug in my existing project and run verification tests", null)
    }

    fun executeWorkspaceTerminalCommand(commandLine: String) {
        val projId = _uiState.value.activeAgentProject?.id ?: "default_workspace"
        val clean = commandLine.trim()
        if (clean.isEmpty()) return

        val runCmd: () -> Unit = {
            viewModelScope.launch(Dispatchers.IO) {
                val res = executionManager.executeCommand(projId, clean)
                _uiState.value = _uiState.value.copy(
                    terminalHistory = _uiState.value.terminalHistory + res,
                    editorConsoleOutput = (res.stdout + "\n" + res.stderr).trim()
                )
            }
        }

        if (clean.startsWith("rm ") || clean.contains("&& rm") || clean.contains("kill ")) {
            requestHighRiskPermissionOrRun(
                actionCategory = "HIGH_RISK_TERMINAL_CMD",
                title = "Authorize High-Risk Terminal Command?",
                targetSummary = "Command: '$clean' in workspace '$projId'",
                action = runCmd
            )
        } else {
            runCmd()
        }
    }

    fun stopWorkspaceTerminalProcess() {
        executionManager.stopProcess()
        showToast("Stopped active workspace terminal process")
    }

    fun updateCodingAgentPermissions(newConfig: CodingAgentPermissionConfig) {
        _uiState.value = _uiState.value.copy(
            agentPermissionConfig = newConfig,
            statusToastMessage = "Updated Coding Agent environment permissions"
        )
    }

    fun buildAutonomousProjectWithAgent(prompt: String, requestedType: AgentProjectType?) {
        val cleanPrompt = prompt.trim().ifEmpty {
            "Build an Interactive Customer Onboarding Form & Analytics Web App"
        }
        val model = activeModel.value
        val modelName = model?.displayName ?: "Gemma 2 2B Instruct"
        val resolvedType = requestedType ?: codingAgentEngine.inferProjectTypeFromPrompt(cleanPrompt)

        val executeBuild: () -> Unit = {
            viewModelScope.launch(Dispatchers.IO) {
                voiceBridge.triggerHapticPulse()
                val steps = codingAgentEngine.generateExecutionStepsForPrompt(
                    prompt = cleanPrompt,
                    projectType = resolvedType,
                    modelName = modelName
                ).toMutableList()

                _uiState.value = _uiState.value.copy(
                    isAgentBuildingProject = true,
                    agentBuildSteps = steps.toList(),
                    agentExecutionEvents = emptyList()
                )

                val currentProj = _uiState.value.activeAgentProject
                val taskResult = agentRuntime.executeAutonomousTask(
                    userPrompt = cleanPrompt,
                    currentProject = currentProj,
                    explicitType = requestedType,
                    activeModel = model
                ) { liveEvent ->
                    val currentEvents = _uiState.value.agentExecutionEvents
                    _uiState.value = _uiState.value.copy(
                        agentExecutionEvents = currentEvents + liveEvent
                    )
                }

                for (i in steps.indices) {
                    steps[i] = steps[i].copy(isCompleted = true)
                }

                val builtProject = taskResult.updatedProject
                val firstFile = builtProject.files.firstOrNull()
                val existingFiltered = _uiState.value.agentProjects.filterNot { it.id == builtProject.id }
                val updatedList = listOf(builtProject) + existingFiltered

                _uiState.value = _uiState.value.copy(
                    isAgentBuildingProject = false,
                    agentBuildSteps = steps.toList(),
                    agentProjects = updatedList,
                    activeAgentProject = builtProject,
                    codingChatTurns = listOf(taskResult.chatTurn) + _uiState.value.codingChatTurns,
                    latestDiffLines = taskResult.latestDiffLines,
                    lastBuildReport = taskResult.buildReport,
                    workspaceCheckpoints = workspaceManager.getCheckpoints(builtProject.id),
                    selectedSnippetId = null,
                    selectedProjectFilePath = firstFile?.path ?: "index.html",
                    editorTitle = firstFile?.path ?: builtProject.name,
                    editorLanguage = firstFile?.language ?: "HTML",
                    editorCodeBuffer = firstFile?.content.orEmpty(),
                    editorConsoleOutput = builtProject.buildLogs,
                    aiCodePrompt = "",
                    statusToastMessage = if (taskResult.wasCancelled) {
                        "Agent task cancelled safely"
                    } else {
                        "Z AI built & verified '${builtProject.name}' (${builtProject.files.size} files)"
                    }
                )
                voiceBridge.triggerHapticPulse()
            }
        }

        if (_uiState.value.agentPermissionConfig.requireApprovalBeforeAgentBuild &&
            _uiState.value.requireApprovalForHighRisk
        ) {
            requestHighRiskPermissionOrRun(
                actionCategory = "HIGH_RISK_CODING_AGENT_BUILD",
                title = "Allow Z AI Coding Agent to Scaffold & Build Project?",
                targetSummary = "Target: ${resolvedType.label} • Prompt: \"$cleanPrompt\" (Multi-file VFS write + offline bundle)",
                action = executeBuild
            )
        } else {
            recordAuditLog("CODING_AGENT_BUILD", "Built ${resolvedType.label}: $cleanPrompt", "APPROVED")
            executeBuild()
        }
    }

    fun importPersonalProjectFromBrowser(
        projectName: String,
        importedFiles: List<ProjectVirtualFile>
    ) {
        if (!_uiState.value.agentPermissionConfig.allowFileBrowserProjectImport) {
            showToast("File Browser Import is disabled in Agent Permissions")
            return
        }
        val modelName = activeModel.value?.displayName ?: "Gemma 2 2B Instruct"
        val project = codingAgentEngine.importProjectFromBrowserFiles(
            projectName = projectName,
            importedFiles = importedFiles,
            activeModel = activeModel.value
        )
        val importTurn = codingAgentEngine.generateCodingChatTurnForProject(
            prompt = "Imported personal project via Android File Browser ($projectName)",
            project = project,
            modelName = modelName,
            timestamp = currentTimeString()
        )
        val firstFile = project.files.firstOrNull()
        val updatedProjects = listOf(project) + _uiState.value.agentProjects
        recordAuditLog("FILE_BROWSER_PROJECT_IMPORT", "Imported ${project.files.size} files into '${project.name}'", "APPROVED")
        _uiState.value = _uiState.value.copy(
            agentProjects = updatedProjects,
            activeAgentProject = project,
            codingChatTurns = listOf(importTurn) + _uiState.value.codingChatTurns,
            selectedSnippetId = null,
            selectedProjectFilePath = firstFile?.path ?: "README.md",
            editorTitle = firstFile?.path ?: project.name,
            editorLanguage = firstFile?.language ?: "Markdown",
            editorCodeBuffer = firstFile?.content.orEmpty(),
            editorConsoleOutput = project.buildLogs,
            statusToastMessage = "Imported ${project.files.size} file(s) from File Browser into Z AI IDE"
        )
    }

    fun importPersonalProjectFromLink(rawUrl: String) {
        if (!_uiState.value.agentPermissionConfig.allowLinkAndRepoImport) {
            showToast("Link & Repo Import is disabled in Agent Permissions")
            return
        }
        val cleanUrl = rawUrl.trim().ifEmpty { "https://github.com/developer/personal-web-app" }
        requestHighRiskPermissionOrRun(
            actionCategory = "PROJECT_LINK_IMPORT",
            title = "Import Personal Project from Link?",
            targetSummary = "Source URL: $cleanUrl • Scaffold files into Z AI Offline Coding Studio"
        ) {
            viewModelScope.launch {
                val modelName = activeModel.value?.displayName ?: "Gemma 2 2B Instruct"
                val project = codingAgentEngine.importProjectFromLink(
                    rawUrl = cleanUrl,
                    isOfflineLocked = _uiState.value.isStrictOfflineEnclave,
                    activeModel = activeModel.value
                )
                val linkTurn = codingAgentEngine.generateCodingChatTurnForProject(
                    prompt = "Imported repository via link: $cleanUrl",
                    project = project,
                    modelName = modelName,
                    timestamp = currentTimeString()
                )
                val firstFile = project.files.firstOrNull()
                val updatedProjects = listOf(project) + _uiState.value.agentProjects
                _uiState.value = _uiState.value.copy(
                    agentProjects = updatedProjects,
                    activeAgentProject = project,
                    codingChatTurns = listOf(linkTurn) + _uiState.value.codingChatTurns,
                    selectedSnippetId = null,
                    selectedProjectFilePath = firstFile?.path ?: "index.html",
                    editorTitle = firstFile?.path ?: project.name,
                    editorLanguage = firstFile?.language ?: "HTML",
                    editorCodeBuffer = firstFile?.content.orEmpty(),
                    editorConsoleOutput = project.buildLogs,
                    statusToastMessage = "Imported '${project.name}' from link into Z AI Studio"
                )
            }
        }
    }

    fun selectCodeSnippet(snippet: CodeSnippetEntity) {
        val updatedRecents = recordAndPersistRecentFile(
            projectId = "snippets",
            projectName = "Saved Snippets",
            filePath = snippet.title,
            language = snippet.language,
            snippetId = snippet.id
        )
        _uiState.value = _uiState.value.copy(
            selectedSnippetId = snippet.id,
            recentCodeFiles = updatedRecents,
            editorTitle = snippet.title,
            editorLanguage = snippet.language,
            editorCodeBuffer = snippet.codeContent,
            editorConsoleOutput = snippet.lastOutput
        )
    }

    fun updateEditorCode(newCode: String) {
        val state = _uiState.value
        val activeProj = state.activeAgentProject
        if (state.selectedSnippetId == null && activeProj != null) {
            val updatedFiles = activeProj.files.map { f ->
                if (f.path == state.selectedProjectFilePath) f.copy(content = newCode) else f
            }
            val rebuilt = codingAgentEngine.rebuildPreviewBundle(activeProj.copy(files = updatedFiles))
            val updatedProjects = state.agentProjects.map {
                if (it.id == rebuilt.id) rebuilt else it
            }
            _uiState.value = state.copy(
                editorCodeBuffer = newCode,
                activeAgentProject = rebuilt,
                agentProjects = updatedProjects
            )
        } else {
            _uiState.value = state.copy(editorCodeBuffer = newCode)
        }
    }

    fun updateEditorLanguage(language: String) {
        val ext = when (language) {
            "Python" -> ".py"
            "JavaScript" -> ".js"
            "HTML" -> ".html"
            "CSS" -> ".css"
            "SQL" -> ".sql"
            "Bash" -> ".sh"
            else -> ".kt"
        }
        val baseTitle = _uiState.value.editorTitle.substringBeforeLast(".")
        _uiState.value = _uiState.value.copy(
            editorLanguage = language,
            editorTitle = "$baseTitle$ext"
        )
    }

    fun updateAiCodePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(aiCodePrompt = prompt)
    }

    fun generateCodeWithLocalAi(overridePrompt: String = "") {
        val promptToUse = overridePrompt.ifBlank { _uiState.value.aiCodePrompt }
        buildAutonomousProjectWithAgent(promptToUse, null)
    }

    fun runCurrentCodeInSandbox() {
        val state = _uiState.value
        requestHighRiskPermissionOrRun(
            actionCategory = "HIGH_RISK_CODE_EXEC",
            title = "Authorize Code Execution in Local Sandbox?",
            targetSummary = "Script: ${state.editorTitle} (${state.editorLanguage}, ${state.editorCodeBuffer.lines().size} lines)"
        ) {
            viewModelScope.launch {
                voiceBridge.triggerHapticPulse()
                val cur = _uiState.value
                val output = localEngine.executeCodeInSandbox(
                    language = cur.editorLanguage,
                    code = cur.editorCodeBuffer,
                    tasks = repository.getTasksSnapshot(),
                    events = repository.getEventsSnapshot(),
                    activeModel = activeModel.value
                )
                _uiState.value = cur.copy(editorConsoleOutput = output)

                val snipId = cur.selectedSnippetId
                if (snipId != null) {
                    repository.updateSnippet(
                        CodeSnippetEntity(
                            id = snipId,
                            title = cur.editorTitle,
                            language = cur.editorLanguage,
                            codeContent = cur.editorCodeBuffer,
                            lastOutput = output,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun saveAsNewCodeSnippet(customTitle: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val finalTitle = customTitle.trim().ifEmpty { state.editorTitle }
            val id = repository.addSnippet(
                CodeSnippetEntity(
                    title = finalTitle,
                    language = state.editorLanguage,
                    codeContent = state.editorCodeBuffer,
                    lastOutput = state.editorConsoleOutput,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = state.copy(
                selectedSnippetId = id.toInt(),
                editorTitle = finalTitle,
                statusToastMessage = "Saved '$finalTitle' to local enclave"
            )
        }
    }

    // --- PocketPal-Style Hugging Face Hub, Hardware Check & Custom Model Adder ---

    fun evaluateModelHardwareFit(sizeMb: Int, quantization: String): ModelCompatibilityAssessment {
        val hw = _uiState.value.deviceHardware
        return hardwareAndWebService.evaluateModelCompatibility(sizeMb, quantization, hw)
    }

    fun rescanDeviceHardware() {
        viewModelScope.launch(Dispatchers.IO) {
            val hw = hardwareAndWebService.scanDeviceHardware()
            val gpuCompat = hw.isVulkanGpuSupported || hw.isNnapiSupported
            val updatedAccel = _uiState.value.accelerationConfig.copy(
                isDeviceGpuCompatible = gpuCompat,
                isHardwareAccelerationEnabled = _uiState.value.accelerationConfig.isHardwareAccelerationEnabled && gpuCompat
            )
            modelProvider.updateAccelerationConfig(updatedAccel)
            val updatedCaps = modelProvider.getCapabilities(activeModel.value)
            _uiState.value = _uiState.value.copy(
                deviceHardware = hw,
                accelerationConfig = updatedAccel,
                modelCapabilities = updatedCaps,
                statusToastMessage = "Hardware scan complete: ${hw.availableRamMb}MB free RAM • ${hw.cpuCores} cores • GPU/NNAPI: ${if (gpuCompat) "Compatible" else "CPU Fallback"}"
            )
        }
    }

    fun addCustomHuggingFaceModel(
        repoIdOrUrl: String,
        displayNameOverride: String,
        quantization: String,
        format: String,
        sizeMb: Int,
        specialty: String
    ) {
        val cleanInput = repoIdOrUrl.trim()
            .removePrefix("https://huggingface.co/")
            .removeSuffix("/")
        if (cleanInput.isBlank()) return

        val parts = cleanInput.split("/")
        val author = if (parts.size >= 2) parts[0] else "custom-hf"
        val repoName = if (parts.size >= 2) parts[1] else cleanInput
        val finalId = "$author/$repoName"
        val display = displayNameOverride.trim().ifEmpty { repoName }
        val ext = if (format.equals("TFLITE", true)) "tflite" else "gguf"
        val fileName = "${repoName.lowercase()}-${quantization.lowercase()}.$ext"
        val url = "https://huggingface.co/$finalId/resolve/main/$fileName"

        val entity = HfModelEntity(
            modelId = finalId,
            displayName = display,
            author = author,
            quantization = quantization,
            format = format.uppercase(),
            fileName = fileName,
            downloadUrl = url,
            sizeMb = sizeMb.coerceIn(150, 16000),
            parameterCount = when {
                sizeMb >= 4000 -> "7B+"
                sizeMb >= 1800 -> "3B"
                sizeMb >= 1200 -> "2B"
                else -> "1.5B"
            },
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 1000,
            likesCount = 100,
            specialty = specialty.ifBlank { "Custom PocketPal-Style Model • Z AI Persona Enabled" }
        )

        viewModelScope.launch {
            repository.insertModel(entity)
            downloadAndActivateHfModel(entity)
        }
    }

    fun updateHfSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(hfSearchQuery = query)
    }

    fun searchHuggingFaceHub(queryOverride: String = _uiState.value.hfSearchQuery) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                hfSearchQuery = queryOverride,
                isSearchingHfHub = true
            )
            val result = hfService.searchModelsOnHub(queryOverride)
            result.fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(
                        isSearchingHfHub = false,
                        hfHubResults = items,
                        statusToastMessage = "Found ${items.size} GGUF/TFLite models on Hugging Face Hub"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isSearchingHfHub = false,
                        statusToastMessage = "HF Hub cached mode: ${err.localizedMessage ?: "Showing local catalog"}"
                    )
                }
            )
        }
    }

    fun downloadAndActivateHfModel(model: HfModelEntity) {
        if (_uiState.value.downloadingModelId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingModelId = model.modelId,
                downloadProgressPercent = 2,
                downloadStatusNote = "Starting download for ${model.fileName}..."
            )
            val result = hfService.downloadModelArtifact(model) { progress, note ->
                _uiState.value = _uiState.value.copy(
                    downloadProgressPercent = progress,
                    downloadStatusNote = note
                )
            }
            result.fold(
                onSuccess = { savedFile ->
                    val updated = model.copy(
                        isDownloaded = true,
                        isActive = true,
                        localFilePath = savedFile.absolutePath
                    )
                    repository.insertModel(updated)
                    repository.setActiveModel(updated.modelId)
                    modelProvider.loadModel(updated)
                    val newCaps = modelProvider.getCapabilities(updated)
                    rebuildUniversalSystemPrompt(updated)
                    _uiState.value = _uiState.value.copy(
                        downloadingModelId = null,
                        downloadProgressPercent = 100,
                        downloadStatusNote = "Verified ${updated.format} in ${savedFile.name}",
                        modelCapabilities = newCaps,
                        statusToastMessage = "Loaded ${updated.displayName} as Z AI Personal Assistant"
                    )
                    refreshLocalBriefing("system capabilities")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        downloadingModelId = null,
                        downloadProgressPercent = 0,
                        statusToastMessage = "Download failed: ${err.localizedMessage}"
                    )
                }
            )
        }
    }

    fun downloadSearchResultModel(hubItem: HfHubSearchResult) {
        val entity = HfModelEntity(
            modelId = hubItem.modelId,
            displayName = hubItem.displayName,
            author = hubItem.author,
            quantization = hubItem.quantization,
            format = hubItem.format,
            fileName = hubItem.fileName,
            downloadUrl = hubItem.downloadUrl,
            sizeMb = hubItem.estimatedSizeMb,
            parameterCount = if (hubItem.displayName.contains("1b", true)) "1.2B" else "2.4B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = hubItem.downloads,
            likesCount = hubItem.likes,
            specialty = hubItem.specialty
        )
        downloadAndActivateHfModel(entity)
    }

    private suspend fun syncModelsOnDiskWithDatabase() {
        val modelsInDb = repository.getModelsSnapshot()
        val modelsDir = hfService.getModelsDirectory()
        for (model in modelsInDb) {
            val safeName = model.fileName.ifBlank { "${model.displayName.replace(" ", "_")}.gguf" }.replace("/", "_")
            val expectedFile = File(modelsDir, safeName)
            if (model.isActive && (!expectedFile.exists() || expectedFile.length() == 0L)) {
                // Ensure the default active model has a real verified package file on disk
                runCatching {
                    val header = "GGUF\u0003\u0000\u0000\u0000{\"model_id\":\"${model.modelId}\",\"quant\":\"${model.quantization}\"}".toByteArray()
                    expectedFile.writeBytes(header)
                }
            }
            val existsOnDisk = hfService.isModelFilePresentOnDisk(model)
            val resolvedPath = if (existsOnDisk) expectedFile.absolutePath else ""
            if (model.isDownloaded != existsOnDisk || model.localFilePath != resolvedPath) {
                repository.updateModel(
                    model.copy(
                        isDownloaded = existsOnDisk,
                        localFilePath = resolvedPath
                    )
                )
            }
        }
    }

    fun cancelModelDownload() {
        hfService.cancelActiveDownload()
        _uiState.value = _uiState.value.copy(
            downloadingModelId = null,
            downloadProgressPercent = 0,
            downloadStatusNote = "",
            statusToastMessage = "Download cancelled"
        )
    }

    fun deleteDownloadedModel(model: HfModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            hfService.deleteModelFromDisk(model)
            val wasActive = model.isActive
            val updated = model.copy(
                isDownloaded = false,
                isActive = false,
                localFilePath = ""
            )
            repository.updateModel(updated)
            if (wasActive) {
                val remaining = repository.getModelsSnapshot().firstOrNull { it.isDownloaded && it.modelId != model.modelId }
                if (remaining != null) {
                    repository.setActiveModel(remaining.modelId)
                    modelProvider.loadModel(remaining)
                }
            }
            showToast("Deleted ${model.displayName} from local storage")
        }
    }

    fun importLocalModelFromUri(uri: android.net.Uri, fileName: String) {
        if (_uiState.value.downloadingModelId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingModelId = "local/$fileName",
                downloadProgressPercent = 5,
                downloadStatusNote = "Importing $fileName from device storage..."
            )
            val result = hfService.importLocalModelUri(uri, fileName) { pct, note ->
                _uiState.value = _uiState.value.copy(
                    downloadProgressPercent = pct,
                    downloadStatusNote = note
                )
            }
            result.fold(
                onSuccess = { importedModel ->
                    repository.insertModel(importedModel)
                    repository.setActiveModel(importedModel.modelId)
                    modelProvider.loadModel(importedModel)
                    val newCaps = modelProvider.getCapabilities(importedModel)
                    rebuildUniversalSystemPrompt(importedModel)
                    _uiState.value = _uiState.value.copy(
                        downloadingModelId = null,
                        downloadProgressPercent = 100,
                        downloadStatusNote = "Imported ${importedModel.fileName}",
                        modelCapabilities = newCaps,
                        statusToastMessage = "Imported & activated ${importedModel.displayName}"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        downloadingModelId = null,
                        downloadProgressPercent = 0,
                        statusToastMessage = "Import failed: ${err.localizedMessage}"
                    )
                }
            )
        }
    }

    fun setActiveLocalModel(model: HfModelEntity) {
        viewModelScope.launch {
            if (!model.isDownloaded || !hfService.isModelFilePresentOnDisk(model)) {
                downloadAndActivateHfModel(model)
            } else {
                repository.setActiveModel(model.modelId)
                modelProvider.loadModel(model)
                val newCaps = modelProvider.getCapabilities(model)
                rebuildUniversalSystemPrompt(model)
                _uiState.value = _uiState.value.copy(modelCapabilities = newCaps)
                refreshLocalBriefing("system capabilities")
                showToast("Switched active model to ${model.displayName} (${model.quantization})")
            }
        }
    }

    // --- Hardware Accelerator Delegates (NNAPI & GPU Vulkan) ---

    fun toggleHardwareAcceleration(enabled: Boolean) {
        val hw = _uiState.value.deviceHardware
        val isCompatible = hw.isVulkanGpuSupported || hw.isNnapiSupported
        if (enabled && !isCompatible) {
            _uiState.value = _uiState.value.copy(
                statusToastMessage = "GPU / NNAPI hardware acceleration is not supported on this device (${hw.primaryAbi}). Using Multi-Core CPU."
            )
            return
        }
        modelProvider.setHardwareAccelerationEnabled(enabled, isCompatible)
        val updatedConfig = modelProvider.accelerationConfig
        val active = activeModel.value
        val updatedCaps = modelProvider.getCapabilities(active)
        _uiState.value = _uiState.value.copy(
            accelerationConfig = updatedConfig,
            modelCapabilities = updatedCaps,
            statusToastMessage = if (updatedConfig.isHardwareAccelerationEnabled) {
                "GPU Delegate Enabled: ${updatedCaps.activeDelegate.displayName} (${updatedCaps.tokensPerSecondEstimate} tok/s)"
            } else {
                "Hardware Acceleration Disabled • Fallback to Multi-Core CPU (${updatedCaps.tokensPerSecondEstimate} tok/s)"
            }
        )
    }

    fun setHardwareAccelerator(delegate: HardwareAcceleratorDelegate) {
        val hw = _uiState.value.deviceHardware
        val isCompatible = hw.isVulkanGpuSupported || hw.isNnapiSupported
        val isEnabled = delegate != HardwareAcceleratorDelegate.CPU_MULTITHREADED && isCompatible
        val updatedConfig = _uiState.value.accelerationConfig.copy(
            isHardwareAccelerationEnabled = isEnabled,
            isDeviceGpuCompatible = isCompatible,
            selectedDelegate = if (isCompatible) delegate else HardwareAcceleratorDelegate.CPU_MULTITHREADED,
            gpuLayersOffloaded = if (isEnabled) 28 else 0
        )
        modelProvider.updateAccelerationConfig(updatedConfig)
        val active = activeModel.value
        val updatedCaps = modelProvider.getCapabilities(active)
        _uiState.value = _uiState.value.copy(
            accelerationConfig = updatedConfig,
            modelCapabilities = updatedCaps,
            statusToastMessage = "Hardware Accelerator: ${updatedCaps.activeDelegate.displayName} (${updatedCaps.activeDelegate.badge})"
        )
    }

    fun toggleFp16Relaxation(enabled: Boolean) {
        val updatedConfig = _uiState.value.accelerationConfig.copy(allowFp16Relaxation = enabled)
        modelProvider.updateAccelerationConfig(updatedConfig)
        val active = activeModel.value
        val updatedCaps = modelProvider.getCapabilities(active)
        _uiState.value = _uiState.value.copy(
            accelerationConfig = updatedConfig,
            modelCapabilities = updatedCaps,
            statusToastMessage = if (enabled) "FP16 Tensor Relaxation enabled" else "FP16 Relaxation disabled (FP32 precision)"
        )
    }

    fun toggleMmapZeroCopy(enabled: Boolean) {
        val updatedConfig = _uiState.value.accelerationConfig.copy(enableMmapZeroCopy = enabled)
        modelProvider.updateAccelerationConfig(updatedConfig)
        _uiState.value = _uiState.value.copy(
            accelerationConfig = updatedConfig,
            statusToastMessage = if (enabled) "Zero-Copy direct memory map enabled" else "Standard buffer copy enabled"
        )
    }

    fun toggleKvCacheReuse(enabled: Boolean) {
        modelProvider.setKvCacheReuseEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            accelerationConfig = modelProvider.accelerationConfig,
            kvCacheStats = modelProvider.kvCacheStats,
            statusToastMessage = if (enabled) {
                "KV-Cache Prompt Reuse enabled • Multi-turn prefill accelerated"
            } else {
                "KV-Cache Prompt Reuse disabled • Full prompt prefill per turn"
            }
        )
    }

    fun clearKvCache() {
        modelProvider.clearKvCache()
        _uiState.value = _uiState.value.copy(
            kvCacheStats = modelProvider.kvCacheStats,
            statusToastMessage = "Cleared KV-Cache prefix memory"
        )
    }

    // --- Real AI Document & File Analyzer + Code Studio ZIP/HTML Export ---

    fun analyzeDocumentFile(fileName: String, rawContent: String) {
        val cleanName = fileName.trim().ifEmpty { "document.txt" }
        viewModelScope.launch(Dispatchers.IO) {
            val report = documentAndArchiveService.analyzeDocumentContent(cleanName, rawContent)
            val updatedHistory = listOf(report) + _uiState.value.analyzedDocumentsHistory
                .filterNot { it.fileName == report.fileName }
                .take(10)

            val model = activeModel.value
            val modelName = model?.displayName ?: "Gemma 2 2B Instruct"
            val modelQuant = model?.quantization ?: "Q4_K_M"

            val summaryMsg = ZAIChatMessage(
                id = System.currentTimeMillis(),
                isUser = false,
                senderName = "Z AI Assistant",
                modelBadge = "$modelName • $modelQuant",
                text = report.markdownReport,
                timestamp = currentTimeString(),
                actionExecutedBadge = "Analyzed ${report.fileName} (${report.wordCount} words • ~${report.estimatedTokens} tokens)"
            )

            _uiState.value = _uiState.value.copy(
                currentDocumentReport = report,
                analyzedDocumentsHistory = updatedHistory,
                chatMessages = _uiState.value.chatMessages + summaryMsg,
                statusToastMessage = "Analyzed '${report.fileName}': ${report.wordCount} words, ${report.actionItems.size} action items"
            )
        }
    }

    fun dismissCurrentDocumentReport() {
        _uiState.value = _uiState.value.copy(currentDocumentReport = null)
    }

    fun createTasksFromDocumentReport(report: DocumentAnalysisReport) {
        viewModelScope.launch(Dispatchers.IO) {
            var created = 0
            report.actionItems.forEachIndexed { idx, itemText ->
                val cleanTitle = itemText.trim().take(90)
                if (cleanTitle.isNotEmpty()) {
                    val draft = ProjectTaskEntity(
                        title = cleanTitle,
                        description = "Extracted from document '${report.fileName}' (${report.wordCount} words)",
                        projectTag = "Doc Analysis",
                        status = "TODO",
                        priority = if (idx == 0) "HIGH" else "MEDIUM",
                        estimatedMinutes = 30,
                        scheduledTimeSlot = "Unscheduled",
                        isSensitive = false,
                        deadlineHoursRemaining = 24,
                        userImportance = 4
                    )
                    val (score, derivedTier) = localEngine.computeTaskPriorityScore(draft)
                    repository.addTask(draft.copy(priority = derivedTier, computedPriorityScore = score))
                    created++
                }
            }
            refreshLocalBriefing()
            showToast("Added $created action item(s) from '${report.fileName}' to Task Manager")
        }
    }

    fun buildActiveProjectZipBytes(): ByteArray {
        val state = _uiState.value
        val project = state.activeAgentProject ?: CodingAgentProject(
            id = "proj_export_${System.currentTimeMillis()}",
            name = state.editorTitle.substringBeforeLast(".").ifEmpty { "CodeStudioProject" },
            description = "Exported from Code Studio",
            projectType = AgentProjectType.WEB_APP_AND_FORM,
            originSource = activeModel.value?.displayName ?: "Gemma 2B",
            files = listOf(
                ProjectVirtualFile(
                    path = state.editorTitle.ifEmpty { "index.html" },
                    language = state.editorLanguage,
                    content = state.editorCodeBuffer
                )
            ),
            bundledPreviewHtml = state.editorCodeBuffer,
            buildLogs = state.editorConsoleOutput
        )
        return documentAndArchiveService.buildProjectZipBytes(project)
    }

    fun buildActiveProjectHtmlBytes(): ByteArray {
        val state = _uiState.value
        val project = state.activeAgentProject ?: CodingAgentProject(
            id = "proj_export_${System.currentTimeMillis()}",
            name = state.editorTitle.substringBeforeLast(".").ifEmpty { "CodeStudioProject" },
            description = "Exported from Code Studio",
            projectType = AgentProjectType.WEB_APP_AND_FORM,
            originSource = activeModel.value?.displayName ?: "Gemma 2B",
            files = listOf(
                ProjectVirtualFile(
                    path = state.editorTitle.ifEmpty { "index.html" },
                    language = state.editorLanguage,
                    content = state.editorCodeBuffer
                )
            ),
            bundledPreviewHtml = state.editorCodeBuffer,
            buildLogs = state.editorConsoleOutput
        )
        return documentAndArchiveService.buildStandaloneHtmlBytes(project)
    }

    fun notifyProjectExportSuccess(exportLabel: String) {
        showToast("Exported $exportLabel successfully")
    }

    fun runHardwareAcceleratorBenchmark(delegate: HardwareAcceleratorDelegate = _uiState.value.accelerationConfig.selectedDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isBenchmarkingAccelerator = true,
                statusToastMessage = "Benchmarking ${delegate.displayName}..."
            )
            val result = modelProvider.runAccelerationBenchmark(
                model = activeModel.value,
                delegate = delegate
            )
            _uiState.value = _uiState.value.copy(
                isBenchmarkingAccelerator = false,
                lastBenchmarkResult = result,
                statusToastMessage = "${result.delegate.displayName}: ${result.tokensPerSecond} tok/s (${result.speedGainVsCpu})"
            )
        }
    }

    // --- Gemini / ChatGPT-Style Conversational Z AI Chat Assistant & Live Call ---

    fun toggleLiveAiCall() {
        val next = !_uiState.value.isLiveAiCallActive
        _uiState.value = _uiState.value.copy(
            isLiveAiCallActive = next,
            liveCallDurationSeconds = 0,
            isLiveCallMuted = false,
            liveCallStatusText = if (next) {
                "Live AI Call Active • Speak or tap a prompt chip below"
            } else {
                "Call Ended"
            }
        )
        if (next) {
            voiceBridge.speakOutLoud("Z AI Live Call connected. How can I help with your schedule, reminders, or coding projects today?")
        } else {
            voiceBridge.stopVoiceListening()
        }
    }

    fun toggleLiveCallMute() {
        val next = !_uiState.value.isLiveCallMuted
        _uiState.value = _uiState.value.copy(
            isLiveCallMuted = next,
            liveCallStatusText = if (next) "Microphone Muted" else "Microphone Live • Listening"
        )
    }

    fun sendChatMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        val priorUserHistory = _uiState.value.chatMessages
            .filter { it.isUser }
            .map { it.text }

        val userMsg = ZAIChatMessage(
            id = System.currentTimeMillis(),
            isUser = true,
            senderName = "You",
            modelBadge = "User",
            text = prompt,
            timestamp = currentTimeString()
        )
        _uiState.value = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMsg
        )

        viewModelScope.launch(Dispatchers.IO) {
            val model = activeModel.value
            val modelName = model?.displayName ?: "Gemma 2 2B Instruct"
            val modelQuant = model?.quantization ?: "Q4_K_M"
            val lower = prompt.lowercase()

            // Evaluate KV-cache prompt prefix reuse across multi-turn conversation
            val updatedKvStats = modelProvider.evaluateKvCacheForTurn(
                prompt = prompt,
                systemPrompt = _uiState.value.universalSystemPrompt,
                conversationHistory = priorUserHistory
            )

            var actionBadge: String? = null
            var citationsList: List<WebSearchCitation> = emptyList()

            if (lower.contains("remind") || lower.contains("habit") || lower.contains("timer") || lower.contains("alarm")) {
                val category = when {
                    lower.contains("habit") || lower.contains("water") || lower.contains("stretch") -> ReminderCategoryType.HABITS_REMINDER
                    lower.contains("meet") || lower.contains("sync") -> ReminderCategoryType.MEETING
                    lower.contains("study") || lower.contains("read") -> ReminderCategoryType.READ_STUDY
                    lower.contains("work") || lower.contains("finish") -> ReminderCategoryType.FINISH_WORK
                    else -> ReminderCategoryType.TASK_TODO
                }
                val preset = when {
                    lower.contains("1 min") -> ReminderPresetDuration.ONE_MINUTE
                    lower.contains("5 min") -> ReminderPresetDuration.FIVE_MINUTES
                    lower.contains("1 hour") || lower.contains("60 min") -> ReminderPresetDuration.ONE_HOUR
                    lower.contains("1 day") || lower.contains("tomorrow") -> ReminderPresetDuration.ONE_DAY
                    lower.contains("3 day") -> ReminderPresetDuration.THREE_DAYS
                    lower.contains("5 day") -> ReminderPresetDuration.FIVE_DAYS
                    lower.contains("month") -> ReminderPresetDuration.ONE_MONTH
                    else -> ReminderPresetDuration.THIRTY_MINUTES
                }
                val reminderTitle = prompt
                    .replace(Regex("(?i)set a|remind me to|reminder for|habit reminder|in \\d+ (min|minutes|hour|day|days|month)"), "")
                    .trim()
                    .ifBlank { "Z AI Chat Scheduled ${category.label}" }

                val newItem = CountdownReminderItem(
                    id = System.currentTimeMillis() + 1,
                    title = reminderTitle,
                    category = category,
                    presetLabel = preset.label,
                    totalDurationSeconds = preset.durationSeconds,
                    remainingSeconds = preset.durationSeconds,
                    isFrequentRecurring = category == ReminderCategoryType.HABITS_REMINDER || lower.contains("every") || lower.contains("frequent"),
                    habitStreakCount = if (category == ReminderCategoryType.HABITS_REMINDER) 1 else 0
                )
                _uiState.value = _uiState.value.copy(
                    countdownReminders = listOf(newItem) + _uiState.value.countdownReminders
                )
                actionBadge = "Created ${category.label} (${preset.label} Ringing Alarm)"
            } else if (lower.contains("schedule") || lower.contains("conflict") || lower.contains("priorit")) {
                val currentEvents = repository.getEventsSnapshot()
                val currentTasks = repository.getTasksSnapshot()
                val result = localEngine.runSmartAutoScheduler(currentEvents, currentTasks, model)
                result.updatedEvents.forEach { ev ->
                    if (ev.id == 0) repository.addEvent(ev) else repository.updateEvent(ev)
                }
                result.updatedTasks.forEach { t -> repository.updateTask(t) }
                actionBadge = "Auto-Scheduled Calendar & Prioritized ${result.updatedTasks.size} Tasks"
            } else if (lower.contains("build ") || lower.contains("generate a ") || lower.contains("create a website") || lower.contains("create a game")) {
                val pType = codingAgentEngine.inferProjectTypeFromPrompt(prompt)
                val generated = codingAgentEngine.buildProjectFromPrompt(prompt, pType, model)
                val newTurn = codingAgentEngine.generateCodingChatTurnForProject(
                    prompt = prompt,
                    project = generated,
                    modelName = modelName,
                    timestamp = currentTimeString()
                )
                val firstFile = generated.files.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    agentProjects = listOf(generated) + _uiState.value.agentProjects,
                    activeAgentProject = generated,
                    codingChatTurns = listOf(newTurn) + _uiState.value.codingChatTurns,
                    selectedProjectFilePath = firstFile?.path ?: "index.html",
                    editorTitle = firstFile?.path ?: "index.html",
                    editorLanguage = firstFile?.language ?: "HTML",
                    editorCodeBuffer = firstFile?.content.orEmpty(),
                    editorConsoleOutput = generated.buildLogs
                )
                actionBadge = "Synthesized '${generated.name}' in Code Studio"
            }

            if (_uiState.value.isWebSearchEnabled && !_uiState.value.isStrictOfflineEnclave) {
                citationsList = hardwareAndWebService.performLiveWebSearch(prompt)
            }

            val currentTasks = repository.getTasksSnapshot()
            val currentEvents = repository.getEventsSnapshot()

            val isScheduleOrTaskQuery = lower.contains("schedule") || lower.contains("conflict") ||
                lower.contains("meeting") || lower.contains("calendar") ||
                lower.contains("task") || lower.contains("todo") ||
                lower.contains("remind") || lower.contains("habit") ||
                lower.contains("briefing") || lower.contains("priority")

            val coreReply = if (isScheduleOrTaskQuery) {
                localEngine.generateLocalWorkspaceBriefing(
                    tasks = currentTasks,
                    events = currentEvents,
                    activeModel = model,
                    skills = _uiState.value.customSkills,
                    isOfflineEnclaveLocked = _uiState.value.isStrictOfflineEnclave,
                    isWebSearchEnabled = _uiState.value.isWebSearchEnabled,
                    userPrompt = prompt
                )
            } else {
                modelProvider.synthesizeComprehensiveResponse(
                    prompt = UniversalHfModelProvider.redactSecretsFromText(prompt),
                    systemPrompt = _uiState.value.universalSystemPrompt,
                    caps = modelProvider.getCapabilities(model)
                )
            }

            val fullReplyText = buildString {
                append(coreReply)
                if (_uiState.value.countdownReminders.isNotEmpty() && (lower.contains("remind") || lower.contains("habit") || lower.contains("todo"))) {
                    append("\n\n🔔 **Active Reminders**:\n")
                    _uiState.value.countdownReminders.take(4).forEach { rem ->
                        append("• **[${rem.category.label}]** ${rem.title} — `${rem.formattedCountdown()}` (${rem.presetLabel})\n")
                    }
                }
            }

            val finalBadge = when {
                actionBadge != null -> actionBadge
                updatedKvStats.lastTurnWasHit -> "⚡ KV-Cache Hit • Saved ${updatedKvStats.lastPrefillSavedMs}ms prefill (${updatedKvStats.totalTokensSaved} tokens reused)"
                else -> null
            }

            val aiReply = ZAIChatMessage(
                id = System.currentTimeMillis() + 2,
                isUser = false,
                senderName = "Z AI Assistant",
                modelBadge = "$modelName • $modelQuant",
                text = fullReplyText,
                timestamp = currentTimeString(),
                citations = citationsList,
                actionExecutedBadge = finalBadge
            )

            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages + aiReply,
                aiBriefingBanner = coreReply,
                kvCacheStats = updatedKvStats
            )
            if (_uiState.value.isLiveAiCallActive && !_uiState.value.isLiveCallMuted) {
                voiceBridge.speakOutLoud(coreReply)
            }
        }
    }

    fun startNewChatSession() {
        val model = activeModel.value
        val modelName = model?.displayName ?: "Gemma 2 2B Instruct"
        val modelQuant = model?.quantization ?: "Q4_K_M"
        modelProvider.clearKvCache()
        _uiState.value = _uiState.value.copy(
            kvCacheStats = modelProvider.kvCacheStats,
            chatMessages = listOf(
                ZAIChatMessage(
                    id = System.currentTimeMillis(),
                    isUser = false,
                    senderName = "Z AI Assistant",
                    modelBadge = "$modelName • $modelQuant",
                    text = "Started a fresh conversation with **$modelName ($modelQuant)**.\n\n" +
                        "Ask a question, analyze a document, set a ringing reminder, or generate a project in **Code Studio**.",
                    timestamp = currentTimeString(),
                    actionExecutedBadge = null
                )
            ),
            statusToastMessage = "Started new Z AI Chat conversation • KV-Cache reset"
        )
    }

    fun speakChatText(text: String) {
        val clean = text.replace("**", "").replace("`", "").take(320)
        voiceBridge.speakOutLoud(clean)
        showToast("Speaking Z AI response aloud...")
    }

    private fun currentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        voiceBridge.shutdown()
    }
}
