package com.example.data

import kotlinx.coroutines.flow.Flow

class OmniRepository(private val dao: OmniDao) {

    val allTasks: Flow<List<ProjectTaskEntity>> = dao.getAllTasks()
    val allEvents: Flow<List<CalendarEventEntity>> = dao.getAllEvents()
    val allMacros: Flow<List<WorkflowMacroEntity>> = dao.getAllMacros()
    val allSnippets: Flow<List<CodeSnippetEntity>> = dao.getAllSnippets()
    val allModels: Flow<List<HfModelEntity>> = dao.getAllModels()

    suspend fun ensureSeedDataInitialized() {
        val existingModels = dao.getAllModelsSnapshot()
        if (existingModels.isEmpty()) {
            dao.insertModels(defaultHfModels())
        }

        val existingTasks = dao.getAllTasksSnapshot()
        if (existingTasks.isEmpty()) {
            dao.insertTasks(defaultProjectTasks())
        }

        val existingEvents = dao.getAllEventsSnapshot()
        if (existingEvents.isEmpty()) {
            dao.insertEvents(defaultCalendarEvents())
        }

        val existingMacros = dao.getAllMacrosSnapshot()
        if (existingMacros.isEmpty()) {
            dao.insertMacros(defaultWorkflowMacros())
        }

        val existingSnippets = dao.getAllSnippetsSnapshot()
        if (existingSnippets.isEmpty()) {
            dao.insertSnippets(defaultCodeSnippets())
        }
    }

