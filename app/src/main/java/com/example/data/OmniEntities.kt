package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_tasks")
data class ProjectTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val projectTag: String, // e.g., "Core Engine", "Security Enclave", "Mobile UI"
    val status: String, // "TODO", "IN_PROGRESS", "DONE"
    val priority: String, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val estimatedMinutes: Int = 45,
    val scheduledTimeSlot: String = "", // e.g., "14:00 - 14:45"
    val isSensitive: Boolean = true,
    val deadlineHoursRemaining: Int = 24, // e.g., 6, 12, 24, 48, 72
    val userImportance: Int = 4, // 1..5 stars defined by user
    val computedPriorityScore: Int = 120, // Calculated by Local Gemma 2B Prioritizer
    val createdAt: Long = System.currentTimeMillis()
) {
    fun deadlineBadgeText(): String = when {
        deadlineHoursRemaining <= 8 -> "Due in ${deadlineHoursRemaining}h (Urgent)"
        deadlineHoursRemaining <= 24 -> "Due Today (${deadlineHoursRemaining}h)"
        deadlineHoursRemaining <= 48 -> "Due Tomorrow (${deadlineHoursRemaining}h)"
        else -> "Due in ${deadlineHoursRemaining / 24}d"
    }
}

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val durationMinutes: Int,
    val attendees: String,
    val locationOrLink: String,
    val category: String, // "MEETING", "DEEP_WORK", "STANDUP", "DEADLINE"
    val linkedTaskId: Int? = null,
    val hasConflict: Boolean = false,
    val conflictReason: String = "",
    val isAutoScheduled: Boolean = false
) {
    val startTotalMinutes: Int get() = startHour * 60 + startMinute
    val endTotalMinutes: Int get() = startTotalMinutes + durationMinutes

    fun formattedTimeRange(): String {
        val endHour = (endTotalMinutes / 60) % 24
        val endMin = endTotalMinutes % 60
        return String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMin)
    }
}

@Entity(tableName = "workflow_macros")
data class WorkflowMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val triggerPhrase: String, // e.g., "run morning standup"
    val description: String,
    val stepsPipeSeparated: String,
    val targetPhoneOrEmail: String = "",
    val runCount: Int = 0,
    val lastRunSummary: String = "Ready for voice or one-tap execution"
) {
    fun stepsList(): List<String> =
        stepsPipeSeparated.split("|").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "code_snippets")
data class CodeSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String, // "Kotlin", "Python", "JavaScript", "SQL", "Bash"
    val codeContent: String,
    val lastOutput: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hf_models")
data class HfModelEntity(
    @PrimaryKey val modelId: String, // e.g. "google/gemma-2-2b-it-GGUF"
    val displayName: String,
    val author: String,
    val quantization: String, // "Q4_K_M", "Q8_0", "INT4-TFLite"
    val format: String, // "GGUF" or "TFLITE"
    val fileName: String,
    val downloadUrl: String,
    val sizeMb: Int,
    val parameterCount: String, // "2.6B", "1.5B", "1.2B"
    val isDownloaded: Boolean = false,
    val isActive: Boolean = false,
    val localFilePath: String = "",
    val downloadsCount: Int = 0,
    val likesCount: Int = 0,
    val specialty: String = "General & Task Reasoning"
)
