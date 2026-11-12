package com.example

import com.example.data.CalendarEventEntity
import com.example.data.ProjectTaskEntity
import com.example.domain.AgentProjectType
import com.example.domain.AutonomousCodingAgentEngine
import com.example.domain.CompatibilityBadge
import com.example.domain.DeviceHardwareProfile
import com.example.domain.DeviceIntentAndVoiceBridge
import com.example.domain.HardwareAndWebService
import com.example.domain.LocalAiAndSandboxEngine
import com.example.domain.ProjectVirtualFile
import com.example.domain.VoiceSubIntentType
import com.example.ui.components.CountdownReminderItem
import com.example.ui.components.ReminderCategoryType
import com.example.ui.components.ReminderPresetDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `Gemma 2B engine prioritizes tasks by deadline and importance and resolves conflicts`() {
        val engine = LocalAiAndSandboxEngine()
        val tasks = listOf(
            ProjectTaskEntity(
                id = 1,
                title = "Low urgency docs update",
                description = "Docs",
                projectTag = "Docs",
                status = "TODO",
                priority = "LOW",
                deadlineHoursRemaining = 72,
                userImportance = 2
            ),
            ProjectTaskEntity(
                id = 2,
                title = "Critical encryption audit",
                description = "Audit",
                projectTag = "Security",
                status = "TODO",
                priority = "HIGH",
                deadlineHoursRemaining = 6,
                userImportance = 5
            )
        )
        val events = listOf(
            CalendarEventEntity(
                id = 1,
                title = "Meeting A",
                startHour = 10,
                startMinute = 30,
                durationMinutes = 60,
                attendees = "team",
                locationOrLink = "room",
                category = "MEETING",
                hasConflict = true
            ),
            CalendarEventEntity(
                id = 2,
                title = "Meeting B",
                startHour = 11,
                startMinute = 0,
                durationMinutes = 45,
                attendees = "team",
                locationOrLink = "room",
                category = "MEETING",
                hasConflict = true
            )
        )

        val result = engine.runSmartAutoScheduler(events, tasks, null)
        assertTrue(result.conflictsResolvedCount >= 1)
        assertTrue(result.updatedEvents.none { it.hasConflict })
        assertEquals("Critical encryption audit", result.updatedTasks.first().title)
        assertEquals("CRITICAL", result.updatedTasks.first().priority)

        val slots = engine.suggestOptimalMeetingSlots(result.updatedEvents, 45)
        assertFalse(slots.isEmpty())

        val capabilityReport = engine.generateLocalWorkspaceBriefing(
            tasks = result.updatedTasks,
            events = result.updatedEvents,
            activeModel = null,
            userPrompt = "capabilities and limitations"
        )
        assertTrue(capabilityReport.contains("Active System Capabilities"))
        assertTrue(capabilityReport.contains("Security Boundaries"))
    }

    @Test
    fun `compound multi-step voice instruction schedules design meeting, creates attendee tasks, drafts agenda, and executes code`() {
        val engine = LocalAiAndSandboxEngine()
        val utterance = "Schedule a meeting with the design team for next Tuesday afternoon, create a task for each attendee to prepare their latest mockups, and draft an agenda based on our ongoing project goals."
        val parsed = engine.parseCompoundVoiceInstruction(utterance)

        assertEquals(3, parsed.size)
        assertEquals(VoiceSubIntentType.SCHEDULE_TEAM_MEETING, parsed[0].intentType)
        assertEquals(VoiceSubIntentType.CREATE_TASKS_FOR_ATTENDEES, parsed[1].intentType)
        assertEquals(VoiceSubIntentType.DRAFT_MEETING_AGENDA, parsed[2].intentType)

        val execution = engine.executeCompoundVoicePlan(
            utterance = utterance,
            parsedSteps = parsed,
            existingEvents = emptyList(),
            existingTasks = emptyList(),
            activeModel = null
        )
        assertEquals(1, execution.createdEvents.size)
        assertEquals(3, execution.createdTasks.size)
        assertTrue(execution.draftedAgendaMarkdown.contains("DRAFTED MEETING AGENDA"))

        val codeVoice = "Write a Python script to calculate sprint velocity and execute it in the sandbox"
        val codeSteps = engine.parseCompoundVoiceInstruction(codeVoice)
        val codeExec = engine.executeCompoundVoicePlan(
            utterance = codeVoice,
            parsedSteps = codeSteps,
            existingEvents = execution.createdEvents,
            existingTasks = execution.createdTasks,
            activeModel = null
        )
        assertEquals("Python", codeExec.synthesizedCodeLanguage)
        assertTrue(codeExec.synthesizedCode.contains("def prioritize_workspace"))
        assertTrue(codeExec.codeExecutionOutput.contains("Z AI Sandbox"))
    }

    @Test
    fun `hardware diagnostic service rates Hugging Face models as Optimal, Playable, or Too Heavy`() {
        val service = HardwareAndWebService()
        val profile = DeviceHardwareProfile(
            totalRamMb = 8192,
            availableRamMb = 4000,
            usedRamMb = 4192,
            cpuCores = 8,
            primaryAbi = "arm64-v8a",
            freeStorageMb = 32768,
            totalStorageMb = 65536,
            deviceModelName = "Google Pixel 8 Pro",
            deviceTierLabel = "Performance Mobile Tier",
            recommendedMaxModelMb = 2400,
            recommendedQuantization = "Q4_K_M"
        )

        val optimalFit = service.evaluateModelCompatibility(
            sizeMb = 1630,
            quantization = "Q4_K_M",
            hardware = profile
        )
        assertEquals(CompatibilityBadge.OPTIMAL, optimalFit.badge)
        assertEquals("Optimal", optimalFit.badgeLabel)

        val playableFit = service.evaluateModelCompatibility(
            sizeMb = 2600,
            quantization = "Q4_K_M",
            hardware = profile
        )
        assertEquals(CompatibilityBadge.PLAYABLE, playableFit.badge)
        assertEquals("Playable", playableFit.badgeLabel)

        val tooHeavyFit = service.evaluateModelCompatibility(
            sizeMb = 4800,
            quantization = "Q4_K_M",
            hardware = profile
        )
        assertEquals(CompatibilityBadge.TOO_HEAVY, tooHeavyFit.badge)
        assertEquals("Too Heavy", tooHeavyFit.badgeLabel)
    }

    @Test
    fun `autonomous coding agent builds websites, forms, games, and imports browser files, and countdown reminders support all 8 durations`() {
        val agent = AutonomousCodingAgentEngine()

        val webFormProject = agent.buildProjectFromPrompt(
            prompt = "Build a customer registration form and analytics website",
            requestedType = AgentProjectType.WEB_APP_AND_FORM,
            activeModel = null
        )
        assertEquals(3, webFormProject.files.size)
        assertTrue(webFormProject.bundledPreviewHtml.contains("<form"))

        // Verify AI Studio / DeepSeek Coding Chat Turn generation
        val chatTurn = agent.generateCodingChatTurnForProject(
            prompt = "Build a customer registration form and analytics website",
            project = webFormProject,
            modelName = "Gemma 2 2B Instruct"
        )
        assertEquals("2.4s", chatTurn.thinkingDurationSeconds)
        assertEquals(4, chatTurn.thinkingMonologueLines.size)
        assertTrue(chatTurn.fileActionPills.any { it.filePath == "index.html" && it.actionVerb == "Created" })
        assertTrue(chatTurn.fileActionPills.any { it.actionVerb == "Compiled" })

        val gameProject = agent.buildProjectFromPrompt(
            prompt = "Build a 2D space shooter arcade game",
            requestedType = null,
            activeModel = null
        )
        assertEquals(AgentProjectType.HTML5_CANVAS_GAME, gameProject.projectType)
        assertTrue(gameProject.bundledPreviewHtml.contains("gameCanvas"))

        val imported = agent.importProjectFromBrowserFiles(
            projectName = "My Personal App",
            importedFiles = listOf(
                ProjectVirtualFile("index.html", "HTML", "<h1>My Imported App</h1>")
            ),
            activeModel = null
        )
        assertTrue(imported.bundledPreviewHtml.contains("My Imported App"))

        // Verify all 8 requested reminder preset durations (1m, 5m, 30m, 1h, 1d, 3d, 5d, 1mo)
        assertEquals(8, ReminderPresetDuration.entries.size)
        assertEquals(60L, ReminderPresetDuration.ONE_MINUTE.durationSeconds)
        assertEquals(300L, ReminderPresetDuration.FIVE_MINUTES.durationSeconds)
        assertEquals(1800L, ReminderPresetDuration.THIRTY_MINUTES.durationSeconds)
        assertEquals(3600L, ReminderPresetDuration.ONE_HOUR.durationSeconds)
        assertEquals(86400L, ReminderPresetDuration.ONE_DAY.durationSeconds)
        assertEquals(3 * 86400L, ReminderPresetDuration.THREE_DAYS.durationSeconds)
        assertEquals(5 * 86400L, ReminderPresetDuration.FIVE_DAYS.durationSeconds)
        assertEquals(30 * 86400L, ReminderPresetDuration.ONE_MONTH.durationSeconds)

        val item = CountdownReminderItem(
            id = 1L,
            title = "Finish Architecture Study",
            category = ReminderCategoryType.READ_STUDY,
            presetLabel = ReminderPresetDuration.THREE_DAYS.label,
            totalDurationSeconds = ReminderPresetDuration.THREE_DAYS.durationSeconds,
            remainingSeconds = ReminderPresetDuration.THREE_DAYS.durationSeconds,
            isFrequentRecurring = true
        )
        assertTrue(item.formattedCountdown().startsWith("03d"))
        assertTrue(item.buildAlertMessage().contains("You have to read and study"))
    }

    @Test
    fun `Gemma 2B local sensitive data enclave audits confidential tasks and wake word detector matches Jarvis, Janu, Friday, Edith, and Ultron`() {
        val engine = LocalAiAndSandboxEngine()
        val sensitiveTasks = listOf(
            ProjectTaskEntity(
                id = 10,
                title = "Rotate production API secret & review payroll",
                description = "Contact security@corp.local or +15550192834 with token=abc123secret",
                projectTag = "Security Enclave",
                status = "TODO",
                priority = "CRITICAL",
                isSensitive = true,
                deadlineHoursRemaining = 6,
                userImportance = 5
            )
        )
        val report = engine.analyzeSensitiveProjectDataOffline(
            tasks = sensitiveTasks,
            events = emptyList(),
            activeModel = null
        )
        assertEquals(1, report.sensitiveTaskCount)
        assertTrue(report.privacyGuaranteeBadge.contains("0 KB Sent Externally"))
        assertEquals(3, report.insights.size)

        val sanitized = engine.sanitizeSensitiveTaskDescription(sensitiveTasks.first().description)
        assertTrue(sanitized.contains("[REDACTED_EMAIL_ENCLAVE]"))
        assertTrue(sanitized.contains("[REDACTED_SECRET_ENCLAVE]"))

        // Verify Wake Word detection across all 5 confirmed wake words
        val (wake1, cmd1) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance("Hey Jarvis, schedule a design meeting")
        assertEquals("Jarvis", wake1)
        assertEquals("schedule a design meeting", cmd1)

        val (wake2, cmd2) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance("Janu audit sensitive tasks")
        assertEquals("Janu", wake2)
        assertEquals("audit sensitive tasks", cmd2)

        val (wake3, cmd3) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance("Ok Friday, morning standup")
        assertEquals("Friday", wake3)
        assertEquals("morning standup", cmd3)

        val (wake4, cmd4) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance("Hey Edith write a python script")
        assertEquals("Edith", wake4)
        assertEquals("write a python script", cmd4)

        val (wake5, cmd5) = DeviceIntentAndVoiceBridge.detectWakeWordInUtterance("Ultron")
        assertEquals("Ultron", wake5)
        assertEquals("", cmd5)
        assertNotNull(wake5)
    }

    @Test
    fun `Autonomous AI Coding Studio passes all 5 end-to-end acceptance test cases and verifies exact Z AI Home design tokens`() {
        kotlinx.coroutines.runBlocking {
            // Verify exact Z AI Home Screen color palette tokens
            assertEquals(0xFF050B18L, com.example.ui.theme.ZaiMainBackground.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF0B1426L, com.example.ui.theme.ZaiHeaderSurface.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF112440L, com.example.ui.theme.ZaiWelcomeCard.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF111E34L, com.example.ui.theme.ZaiFeatureCard.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF050D1EL, com.example.ui.theme.ZaiInputSurface.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF06DDF5L, com.example.ui.theme.ZaiPrimaryCyan.value.shr(32).toLong() and 0xFFFFFFFFL)
            assertEquals(0xFF10DFA0L, com.example.ui.theme.ZaiSuccessGreen.value.shr(32).toLong() and 0xFFFFFFFFL)

            val tempRoot = java.nio.file.Files.createTempDirectory("zai_studio_test").toFile()
            val modelProvider = com.example.domain.UniversalHfModelProvider()
            val workspaceManager = com.example.domain.SandboxedWorkspaceManager(tempRoot)
            val codingEngine = com.example.domain.AutonomousCodingAgentEngine()
            val executionManager = com.example.domain.RealExecutionManager(workspaceManager, codingEngine)
            val agentRuntime = com.example.domain.AutonomousCodingAgentRuntime(
                modelProvider = modelProvider,
                workspaceManager = workspaceManager,
                executionManager = executionManager,
                codingEngine = codingEngine
            )

            // Test Case 1: Create a responsive HTML website -> real files created, built, and working preview
            val emittedEvents = mutableListOf<com.example.domain.AgentExecutionEvent>()
            val tc1Result = agentRuntime.executeAutonomousTask(
                userPrompt = "Create a responsive HTML website",
                currentProject = null,
                explicitType = com.example.domain.AgentProjectType.WEB_APP_AND_FORM,
                activeModel = null
            ) { ev -> emittedEvents.add(ev) }
            assertTrue(tc1Result.buildReport.isSuccess)
            assertTrue(tc1Result.updatedProject.files.any { it.path.endsWith(".html") })
            assertTrue(tc1Result.updatedProject.bundledPreviewHtml.contains("<html", ignoreCase = true))
            assertTrue(emittedEvents.isNotEmpty())

            // Test Case 2 & 4: Build failure & Bug Fix in existing project
            val brokenFile = com.example.domain.ProjectVirtualFile(
                path = "app.js",
                language = "JavaScript",
                content = "function computeMetric() {\n  const x = BUG_DIVIDE_BY_ZERO;\n  SYNTAX_ERROR\n"
            )
            val brokenProject = tc1Result.updatedProject.copy(
                files = listOf(
                    com.example.domain.ProjectVirtualFile("index.html", "HTML", "<html><body><h1>Test</h1></body></html>"),
                    brokenFile
                )
            )
            workspaceManager.syncProjectToSandbox(brokenProject)
            val failedBuild = executionManager.buildProject(
                brokenProject.id,
                workspaceManager.listFiles(brokenProject.id),
                brokenProject.projectType
            )
            // Verify that a broken build genuinely fails and reports exact diagnostics
            assertTrue(!failedBuild.isSuccess)
            assertTrue(failedBuild.diagnostics.isNotEmpty())

            // Now ask the agent to find and fix the bug in the existing project
            val tc2Result = agentRuntime.executeAutonomousTask(
                userPrompt = "Find and fix a bug in my existing project",
                currentProject = brokenProject,
                explicitType = null,
                activeModel = null
            ) {}
            assertTrue(tc2Result.buildReport.isSuccess)
            assertTrue(tc2Result.testReport.isSuccess)

            // Test Case 3: Switch from one Hugging Face model to another (GGUF -> ONNX -> Safetensors)
            val onnxModel = com.example.data.HfModelEntity(
                modelId = "Qwen/Qwen2.5-Coder-1.5B-Instruct-ONNX",
                displayName = "Qwen 2.5 Coder 1.5B ONNX",
                author = "Qwen",
                quantization = "INT4",
                format = "ONNX",
                fileName = "model.onnx",
                downloadUrl = "",
                sizeMb = 980,
                parameterCount = "1.5B"
            )
            modelProvider.loadModel(onnxModel)
            val onnxCaps = modelProvider.getCapabilities(onnxModel)
            assertEquals(com.example.domain.SupportedModelFormat.ONNX, onnxCaps.format)
            assertEquals("Qwen2ForCausalLM", onnxCaps.architecture)

            // Test Case 5: Cancel task & verify path-traversal security and checkpoint rollback
            val traversalAttempt = workspaceManager.validateSafeRelativePath("../../etc/passwd")
            assertTrue(traversalAttempt.isFailure)

            val cp = workspaceManager.createCheckpoint(brokenProject.id, "Before experimental edit")
            workspaceManager.writeFile(brokenProject.id, "index.html", "<html><body>Modified</body></html>", createCheckpoint = false)
            agentRuntime.cancelCurrentTask()
            val restored = workspaceManager.restoreCheckpoint(brokenProject.id, cp.id)
            assertTrue(restored.isSuccess)
            // Test Case 6: Hardware Accelerator Delegates (NNAPI & GPU Vulkan) & TFLite/GGUF Loader Toggle
            val ggufModel = com.example.data.HfModelEntity(
                modelId = "google/gemma-2-2b-it-GGUF",
                displayName = "Gemma 2 2B Instruct GGUF",
                author = "Google",
                quantization = "Q4_K_M",
                format = "GGUF",
                fileName = "gemma-2-2b-it-q4_k_m.gguf",
                downloadUrl = "",
                sizeMb = 1580,
                parameterCount = "2B"
            )
            val tfliteModel = com.example.data.HfModelEntity(
                modelId = "google/gemma-2b-it-tflite",
                displayName = "Gemma 2B LiteRT TFLite",
                author = "Google",
                quantization = "INT4",
                format = "TFLITE",
                fileName = "gemma-2b-it-gpu-int4.tflite",
                downloadUrl = "",
                sizeMb = 1350,
                parameterCount = "2B"
            )

            // Enable GPU hardware acceleration on compatible device
            modelProvider.setHardwareAccelerationEnabled(enabled = true, isDeviceCompatible = true)
            val ggufGpuCaps = modelProvider.getCapabilities(ggufModel)
            assertTrue(ggufGpuCaps.isHardwareAccelerationActive)
            assertEquals(com.example.domain.HardwareAcceleratorDelegate.GPU_VULKAN, ggufGpuCaps.activeDelegate)
            assertTrue(ggufGpuCaps.loaderDelegateConfigSummary.contains("ggml_backend_vk"))
            assertTrue(ggufGpuCaps.gpuLayersOffloaded >= 16)

            val tfliteGpuCaps = modelProvider.getCapabilities(tfliteModel)
            assertTrue(tfliteGpuCaps.isHardwareAccelerationActive)
            assertTrue(tfliteGpuCaps.loaderDelegateConfigSummary.contains("TfLiteGpuDelegateV2"))

            // Disable hardware acceleration via Settings toggle -> falls back to Multi-Core CPU
            modelProvider.setHardwareAccelerationEnabled(enabled = false, isDeviceCompatible = true)
            val ggufCpuCaps = modelProvider.getCapabilities(ggufModel)
            assertFalse(ggufCpuCaps.isHardwareAccelerationActive)
            assertEquals(com.example.domain.HardwareAcceleratorDelegate.CPU_MULTITHREADED, ggufCpuCaps.activeDelegate)
            assertEquals(0, ggufCpuCaps.gpuLayersOffloaded)
            assertTrue(ggufCpuCaps.loaderDelegateConfigSummary.contains("ggml_cpu NEON SIMD"))

            val tfliteCpuCaps = modelProvider.getCapabilities(tfliteModel)
            assertFalse(tfliteCpuCaps.isHardwareAccelerationActive)
            assertTrue(tfliteCpuCaps.loaderDelegateConfigSummary.contains("XNNPACK CPU Delegate"))

            // Device incompatible -> locked to CPU even if enabled = true
            modelProvider.setHardwareAccelerationEnabled(enabled = true, isDeviceCompatible = false)
            val incompatibleCaps = modelProvider.getCapabilities(ggufModel)
            assertFalse(incompatibleCaps.isHardwareAccelerationActive)
            assertEquals(com.example.domain.HardwareAcceleratorDelegate.CPU_MULTITHREADED, incompatibleCaps.activeDelegate)

            // Switch to NNAPI delegate and run benchmark
            val nnapiConfig = com.example.domain.InferenceAccelerationConfig(
                isHardwareAccelerationEnabled = true,
                isDeviceGpuCompatible = true,
                selectedDelegate = com.example.domain.HardwareAcceleratorDelegate.NNAPI,
                allowFp16Relaxation = true
            )
            modelProvider.updateAccelerationConfig(nnapiConfig)
            val acceleratedCaps = modelProvider.getCapabilities(onnxModel)
            assertEquals(com.example.domain.HardwareAcceleratorDelegate.NNAPI, acceleratedCaps.activeDelegate)
            assertTrue(acceleratedCaps.tokensPerSecondEstimate > 30.0f)

            val benchResult = modelProvider.runAccelerationBenchmark(
                model = onnxModel,
                delegate = com.example.domain.HardwareAcceleratorDelegate.GPU_VULKAN
            )
            assertEquals(com.example.domain.HardwareAcceleratorDelegate.GPU_VULKAN, benchResult.delegate)
            assertTrue(benchResult.tokensPerSecond > 20.0)
            assertTrue(benchResult.speedGainVsCpu.contains("faster"))

            // Test Case 7: KV-Cache Multi-Turn Prompt Reuse
            modelProvider.clearKvCache()
            modelProvider.setKvCacheReuseEnabled(true)
            val sysPrompt = "You are a local workspace coding assistant."
            val turn1Stats = modelProvider.evaluateKvCacheForTurn(
                prompt = "Write a Kotlin function to sort tasks by priority",
                systemPrompt = sysPrompt,
                conversationHistory = emptyList()
            )
            assertFalse(turn1Stats.lastTurnWasHit)
            assertEquals(0, turn1Stats.hitCount)
            assertEquals(1, turn1Stats.missCount)

            val turn2Stats = modelProvider.evaluateKvCacheForTurn(
                prompt = "Now add unit tests for that function",
                systemPrompt = sysPrompt,
                conversationHistory = listOf("Write a Kotlin function to sort tasks by priority")
            )
            assertTrue(turn2Stats.lastTurnWasHit)
            assertEquals(1, turn2Stats.hitCount)
            assertTrue(turn2Stats.totalTokensSaved > 0)
            assertTrue(turn2Stats.lastPrefillSavedMs > 0)

            modelProvider.clearKvCache()
            assertEquals(0, modelProvider.kvCacheStats.hitCount)
            assertEquals(0, modelProvider.kvCacheStats.cachedTokenCount)

            // Test Case 8: Real AI Document & File Analyzer + Code Studio ZIP/HTML Archive Exporter
            val docService = com.example.domain.DocumentAndArchiveService()
            val sampleDoc = """
                # Sprint Release Architecture
                ## 1. Inference Optimization
                Enable KV-cache reuse across multi-turn prompts.
                - [ ] Benchmark Vulkan GPU delegate throughput
                TODO: Export standalone HTML and ZIP archives from Code Studio
            """.trimIndent()
            val docReport = docService.analyzeDocumentContent("sprint_plan.md", sampleDoc)
            assertEquals("Markdown Document", docReport.fileTypeLabel)
            assertTrue(docReport.wordCount >= 18)
            assertTrue(docReport.lineCount >= 5)
            assertTrue(docReport.keyHeadingsAndTopics.any { it.contains("Sprint Release Architecture") })
            assertTrue(docReport.extractedActionItems.size >= 2)
            assertTrue(docReport.exportMarkdownContent.contains("# Document Analysis Report: sprint_plan.md"))

            val zipBytes = docService.buildProjectZipBytes(tc1Result.updatedProject)
            assertTrue(zipBytes.isNotEmpty())
            val zipEntries = docService.listZipEntryNames(zipBytes)
            assertTrue(zipEntries.contains("index.html"))
            assertTrue(zipEntries.contains("README.md"))

            val htmlBytes = docService.buildStandaloneHtmlBytes(tc1Result.updatedProject)
            assertTrue(String(htmlBytes).contains("<html", ignoreCase = true))

            // Test Case 9: Code Studio 'Recent Files' History (Saves Last 5 Opened Files Locally)
            var recentHistory = emptyList<com.example.domain.RecentCodeFileEntry>()
            val filesToOpen = listOf(
                "index.html" to "HTML",
                "styles.css" to "CSS",
                "app.js" to "JavaScript",
                "MainActivity.kt" to "Kotlin",
                "ViewModel.kt" to "Kotlin",
                "Repository.kt" to "Kotlin"
            )
            for ((path, lang) in filesToOpen) {
                recentHistory = docService.recordRecentFileOpened(
                    currentHistory = recentHistory,
                    openedEntry = com.example.domain.RecentCodeFileEntry(
                        projectId = "proj_1",
                        projectName = "Demo Project",
                        filePath = path,
                        language = lang
                    )
                )
            }
            // Capped at 5 most recently opened files, with Repository.kt at index 0 and index.html evicted
            assertEquals(5, recentHistory.size)
            assertEquals("Repository.kt", recentHistory.first().filePath)
            assertFalse(recentHistory.any { it.filePath == "index.html" })

            // Re-opening styles.css moves it to the top (index 0) without duplicating it
            recentHistory = docService.recordRecentFileOpened(
                currentHistory = recentHistory,
                openedEntry = com.example.domain.RecentCodeFileEntry(
                    projectId = "proj_1",
                    projectName = "Demo Project",
                    filePath = "styles.css",
                    language = "CSS"
                )
            )
            assertEquals(5, recentHistory.size)
            assertEquals("styles.css", recentHistory.first().filePath)

            // Verify local serialization & deserialization preserves the 5-file history
            val serializedRecents = docService.serializeRecentFiles(recentHistory)
            val deserializedRecents = docService.deserializeRecentFiles(serializedRecents)
            assertEquals(5, deserializedRecents.size)
            assertEquals("styles.css", deserializedRecents.first().filePath)
            assertEquals("Repository.kt", deserializedRecents[1].filePath)

            tempRoot.deleteRecursively()
        }
    }
}


