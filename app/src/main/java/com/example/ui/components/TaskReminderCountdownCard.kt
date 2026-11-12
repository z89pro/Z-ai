package com.example.ui.components

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CalendarEventEntity
import com.example.data.ProjectTaskEntity
import com.example.ui.theme.AmberConflict
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldEnclave
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet

enum class ReminderPresetDuration(val label: String, val durationSeconds: Long) {
    ONE_MINUTE("1 Min", 60L),
    FIVE_MINUTES("5 Min", 5 * 60L),
    THIRTY_MINUTES("30 Min", 30 * 60L),
    ONE_HOUR("1 Hour", 60 * 60L),
    ONE_DAY("1 Day", 24 * 3600L),
    THREE_DAYS("3 Days", 3 * 24 * 3600L),
    FIVE_DAYS("5 Days", 5 * 24 * 3600L),
    ONE_MONTH("1 Month", 30 * 24 * 3600L)
}

enum class ReminderCategoryType(
    val label: String,
    val spokenPrefix: String,
    val sectionSubtitle: String
) {
    TASK_TODO(
        label = "Task To-Do",
        spokenPrefix = "You have a task to do",
        sectionSubtitle = "Action items & daily checklist"
    ),
    HABITS_REMINDER(
        label = "Habits Reminder",
        spokenPrefix = "Daily habit reminder check-in",
        sectionSubtitle = "Routines, wellness & consistency streaks"
    ),
    FINISH_WORK(
        label = "Work Reminder",
        spokenPrefix = "You have to finish this work",
        sectionSubtitle = "Deep-work sprints & deliverables"
    ),
    MEETING(
        label = "Meeting Alert",
        spokenPrefix = "You have a meeting",
        sectionSubtitle = "Team syncs & calendar calls"
    ),
    READ_STUDY(
        label = "Read & Study",
        spokenPrefix = "You have to read and study",
        sectionSubtitle = "Learning, docs & research blocks"
    ),
    FREQUENT_REMINDER(
        label = "Frequent Loop",
        spokenPrefix = "Frequent reminder check-in",
        sectionSubtitle = "Auto-repeating interval alerts"
    )
}

data class CountdownReminderItem(
    val id: Long,
    val title: String,
    val category: ReminderCategoryType,
    val presetLabel: String,
    val totalDurationSeconds: Long,
    val remainingSeconds: Long,
    val isFrequentRecurring: Boolean = false,
    val isPaused: Boolean = false,
    val firedCount: Int = 0,
    val habitStreakCount: Int = 1
) {
    val progressFraction: Float
        get() = if (totalDurationSeconds <= 0L) 0f else {
            ((totalDurationSeconds - remainingSeconds).toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        }

    fun formattedCountdown(): String {
        val secs = remainingSeconds.coerceAtLeast(0L)
        val days = secs / 86400
        val hours = (secs % 86400) / 3600
        val minutes = (secs % 3600) / 60
        val remSecs = secs % 60
        return if (days > 0) {
            String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, remSecs)
        } else {
            String.format("%02dh %02dm %02ds", hours, minutes, remSecs)
        }
    }

    fun buildAlertMessage(): String {
        return "${category.spokenPrefix}: \"$title\" ($presetLabel${if (isFrequentRecurring) " • Frequent Repeat" else ""})"
    }
}

@Composable
fun CategoryIconForReminder(category: ReminderCategoryType): ImageVector = when (category) {
    ReminderCategoryType.TASK_TODO -> Icons.Default.TaskAlt
    ReminderCategoryType.HABITS_REMINDER -> Icons.Default.Favorite
    ReminderCategoryType.FINISH_WORK -> Icons.Default.WorkOutline
    ReminderCategoryType.MEETING -> Icons.Default.Event
    ReminderCategoryType.READ_STUDY -> Icons.Default.MenuBook
    ReminderCategoryType.FREQUENT_REMINDER -> Icons.Default.Repeat
}

