package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OmniDao {

    // Project Tasks
    @Query("SELECT * FROM project_tasks ORDER BY CASE status WHEN 'DONE' THEN 1 ELSE 0 END ASC, computedPriorityScore DESC, deadlineHoursRemaining ASC")
    fun getAllTasks(): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks")
    suspend fun getAllTasksSnapshot(): List<ProjectTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ProjectTaskEntity>)

    @Update
    suspend fun updateTask(task: ProjectTaskEntity)

    @Query("DELETE FROM project_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Int)

    // Calendar Events
    @Query("SELECT * FROM calendar_events ORDER BY startHour ASC, startMinute ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events ORDER BY startHour ASC, startMinute ASC")
    suspend fun getAllEventsSnapshot(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>)

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: Int)

    // Workflow Macros
    @Query("SELECT * FROM workflow_macros ORDER BY id ASC")
    fun getAllMacros(): Flow<List<WorkflowMacroEntity>>

    @Query("SELECT * FROM workflow_macros ORDER BY id ASC")
    suspend fun getAllMacrosSnapshot(): List<WorkflowMacroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: WorkflowMacroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacros(macros: List<WorkflowMacroEntity>)

    @Update
    suspend fun updateMacro(macro: WorkflowMacroEntity)

    @Query("DELETE FROM workflow_macros WHERE id = :macroId")
    suspend fun deleteMacro(macroId: Int)

    // Code Snippets
    @Query("SELECT * FROM code_snippets ORDER BY updatedAt DESC")
    fun getAllSnippets(): Flow<List<CodeSnippetEntity>>

    @Query("SELECT * FROM code_snippets ORDER BY updatedAt DESC")
    suspend fun getAllSnippetsSnapshot(): List<CodeSnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CodeSnippetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippets(snippets: List<CodeSnippetEntity>)

    @Update
    suspend fun updateSnippet(snippet: CodeSnippetEntity)

    @Query("DELETE FROM code_snippets WHERE id = :snippetId")
    suspend fun deleteSnippet(snippetId: Int)

    // Hugging Face Models
    @Query("SELECT * FROM hf_models ORDER BY isActive DESC, isDownloaded DESC, likesCount DESC")
    fun getAllModels(): Flow<List<HfModelEntity>>

    @Query("SELECT * FROM hf_models")
    suspend fun getAllModelsSnapshot(): List<HfModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: HfModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<HfModelEntity>)

    @Update
    suspend fun updateModel(model: HfModelEntity)

    @Query("UPDATE hf_models SET isActive = CASE WHEN modelId = :activeId THEN 1 ELSE 0 END")
    suspend fun setActiveModel(activeId: String)
}
