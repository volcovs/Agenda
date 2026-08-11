package com.personalagenda.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {

    // --- Reactive reads for a given day ---

    @Query("SELECT * FROM events WHERE epochDay = :day ORDER BY startMinute")
    fun events(day: Long): Flow<List<EventEntity>>

    @Query(
        "SELECT epochDay AS day, COUNT(*) AS count FROM events " +
            "WHERE epochDay BETWEEN :start AND :end GROUP BY epochDay"
    )
    fun eventCountsBetween(start: Long, end: Long): Flow<List<DayCount>>

    @Query("SELECT * FROM events WHERE epochDay BETWEEN :start AND :end")
    fun eventsBetween(start: Long, end: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM tasks WHERE epochDay = :day ORDER BY id")
    fun tasks(day: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY epochDay, id")
    fun allTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM focus_items WHERE epochDay = :day ORDER BY id")
    fun focus(day: Long): Flow<List<FocusEntity>>

    @Query("SELECT * FROM projects ORDER BY id")
    fun projects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM notes WHERE epochDay = :day ORDER BY minute DESC LIMIT 1")
    fun latestNote(day: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY epochDay DESC, minute DESC")
    fun allNotes(): Flow<List<NoteEntity>>

    // --- Writes ---

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Insert
    suspend fun insertFocus(focus: FocusEntity)

    @Insert
    suspend fun insertProject(project: ProjectEntity)

    @Query(
        "UPDATE projects SET name = :name, progress = :progress, category = :category, " +
            "description = :description, status = :status, nextAction = :nextAction, " +
            "deadlineEpochDay = :deadlineEpochDay WHERE id = :id"
    )
    suspend fun updateProject(
        id: Long,
        name: String,
        progress: Float,
        category: String,
        description: String?,
        status: String?,
        nextAction: String?,
        deadlineEpochDay: Long?,
    )

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Query("UPDATE tasks SET done = NOT done WHERE id = :id")
    suspend fun toggleTask(id: Long)

    @Query("UPDATE tasks SET text = :text WHERE id = :id")
    suspend fun updateTaskText(id: Long, text: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query(
        "UPDATE events SET startMinute = :startMinute, endMinute = :endMinute, " +
            "title = :title, subtitle = :subtitle, category = :category, location = :location " +
            "WHERE id = :id"
    )
    suspend fun updateEvent(
        id: Long,
        startMinute: Int,
        endMinute: Int,
        title: String,
        subtitle: String?,
        category: String,
        location: String?,
    )

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query(
        "UPDATE notes SET text = :text, projectId = :projectId, " +
            "eventId = :eventId, taskId = :taskId WHERE id = :id"
    )
    suspend fun updateNoteFull(id: Long, text: String, projectId: Long?, eventId: Long?, taskId: Long?)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // --- Seeding helpers ---

    @Query("SELECT COUNT(*) FROM events")
    suspend fun eventCount(): Int

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount(): Int
}