@Composable
fun CategoryColorForReminder(category: ReminderCategoryType): Color = when (category) {
    ReminderCategoryType.TASK_TODO -> ElectricCyan
    ReminderCategoryType.HABITS_REMINDER -> Color(0xFFF472B6)
    ReminderCategoryType.FINISH_WORK -> EmeraldEnclave
    ReminderCategoryType.MEETING -> AmberConflict
    ReminderCategoryType.READ_STUDY -> NeuralViolet
    ReminderCategoryType.FREQUENT_REMINDER -> ElectricCyan
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskReminderCountdownCard(
    reminders: List<CountdownReminderItem>,
    activeFiredAlertMessage: String?,
    tasks: List<ProjectTaskEntity>,
    events: List<CalendarEventEntity>,
    onAddCountdownReminder: (String, ReminderCategoryType, ReminderPresetDuration, Boolean) -> Unit,
    onTogglePauseReminder: (Long) -> Unit,
    onTriggerReminderAlertNow: (CountdownReminderItem) -> Unit,
    onSnoozeFiredAlertFiveMin: () -> Unit,
    onDismissFiredAlert: () -> Unit,
    onDeleteReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var reminderTitle by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ReminderCategoryType.TASK_TODO) }
    var selectedSectionFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedPreset by rememberSaveable { mutableStateOf(ReminderPresetDuration.THIRTY_MINUTES) }
    var isFrequentRepeat by rememberSaveable { mutableStateOf(false) }
    var notificationPermGranted by rememberSaveable { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermGranted = granted
    }

    val filteredReminders = if (selectedSectionFilter == "ALL") {
        reminders
    } else {
        reminders.filter { it.category.name == selectedSectionFilter }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                ElectricCyan.copy(alpha = 0.55f),
                RoundedCornerShape(22.dp)
            )
            .testTag("task_reminder_countdown_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Gradient Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E1B4B),
                                Color(0xFF064E3B)
                            )
                        )
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
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
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = "Ringing Alarm & Popup Reminders",
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Z AI Ringing Alarm & Popup Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Task To-Do • Habits • Work • Meetings • Study",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldEnclave
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermGranted) {
                        OutlinedButton(
                            onClick = {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Enable Popup Notifications",
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enable Popup", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    } else {
                        Surface(
                            color = EmeraldEnclave.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔔 Ring + Popup ON",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldEnclave,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Section Overview Pills (Task To-Do, Habits, Work, Meeting, Study)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SectionFilterCard(
                        title = "All Sections",
                        subtitle = "Every active timer",
                        count = reminders.size,
                        accent = ElectricCyan,
                        selected = selectedSectionFilter == "ALL",
                        onClick = { selectedSectionFilter = "ALL" }
                    )
                }
                items(ReminderCategoryType.entries) { cat ->
                    val count = reminders.count { it.category == cat }
                    val accent = CategoryColorForReminder(cat)
                    SectionFilterCard(
                        title = cat.label,
                        subtitle = cat.sectionSubtitle,
                        count = count,
                        accent = accent,
                        selected = selectedSectionFilter == cat.name,
                        onClick = {
                            selectedSectionFilter = cat.name
                            selectedCategory = cat
                        }
                    )
                }
            }

            // Create New Reminder Section Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "1. Choose Reminder Section:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReminderCategoryType.entries) { category ->
                        val catColor = CategoryColorForReminder(category)
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                                if (category == ReminderCategoryType.FREQUENT_REMINDER ||
                                    category == ReminderCategoryType.HABITS_REMINDER
                                ) {
                                    isFrequentRepeat = true
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconForReminder(category),
                                    contentDescription = category.label,
                                    tint = catColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = { Text(category.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("reminder_category_${category.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.22f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }
            }

            // Quick Section-Aware Templates
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickTemplates = listOf(
                    "Task To-Do: Submit Sprint Code Review" to ReminderCategoryType.TASK_TODO,
                    "Habit: Drink 500ml Water & Stretch" to ReminderCategoryType.HABITS_REMINDER,
                    "Work: Finish Zero-Trust Enclave Audit" to ReminderCategoryType.FINISH_WORK,
                    "Meeting: Design Team Sync Call" to ReminderCategoryType.MEETING,
                    "Study: Read Gemma 2B Architecture Paper" to ReminderCategoryType.READ_STUDY
                )
                items(quickTemplates) { (tplText, cat) ->
                    SuggestionChip(
                        onClick = {
                            reminderTitle = tplText.substringAfter(": ")
                            selectedCategory = cat
                            if (cat == ReminderCategoryType.HABITS_REMINDER) {
                                isFrequentRepeat = true
                            }
                        },
                        label = {
                            Text(
                                text = tplText,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            // 2. Duration Preset Selectors (1m, 5m, 30m, 1h, 1d, 3d, 5d, 1mo)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "2. Select Countdown Timer (Rings Alarm + Popup at 00:00):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReminderPresetDuration.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset },
                            label = {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("reminder_preset_${preset.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldEnclave.copy(alpha = 0.24f),
                                selectedLabelColor = EmeraldEnclave
                            )
                        )
                    }
                }
            }

            // 3. Title Input + Start Countdown Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { reminderTitle = it },
                    placeholder = {
                        Text(
                            "Enter ${selectedCategory.label} title...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reminder_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        val finalTitle = reminderTitle.trim().ifEmpty {
                            when (selectedCategory) {
                                ReminderCategoryType.TASK_TODO -> tasks.firstOrNull()?.title ?: "Complete Priority Task To-Do"
                                ReminderCategoryType.HABITS_REMINDER -> "Hydration, Posture & Mindful Habit Check"
                                ReminderCategoryType.FINISH_WORK -> tasks.firstOrNull()?.title ?: "Finish Deep-Work Sprint Deliverable"
                                ReminderCategoryType.MEETING -> events.firstOrNull()?.title ?: "Upcoming Team Sync Meeting"
                                ReminderCategoryType.READ_STUDY -> "Read & Study Technical Documentation"
                                ReminderCategoryType.FREQUENT_REMINDER -> "Frequent Focus & Progress Check-In"
                            }
                        }
                        onAddCountdownReminder(
                            finalTitle,
                            selectedCategory,
                            selectedPreset,
                            isFrequentRepeat
                        )
                        reminderTitle = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF042F2E)
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("add_countdown_reminder_button")
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = "Set Alarm Reminder", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set (${selectedPreset.label})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Frequent / Auto-Repeating Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Frequent Repeat",
                        tint = if (isFrequentRepeat) EmeraldEnclave else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Frequent Auto-Repeat Loop (Restarts automatically after ringing)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFrequentRepeat) EmeraldEnclave else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isFrequentRepeat,
                    onCheckedChange = { isFrequentRepeat = it },
                    modifier = Modifier.testTag("frequent_reminder_switch")
                )
            }

            // 4. Active Countdowns Organized by Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredReminders.forEach { item ->
                    val accentColor = CategoryColorForReminder(item.category)

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .testTag("countdown_reminder_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = accentColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconForReminder(item.category),
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${item.category.label} • ${item.presetLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (item.category == ReminderCategoryType.HABITS_REMINDER) {
                                        Surface(
                                            color = AmberConflict.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = "Habit Streak",
                                                    tint = AmberConflict,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${item.habitStreakCount + item.firedCount}d Streak",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AmberConflict,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else if (item.isFrequentRecurring) {
                                        Surface(
                                            color = AmberConflict.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Frequent ↻",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberConflict,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Live Digital Countdown Clock
                                Surface(
                                    color = Color(0xFF090D16),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.border(
                                        1.dp,
                                        if (item.remainingSeconds <= 60L) CrimsonAlert else ElectricCyan.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Text(
                                        text = item.formattedCountdown(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (item.remainingSeconds <= 60L) CrimsonAlert else ElectricCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = item.category.spokenPrefix,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LinearProgressIndicator(
                                progress = { item.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.16f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.isPaused) "Timer Paused" else "Alarm & Popup Armed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isPaused) AmberConflict else EmeraldEnclave
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ElectricCyan.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .clickable { onTriggerReminderAlertNow(item) }
                                            .testTag("test_speak_reminder_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = "Ring Alarm Popup Now",
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Ring Popup Now",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onTogglePauseReminder(item.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = "Pause or Resume Countdown",
                                            tint = EmeraldEnclave,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteReminder(item.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Countdown Reminder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
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

    // Full Ringing Alarm Popup Modal Dialog when any reminder triggers!
    if (activeFiredAlertMessage != null) {
        RingingReminderPopupDialog(
            alertMessage = activeFiredAlertMessage,
            onStopAlarmAndComplete = onDismissFiredAlert,
            onSnoozeFiveMin = onSnoozeFiredAlertFiveMin
        )
    }
}

@Composable
private fun SectionFilterCard(
    title: String,
    subtitle: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .width(155.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = accent.copy(alpha = 0.24f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * High-priority Ringing Alarm Popup Modal that appears over the workspace when a reminder countdown finishes.
 */
@Composable
fun RingingReminderPopupDialog(
    alertMessage: String,
    onStopAlarmAndComplete: () -> Unit,
    onSnoozeFiveMin: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )

    AlertDialog(
        onDismissRequest = onStopAlarmAndComplete,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(CrimsonAlert.copy(alpha = 0.22f))
                    .border(2.dp, CrimsonAlert, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Ringing Alarm Popup",
                    tint = CrimsonAlert,
                    modifier = Modifier.size(34.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = CrimsonAlert.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "🔔 RINGING ALARM & POPUP REMINDER",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Z AI Scheduled Reminder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .testTag("active_reminder_alert_banner")
                ) {
                    Text(
                        text = alertMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Text(
                    text = "Alarm ringtone & voice announcement active. Stop alarm or snooze for 5 minutes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onStopAlarmAndComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldEnclave,
                    contentColor = Color(0xFF022C22)
                ),
                modifier = Modifier.testTag("dismiss_reminder_alert_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Alarm & Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onSnoozeFiveMin,
                modifier = Modifier.testTag("snooze_reminder_5m_button")
            ) {
                Icon(Icons.Default.Snooze, contentDescription = "Snooze 5 Min", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Snooze 5 Min")
            }
        }
    )
}