    suspend fun addTask(task: ProjectTaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: ProjectTaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(taskId: Int) = dao.deleteTask(taskId)
    suspend fun getTasksSnapshot() = dao.getAllTasksSnapshot()

    suspend fun addEvent(event: CalendarEventEntity) = dao.insertEvent(event)
    suspend fun updateEvent(event: CalendarEventEntity) = dao.updateEvent(event)
    suspend fun deleteEvent(eventId: Int) = dao.deleteEvent(eventId)
    suspend fun getEventsSnapshot() = dao.getAllEventsSnapshot()

    suspend fun addMacro(macro: WorkflowMacroEntity) = dao.insertMacro(macro)
    suspend fun updateMacro(macro: WorkflowMacroEntity) = dao.updateMacro(macro)
    suspend fun deleteMacro(macroId: Int) = dao.deleteMacro(macroId)
    suspend fun getMacrosSnapshot() = dao.getAllMacrosSnapshot()

    suspend fun addSnippet(snippet: CodeSnippetEntity) = dao.insertSnippet(snippet)
    suspend fun updateSnippet(snippet: CodeSnippetEntity) = dao.updateSnippet(snippet)
    suspend fun deleteSnippet(snippetId: Int) = dao.deleteSnippet(snippetId)

    suspend fun insertModel(model: HfModelEntity) = dao.insertModel(model)
    suspend fun updateModel(model: HfModelEntity) = dao.updateModel(model)
    suspend fun setActiveModel(modelId: String) = dao.setActiveModel(modelId)
    suspend fun getModelsSnapshot() = dao.getAllModelsSnapshot()

    private fun defaultHfModels(): List<HfModelEntity> = listOf(
        HfModelEntity(
            modelId = "google/gemma-2-2b-it-GGUF",
            displayName = "Gemma 2 2B Instruct",
            author = "google",
            quantization = "Q4_K_M",
            format = "GGUF",
            fileName = "gemma-2-2b-it-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            sizeMb = 1630,
            parameterCount = "2.6B",
            isDownloaded = true,
            isActive = true,
            localFilePath = "",
            downloadsCount = 184500,
            likesCount = 940,
            specialty = "Default Z AI Engine • Schedule, Conflict & Deadline Reasoning"
        ),
        HfModelEntity(
            modelId = "Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF",
            displayName = "Qwen 2.5 Coder 1.5B",
            author = "Qwen",
            quantization = "Q4_K_M",
            format = "GGUF",
            fileName = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            sizeMb = 1120,
            parameterCount = "1.5B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 142300,
            likesCount = 815,
            specialty = "Code Studio • Polyglot Synthesis & Refactoring"
        ),
        HfModelEntity(
            modelId = "litert-community/Gemma-2-2B-IT-TFLite",
            displayName = "Gemma 2 2B LiteRT (NPU)",
            author = "litert-community",
            quantization = "INT4-TFLite",
            format = "TFLITE",
            fileName = "gemma2_2b_it_gpu_int4.tflite",
            downloadUrl = "https://huggingface.co/litert-community/Gemma-2-2B-IT-TFLite/resolve/main/gemma2_2b_it_gpu_int4.tflite",
            sizeMb = 1390,
            parameterCount = "2.6B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 67400,
            likesCount = 520,
            specialty = "Ultra-Low Latency NPU/GPU Voice & Intent Parsing"
        ),
        HfModelEntity(
            modelId = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            displayName = "Llama 3.2 1B Instruct",
            author = "bartowski",
            quantization = "Q8_0",
            format = "GGUF",
            fileName = "Llama-3.2-1B-Instruct-Q8_0.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf",
            sizeMb = 1320,
            parameterCount = "1.2B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 219000,
            likesCount = 1120,
            specialty = "Fast Autonomous Macro & Meeting Summarizer"
        ),
        HfModelEntity(
            modelId = "unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            displayName = "DeepSeek R1 Distill 1.5B",
            author = "unsloth",
            quantization = "Q4_K_M",
            format = "GGUF",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            sizeMb = 1110,
            parameterCount = "1.5B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 312000,
            likesCount = 1650,
            specialty = "Chain-of-Thought Reasoning & Complex Planning"
        ),
        HfModelEntity(
            modelId = "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF",
            displayName = "Llama 3.1 8B Instruct (Heavy)",
            author = "bartowski",
            quantization = "Q4_K_M",
            format = "GGUF",
            fileName = "Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
            sizeMb = 4920,
            parameterCount = "8.0B",
            isDownloaded = false,
            isActive = false,
            localFilePath = "",
            downloadsCount = 410000,
            likesCount = 2300,
            specialty = "High-Capacity Flagship Model (Requires 8GB+ Free RAM)"
        )
    )

    private fun defaultProjectTasks(): List<ProjectTaskEntity> = listOf(
        ProjectTaskEntity(
            title = "Audit zero-trust encryption keys for Q4 release",
            description = "Verify AES-256-GCM key rotation schedule in local enclave storage without cloud telemetry.",
            projectTag = "Security Enclave",
            status = "TODO",
            priority = "CRITICAL",
            estimatedMinutes = 60,
            scheduledTimeSlot = "Unscheduled",
            isSensitive = true,
            deadlineHoursRemaining = 6,
            userImportance = 5,
            computedPriorityScore = 223
        ),
        ProjectTaskEntity(
            title = "Optimize Gemma 2B KV-cache memory footprint",
            description = "Reduce context buffer allocation from 512MB to 320MB for 4K token sequences on mobile RAM.",
            projectTag = "AI Runtime",
            status = "IN_PROGRESS",
            priority = "HIGH",
            estimatedMinutes = 45,
            scheduledTimeSlot = "10:30 - 11:15",
            isSensitive = true,
            deadlineHoursRemaining = 18,
            userImportance = 5,
            computedPriorityScore = 199
        ),
        ProjectTaskEntity(
            title = "Finalize API contract for offline calendar sync",
            description = "Map bidirectional conflict resolution rules between Kanban deadlines and CalDAV blocks.",
            projectTag = "Core Engine",
            status = "TODO",
            priority = "HIGH",
            estimatedMinutes = 45,
            scheduledTimeSlot = "Unscheduled",
            isSensitive = false,
            deadlineHoursRemaining = 24,
            userImportance = 4,
            computedPriorityScore = 152
        ),
        ProjectTaskEntity(
            title = "Implement voice macro confirmation haptics",
            description = "Add distinct vibration feedback patterns when multi-step autonomous phone workflows complete.",
            projectTag = "Mobile UI",
            status = "IN_PROGRESS",
            priority = "MEDIUM",
            estimatedMinutes = 30,
            scheduledTimeSlot = "13:30 - 14:00",
            isSensitive = false,
            deadlineHoursRemaining = 48,
            userImportance = 3,
            computedPriorityScore = 79
        ),
        ProjectTaskEntity(
            title = "Benchmark Qwen 2.5 Coder snippet sandbox",
            description = "Verify sub-50ms local evaluation of Kotlin and Python automation scripts.",
            projectTag = "AI Runtime",
            status = "DONE",
            priority = "MEDIUM",
            estimatedMinutes = 30,
            scheduledTimeSlot = "09:00 - 09:30",
            isSensitive = true,
            deadlineHoursRemaining = 72,
            userImportance = 3,
            computedPriorityScore = 85
        )
    )

    private fun defaultCalendarEvents(): List<CalendarEventEntity> = listOf(
        CalendarEventEntity(
            title = "Daily Architecture Standup",
            startHour = 9,
            startMinute = 30,
            durationMinutes = 30,
            attendees = "eng-core@zai.local",
            locationOrLink = "Room 4B / Encrypted Audio",
            category = "STANDUP",
            hasConflict = false,
            conflictReason = "",
            isAutoScheduled = false
        ),
        CalendarEventEntity(
            title = "Security Enclave Threat Modeling",
            startHour = 10,
            startMinute = 30,
            durationMinutes = 60,
            attendees = "sec-ops@zai.local",
            locationOrLink = "Local Vault Bridge",
            category = "MEETING",
            hasConflict = true,
            conflictReason = "Overlaps by 30m with Product Roadmap & Sprint Review (11:00)",
            isAutoScheduled = false
        ),
        CalendarEventEntity(
            title = "Product Roadmap & Sprint Review",
            startHour = 11,
            startMinute = 0,
            durationMinutes = 45,
            attendees = "product@zai.local",
            locationOrLink = "Huddle Link #204",
            category = "MEETING",
            hasConflict = true,
            conflictReason = "Overlaps by 30m with Security Enclave Threat Modeling (10:30–11:30)",
            isAutoScheduled = false
        ),
        CalendarEventEntity(
            title = "Deep Work: Gemma 2B Quantization Tuning",
            startHour = 14,
            startMinute = 0,
            durationMinutes = 60,
            attendees = "Solo Focus Block",
            locationOrLink = "Offline Enclave Mode",
            category = "DEEP_WORK",
            hasConflict = false,
            conflictReason = "",
            isAutoScheduled = true
        )
    )

    private fun defaultWorkflowMacros(): List<WorkflowMacroEntity> = listOf(
        WorkflowMacroEntity(
            title = "New Task Auto-Triage & Priority Pipeline",
            triggerPhrase = "triage new task",
            description = "Triggered when a new task is added: advances urgent tasks to IN_PROGRESS, recalculates priority scores, time-blocks conflicts, and posts a notification.",
            stepsPipeSeparated = "SEND_NOTIFICATION|UPDATE_TASK_STATUS|RESCHEDULE_EVENTS|SPEAK_BRIEFING",
            targetPhoneOrEmail = "eng-lead@zai.local",
            runCount = 4,
            lastRunSummary = "Auto-triaged task statuses, resolved conflicts & sent alert",
            triggerEventType = "ON_TASK_ADDED",
            isAutoTriggerEnabled = true,
            lastTriggeredByEvent = "ON_TASK_ADDED"
        ),
        WorkflowMacroEntity(
            title = "Meeting Scheduled Conflict Resolver & Prep",
            triggerPhrase = "meeting conflict check",
            description = "Triggered when a meeting is scheduled: automatically reschedules overlapping events, updates linked task statuses, and sends a schedule notification.",
            stepsPipeSeparated = "RESCHEDULE_EVENTS|UPDATE_TASK_STATUS|SEND_NOTIFICATION|SPEAK_BRIEFING",
            targetPhoneOrEmail = "team@zai.local",
            runCount = 3,
            lastRunSummary = "Rescheduled overlapping events & notified attendees",
            triggerEventType = "ON_MEETING_SCHEDULED",
            isAutoTriggerEnabled = true,
            lastTriggeredByEvent = "ON_MEETING_SCHEDULED"
        ),
        WorkflowMacroEntity(
            title = "Approaching Deadline Rescue & Focus Sprint",
            triggerPhrase = "deadline rescue",
            description = "Triggered when a task deadline is approaching (≤12h): promotes task status to IN_PROGRESS, clears calendar conflicts, starts a focus countdown timer, and alerts you.",
            stepsPipeSeparated = "SEND_NOTIFICATION|UPDATE_TASK_STATUS|RESCHEDULE_EVENTS|CREATE_COUNTDOWN_TIMER",
            targetPhoneOrEmail = "sec-ops@zai.local",
            runCount = 5,
            lastRunSummary = "Escalated ≤12h deadline task, cleared schedule & started timer",
            triggerEventType = "ON_DEADLINE_APPROACHING",
            isAutoTriggerEnabled = true,
            lastTriggeredByEvent = "ON_DEADLINE_APPROACHING"
        ),
        WorkflowMacroEntity(
            title = "Emergency Incident Escalation & Call Lead",
            triggerPhrase = "escalate incident",
            description = "Summarizes sensitive security tasks via Gemma 2B, drafts an encrypted status email, and dials the on-call lead (Permission-Gated).",
            stepsPipeSeparated = "ANALYZE_SENSITIVE_TASKS|GENERATE_INCIDENT_SCRIPT|SEND_EMAIL:sec-ops@zai.local|DIAL_NUMBER:+15550192834",
            targetPhoneOrEmail = "+15550192834",
            runCount = 1,
            lastRunSummary = "Requires user approval gate before calling/emailing",
            triggerEventType = "MANUAL_OR_VOICE",
            isAutoTriggerEnabled = false
        )
    )

    private fun defaultCodeSnippets(): List<CodeSnippetEntity> = listOf(
        CodeSnippetEntity(
            title = "Gemma 2B Task Priority & Conflict Scorer.kt",
            language = "Kotlin",
            codeContent = """
                // Executed locally inside Z AI Assistant Sandbox
                fun scoreTaskUrgency(importanceStars: Int, deadlineHours: Int, conflicts: Int): String {
                    val urgencyScore = (importanceStars * 25) + (100 - deadlineHours * 2).coerceAtLeast(0)
                    val tier = if (urgencyScore >= 170) "CRITICAL" else if (urgencyScore >= 120) "HIGH" else "MEDIUM"
                    println("Z AI Priority Engine -> Tier: " + tier + " (Score: " + urgencyScore + ")")
                    println("Active Calendar Conflicts Detected: " + conflicts)
                    return tier
                }
                
                scoreTaskUrgency(importanceStars = 5, deadlineHours = 6, conflicts = 2)
            """.trimIndent(),
            lastOutput = "[Z AI Sandbox • Gemma-2-2B Verified]\n> Z AI Priority Engine -> Tier: CRITICAL (Score: 213)\n> Active Calendar Conflicts Detected: 2\n> Exit Code: 0 (Zero network egress verified)"
        ),
        CodeSnippetEntity(
            title = "GGUF Hardware Capacity Estimator.py",
            language = "Python",
            codeContent = """
                # Calculate mobile RAM footprint for quantized GGUF models
                def estimate_ram_mb(params_billions, bits_per_weight, context_k):
                    weights_mb = params_billions * 1024 * (bits_per_weight / 8.0)
                    kv_cache_mb = context_k * 64.0
                    total_mb = round(weights_mb + kv_cache_mb + 180)
                    print(f"Model Weights: {round(weights_mb)} MB | KV Cache: {round(kv_cache_mb)} MB")
                    print(f"Total Enclave RAM Required: {total_mb} MB")
                    return total_mb

                estimate_ram_mb(params_billions=2.6, bits_per_weight=4.5, context_k=4)
            """.trimIndent(),
            lastOutput = "[Z AI Sandbox • Python Runtime]\n> Model Weights: 1498 MB | KV Cache: 256 MB\n> Total Enclave RAM Required: 1934 MB\n> Exit Code 0 (11ms)"
        ),
        CodeSnippetEntity(
            title = "Sensitive Task Deadline Audit.sql",
            language = "SQL",
            codeContent = """
                -- Local SQLite Enclave Audit for Urgent Sensitive Work
                SELECT id, title, priority, deadlineHoursRemaining, userImportance
                FROM project_tasks
                WHERE isSensitive = 1
                  AND status != 'DONE'
                ORDER BY computedPriorityScore DESC;
            """.trimIndent(),
            lastOutput = "[SQLite Enclave Query Result]\n• #1 [CRITICAL] Audit zero-trust encryption keys (Due in 6h • 5★)\n• #2 [HIGH] Optimize Gemma 2B KV-cache memory footprint (Due in 18h • 5★)\n2 sensitive rows returned locally."
        )
    )
}
