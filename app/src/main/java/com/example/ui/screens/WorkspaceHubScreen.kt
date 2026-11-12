package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CalendarEventEntity
import com.example.data.HfModelEntity
import com.example.data.ProjectTaskEntity
import com.example.domain.SensitiveDataAuditReport
import com.example.domain.SuggestedMeetingSlot
import com.example.domain.WebSearchCitation
import com.example.ui.components.CountdownReminderItem
import com.example.ui.components.ReminderCategoryType
import com.example.ui.components.ReminderPresetDuration
import com.example.ui.components.TaskReminderCountdownCard
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.NeuralViolet

@Composable
fun WorkspaceHubScreen(
    tasks: List<ProjectTaskEntity>,
    events: List<CalendarEventEntity>,
    activeModel: HfModelEntity?,
    aiBriefing: String,
    suggestedSlots: List<SuggestedMeetingSlot>,
    isWebSearchEnabled: Boolean,
    isSearchingWeb: Boolean,
    webCitations: List<WebSearchCitation>,
    countdownReminders: List<CountdownReminderItem> = emptyList(),
    activeReminderAlertMessage: String? = null,
    sensitiveAuditReport: SensitiveDataAuditReport? = null,
    onExecuteSensitiveInsightAction: (String) -> Unit = {},
    onAddCountdownReminder: (String, ReminderCategoryType, ReminderPresetDuration, Boolean) -> Unit = { _, _, _, _ -> },
    onTogglePauseReminder: (Long) -> Unit = {},
    onTriggerReminderAlertNow: (CountdownReminderItem) -> Unit = {},
    onSnoozeReminderFiveMin: () -> Unit = {},
    onDismissReminderAlert: () -> Unit = {},
    onDeleteReminder: (Long) -> Unit = {},
    onToggleWebSearch: (Boolean) -> Unit,
    onSearchWebNow: (String) -> Unit,
    onOpenCitationUrl: (String) -> Unit,
    onRunAutoScheduler: () -> Unit,
    onRunPrioritizeOnly: () -> Unit,
    onBookSuggestedSlot: (SuggestedMeetingSlot) -> Unit,
    onAskLocalAi: (String) -> Unit,
    onAdvanceTaskStatus: (ProjectTaskEntity) -> Unit,
    onCycleTaskImportance: (ProjectTaskEntity, Int, Int) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onCreateTask: (String, String, String, String, Int, Boolean, Int, Int) -> Unit,
    onCreateEvent: (String, Int, Int, Int, String, String) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onSyncSystemCalendar: () -> Unit
) {
    var selectedViewMode by rememberSaveable { mutableStateOf("UNIFIED") }
    var selectedKanbanStage by rememberSaveable { mutableStateOf("ALL") }
    var localAiQuestion by rememberSaveable { mutableStateOf("") }
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
    var showAddEventDialog by rememberSaveable { mutableStateOf(false) }

    val conflictCount = events.count { it.hasConflict }
    val sensitiveCount = tasks.count { it.isSensitive }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workspace_hub_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Z AI Personal Assistant & Local Gemma 2B Intelligence Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_workspace_hero_1794370713403),
                        contentDescription = "Z AI Assistant Neural Timeline Hero",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color(0xFF090D16).copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = EmeraldEnclave.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.border(
                                    1.dp,
                                    EmeraldEnclave.copy(alpha = 0.6f),
                                    RoundedCornerShape(50)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Z AI Privacy Enclave",
                                        tint = EmeraldEnclave,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Z AI Assistant • $sensitiveCount Sensitive Tasks",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = NeuralViolet.copy(alpha = 0.32f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = activeModel?.let { "${it.displayName} (${it.quantization})" }
                                        ?: "Gemma 2 2B Q4_K_M",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(44.dp))

                        Text(
                            text = "Gemma 2B Schedule & Task Intelligence",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Z AI Self-Aware Briefing Box
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = aiBriefing,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Quick Self-Introspection & Capability Prompts
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val quickPrompts = listOf(
                            "System capabilities & enclave status",
                            "Analyze conflicts & optimal meeting slots",
                            "Prioritize tasks by deadline & importance",
                            "Can you search the web right now?"
                        )
                        items(quickPrompts) { prompt ->
                            SuggestionChip(
                                onClick = { onAskLocalAi(prompt) },
                                label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("zai_prompt_chip_${prompt.take(10).replace(" ", "_")}")
                            )
                        }
                    }

                    // Ask Z AI + Permission-Gated Web Search Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = localAiQuestion,
                            onValueChange = { localAiQuestion = it },
                            placeholder = {
                                Text(
                                    "Ask Z AI (${activeModel?.displayName ?: "Gemma 2B"}) or search web...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("local_ai_query_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                onAskLocalAi(localAiQuestion)
                                localAiQuestion = ""
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("local_ai_ask_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Ask Z AI",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Web Search Grounding Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Web Search Grounding",
                                tint = if (isWebSearchEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWebSearchEnabled) {
                                    "Web Search: ENABLED (Permission-Gated)"
                                } else {
                                    "Web Search: OFF (Strict Local Enclave)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isWebSearchEnabled) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isWebSearchEnabled) {
                                TextButton(
                                    onClick = {
                                        onSearchWebNow(localAiQuestion.ifBlank { "Gemma 2B Android scheduling optimization" })
                                    },
                                    modifier = Modifier.testTag("search_web_now_button")
                                ) {
                                    Text("Search Web", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Switch(
                                checked = isWebSearchEnabled,
                                onCheckedChange = onToggleWebSearch,
                                modifier = Modifier.testTag("web_search_toggle_switch")
                            )
                        }
                    }

                    if (isSearchingWeb) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = ElectricCyan
                        )
                    }

                    // Render Live Web Citations if present
                    AnimatedVisibility(visible = webCitations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Live Web Search Grounding Citations:",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            webCitations.forEach { citation ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenCitationUrl(citation.url) }
                                        .border(
                                            1.dp,
                                            ElectricCyan.copy(alpha = 0.35f),
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${citation.title} (${citation.sourceDomain})",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ElectricCyan
                                            )
                                            Text(
                                                text = citation.snippet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open Source Link",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dual Smart Action Buttons: Auto-Schedule + Deadline Prioritizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunAutoScheduler,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("auto_schedule_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (conflictCount > 0) AmberConflict else ElectricCyan,
                                contentColor = Color(0xFF04131A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Smart Auto-Scheduler",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (conflictCount > 0) {
                                    "Fix $conflictCount Conflicts & Schedule"
                                } else {
                                    "Auto-Schedule All"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onRunPrioritizeOnly,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("prioritize_tasks_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Prioritize by Deadline & Importance",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rank Tasks",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // 1B. Gemma 2B Local Sensitive Data Processing & Offline Smart Insights Enclave Card
        if (sensitiveAuditReport != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            EmeraldEnclave.copy(alpha = 0.65f),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("local_sensitive_insights_card"),
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
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Gemma 2B Sensitive Enclave",
                                    tint = EmeraldEnclave,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Gemma 2B Sensitive Project Data Enclave",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = sensitiveAuditReport.privacyGuaranteeBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldEnclave
                                    )
                                }
                            }

                            Surface(
                                color = EmeraldEnclave.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${sensitiveAuditReport.enclaveProtectionScore}% Shield",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldEnclave,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Enclave KPIs Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${sensitiveAuditReport.sensitiveTaskCount}/${sensitiveAuditReport.totalTasksAudited}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Sensitive Tasks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${sensitiveAuditReport.redactedFieldCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmberConflict,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "PII/Tokens Shielded",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "0 KB",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldEnclave,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Cloud Egress",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Offline Smart Insights List with One-Tap Actions
                        sensitiveAuditReport.insights.forEach { insight ->
                            val sevColor = when (insight.severityBadge) {
                                "HIGH RISK" -> CrimsonAlert
                                "OPTIMIZATION" -> ElectricCyan
                                else -> EmeraldEnclave
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, sevColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .testTag("sensitive_insight_item_${insight.id}")
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
                                            text = insight.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = sevColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = insight.severityBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = sevColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = insight.insightSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { onExecuteSensitiveInsightAction(insight.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                                            modifier = Modifier.testTag("execute_sensitive_insight_${insight.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = sevColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = insight.recommendedOfflineAction,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = sevColor,
                                                fontWeight = FontWeight.Bold
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

        // 2. Gemma 2B Suggested Optimal Meeting Times Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gemma 2B Suggested Optimal Meeting Slots",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Tap slot to book",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(suggestedSlots, key = { it.id }) { slot ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(235.dp)
                                .border(
                                    1.dp,
                                    EmeraldEnclave.copy(alpha = 0.55f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onBookSuggestedSlot(slot) }
                                .testTag("book_suggested_slot_${slot.id}")
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
                                        text = slot.formattedWindow,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldEnclave,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = EmeraldEnclave.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${slot.confidencePercent}% Match",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldEnclave,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = slot.rationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2B. Z AI Smart Countdown & Frequent Reminder Center (1m, 5m, 30m, 1h, 1d, 3d, 5d, 1mo)
        item {
            TaskReminderCountdownCard(
                reminders = countdownReminders,
                activeFiredAlertMessage = activeReminderAlertMessage,
                tasks = tasks,
                events = events,
                onAddCountdownReminder = onAddCountdownReminder,
                onTogglePauseReminder = onTogglePauseReminder,
                onTriggerReminderAlertNow = onTriggerReminderAlertNow,
                onSnoozeFiredAlertFiveMin = onSnoozeReminderFiveMin,
                onDismissFiredAlert = onDismissReminderAlert,
                onDeleteReminder = onDeleteReminder
            )
        }

        // 3. View Mode Switcher + Quick Add Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "UNIFIED" to "Unified",
                        "KANBAN" to "Kanban (${tasks.size})",
                        "TIMELINE" to "Timeline (${events.size})"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = selectedViewMode == mode,
                            onClick = { selectedViewMode = mode },
                            label = { Text(label) },
                            modifier = Modifier.testTag("view_mode_$mode"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showAddTaskDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_add_task_dialog_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Task", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Task", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { showAddEventDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_add_event_dialog_button")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "New Event", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Event", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 4. KANBAN PROJECT SECTION
        if (selectedViewMode == "UNIFIED" || selectedViewMode == "KANBAN") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewKanban,
                                contentDescription = "Kanban Board",
                                tint = ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deadline & Importance Kanban",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = "${tasks.count { it.status == "DONE" }}/${tasks.size} Done",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldEnclave
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val stages = listOf(
                            "ALL" to "All (${tasks.size})",
                            "TODO" to "To Do (${tasks.count { it.status == "TODO" }})",
                            "IN_PROGRESS" to "In Progress (${tasks.count { it.status == "IN_PROGRESS" }})",
                            "DONE" to "Done (${tasks.count { it.status == "DONE" }})"
                        )
                        items(stages) { (stageKey, stageLabel) ->
                            FilterChip(
                                selected = selectedKanbanStage == stageKey,
                                onClick = { selectedKanbanStage = stageKey },
                                label = { Text(stageLabel) },
                                modifier = Modifier.testTag("kanban_filter_$stageKey")
                            )
                        }
                    }
                }
            }

            val filteredTasks = if (selectedKanbanStage == "ALL") {
                tasks
            } else {
                tasks.filter { it.status == selectedKanbanStage }
            }

            items(filteredTasks, key = { "task_${it.id}" }) { task ->
                KanbanTaskCard(
                    task = task,
                    onAdvanceStatus = { onAdvanceTaskStatus(task) },
                    onCycleImportance = {
                        val nextStars = if (task.userImportance >= 5) 1 else task.userImportance + 1
                        onCycleTaskImportance(task, nextStars, task.deadlineHoursRemaining)
                    },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }

        // 5. CALENDAR TIMELINE SECTION
        if (selectedViewMode == "UNIFIED" || selectedViewMode == "TIMELINE") {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Calendar Timeline",
                            tint = NeuralViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time-Blocked Schedule",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    TextButton(
                        onClick = onSyncSystemCalendar,
                        modifier = Modifier.testTag("sync_system_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Export to Android Calendar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync OS Calendar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            items(events, key = { "event_${it.id}" }) { event ->
                CalendarEventCard(
                    event = event,
                    onDelete = { onDeleteEvent(event.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddTaskDialog) {
        AddProjectTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, tag, priority, mins, sensitive, deadlineH, stars ->
                onCreateTask(title, desc, tag, priority, mins, sensitive, deadlineH, stars)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddEventDialog) {
        AddCalendarEventDialog(
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, h, m, dur, attendees, cat ->
                onCreateEvent(title, h, m, dur, attendees, cat)
                showAddEventDialog = false
            }
        )
    }
}

@Composable
private fun KanbanTaskCard(
    task: ProjectTaskEntity,
    onAdvanceStatus: () -> Unit,
    onCycleImportance: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "CRITICAL" -> CrimsonAlert
        "HIGH" -> AmberConflict
        "MEDIUM" -> ElectricCyan
        else -> EmeraldEnclave
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${task.priority} • ${task.computedPriorityScore}",
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Interactive User Importance Stars chip
                    Surface(
                        color = NeuralViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onCycleImportance() }
                            .testTag("task_importance_chip_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "User Importance Rating",
                                tint = AmberConflict,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${task.userImportance}★",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Deadline Badge
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.deadlineBadgeText(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.deadlineHoursRemaining <= 12) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time Slot",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.projectTag} • ${task.estimatedMinutes}m • ${task.scheduledTimeSlot.ifBlank { "Unscheduled" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onAdvanceStatus,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("advance_task_status_${task.id}")
                ) {
                    Text(
                        text = when (task.status) {
                            "TODO" -> "Start Task"
                            "IN_PROGRESS" -> "Mark Done"
                            else -> "Reopen"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Advance Kanban Stage",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEventCard(
    event: CalendarEventEntity,
    onDelete: () -> Unit
) {
    val accentColor = when {
        event.hasConflict -> AmberConflict
        event.isAutoScheduled -> EmeraldEnclave
        else -> NeuralViolet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (event.hasConflict) 1.5.dp else 1.dp,
                color = accentColor.copy(alpha = if (event.hasConflict) 0.85f else 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("calendar_event_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(86.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%02d:%02d", event.startHour, event.startMinute),
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${event.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.hasConflict) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Conflict Warning",
                            tint = AmberConflict,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SCHEDULING CONFLICT",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberConflict,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (event.isAutoScheduled) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Auto-Scheduled",
                            tint = EmeraldEnclave,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "GEMMA 2B AUTO-SCHEDULED",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldEnclave,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuralViolet
                        )
                    }
                }

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${event.formattedTimeRange()} • ${event.attendees}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (event.hasConflict && event.conflictReason.isNotBlank()) {
                    Text(
                        text = event.conflictReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberConflict
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Calendar Event",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddProjectTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Int, Boolean, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var projectTag by remember { mutableStateOf("Security Enclave") }
    var priority by remember { mutableStateOf("HIGH") }
    var minutesText by remember { mutableStateOf("45") }
    var deadlineHoursText by remember { mutableStateOf("12") }
    var importanceStars by remember { mutableStateOf(5) }
    var isSensitive by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project Task (Gemma 2B Prioritized)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_task_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Technical Notes / Sub-tasks") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = projectTag,
                        onValueChange = { projectTag = it },
                        label = { Text("Project Tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Est. Mins") },
                        singleLine = true,
                        modifier = Modifier.width(85.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = deadlineHoursText,
                        onValueChange = { deadlineHoursText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Deadline (Hours)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Importance: ${importanceStars}★", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                FilterChip(
                                    selected = importanceStars == star,
                                    onClick = { importanceStars = star },
                                    label = { Text("$star") }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark Sensitive (Local Enclave Only)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isSensitive, onCheckedChange = { isSensitive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        description,
                        projectTag,
                        priority,
                        minutesText.toIntOrNull() ?: 45,
                        isSensitive,
                        deadlineHoursText.toIntOrNull() ?: 12,
                        importanceStars
                    )
                },
                modifier = Modifier.testTag("confirm_add_task_button")
            ) {
                Text("Add & Auto-Rank")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hourText by remember { mutableStateOf("11") }
    var minuteText by remember { mutableStateOf("00") }
    var durationText by remember { mutableStateOf("45") }
    var attendees by remember { mutableStateOf("eng-team@zai.local") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Calendar Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Meeting or Focus Block Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_event_title_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter { c -> c.isDigit() } },
                        label = { Text("Hour (0-23)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { c -> c.isDigit() } },
                        label = { Text("Min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                        label = { Text("Dur (m)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = attendees,
                    onValueChange = { attendees = it },
                    label = { Text("Attendees / Channel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        hourText.toIntOrNull() ?: 11,
                        minuteText.toIntOrNull() ?: 0,
                        durationText.toIntOrNull() ?: 45,
                        attendees,
                        "MEETING"
                    )
                },
                modifier = Modifier.testTag("confirm_add_event_button")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
