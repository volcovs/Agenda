package com.personalagenda.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {

    // --- Reactive reads for the UI (soft-deleted rows are hidden) ---

    @Query("SELECT * FROM events WHERE epochDay = :day AND deleted = 0 ORDER BY startMinute")
    fun events(day: Long): Flow<List<EventEntity>>

    @Query(
        "SELECT epochDay AS day, COUNT(*) AS count FROM events " +
            "WHERE epochDay BETWEEN :start AND :end AND deleted = 0 GROUP BY epochDay"
    )
    fun eventCountsBetween(start: Long, end: Long): Flow<List<DayCount>>

    @Query("SELECT * FROM events WHERE epochDay BETWEEN :start AND :end AND deleted = 0")
    fun eventsBetween(start: Long, end: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM tasks WHERE epochDay = :day AND deleted = 0 ORDER BY id")
    fun tasks(day: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY epochDay, id")
    fun allTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM projects WHERE deleted = 0 ORDER BY id")
    fun projects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM notes WHERE epochDay = :day AND deleted = 0 ORDER BY minute DESC LIMIT 1")
    fun latestNote(day: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY epochDay DESC, minute DESC")
    fun allNotes(): Flow<List<NoteEntity>>

    // --- Inserts ---

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    // Full-row updates by primary key — used when applying a remote change.
    @Update
    suspend fun updateEventRow(event: EventEntity)

    @Update
    suspend fun updateTaskRow(task: TaskEntity)

    @Update
    suspend fun updateProjectRow(project: ProjectEntity)

    @Update
    suspend fun updateNoteRow(note: NoteEntity)

    // --- Field updates from the UI ---

    @Query(
        "UPDATE projects SET name = :name, progress = :progress, category = :category, " +
            "description = :description, status = :status, nextAction = :nextAction, " +
            "deadlineEpochDay = :deadlineEpochDay, updatedAt = :now WHERE id = :id"
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
        now: Long,
    )

    @Query("UPDATE tasks SET done = NOT done, updatedAt = :now WHERE id = :id")
    suspend fun toggleTask(id: Long, now: Long)

    @Query("UPDATE tasks SET text = :text, updatedAt = :now WHERE id = :id")
    suspend fun updateTaskText(id: Long, text: String, now: Long)

    @Query("UPDATE tasks SET projectId = :projectId, projectUid = :projectUid, updatedAt = :now WHERE id = :id")
    suspend fun updateTaskProject(id: Long, projectId: Long?, projectUid: String?, now: Long)

    /** Clear the project link on all tasks pointing at a (soft-)deleted project. */
    @Query("UPDATE tasks SET projectId = NULL, projectUid = NULL, updatedAt = :now WHERE projectId = :projectId")
    suspend fun unlinkTasksFromProject(projectId: Long, now: Long)

    @Query(
        "UPDATE events SET startMinute = :startMinute, endMinute = :endMinute, " +
            "title = :title, subtitle = :subtitle, category = :category, location = :location, " +
            "updatedAt = :now WHERE id = :id"
    )
    suspend fun updateEvent(
        id: Long,
        startMinute: Int,
        endMinute: Int,
        title: String,
        subtitle: String?,
        category: String,
        location: String?,
        now: Long,
    )

    @Query(
        "UPDATE notes SET text = :text, projectId = :projectId, eventId = :eventId, taskId = :taskId, " +
            "projectUid = :projectUid, eventUid = :eventUid, taskUid = :taskUid, updatedAt = :now WHERE id = :id"
    )
    suspend fun updateNoteFull(
        id: Long,
        text: String,
        projectId: Long?,
        eventId: Long?,
        taskId: Long?,
        projectUid: String?,
        eventUid: String?,
        taskUid: String?,
        now: Long,
    )

    // --- Soft deletes (tombstones so deletions sync) ---

    @Query("UPDATE events SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteEvent(id: Long, now: Long)

    @Query("UPDATE tasks SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteTask(id: Long, now: Long)

    @Query("UPDATE projects SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteProject(id: Long, now: Long)

    @Query("UPDATE notes SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun deleteNote(id: Long, now: Long)

    // --- uid lookups (to keep link uids in step with local writes) ---

    @Query("SELECT uid FROM projects WHERE id = :id")
    suspend fun projectUidById(id: Long): String?

    @Query("SELECT uid FROM events WHERE id = :id")
    suspend fun eventUidById(id: Long): String?

    @Query("SELECT uid FROM tasks WHERE id = :id")
    suspend fun taskUidById(id: Long): String?

    // --- Sync: full one-shot reads and reactive streams (INCLUDING tombstones) ---

    @Query("SELECT * FROM events")
    fun eventsSyncFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM tasks")
    fun tasksSyncFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM projects")
    fun projectsSyncFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM notes")
    fun notesSyncFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM events WHERE uid = :uid LIMIT 1")
    suspend fun eventByUid(uid: String): EventEntity?

    @Query("SELECT * FROM tasks WHERE uid = :uid LIMIT 1")
    suspend fun taskByUid(uid: String): TaskEntity?

    @Query("SELECT * FROM projects WHERE uid = :uid LIMIT 1")
    suspend fun projectByUid(uid: String): ProjectEntity?

    @Query("SELECT * FROM notes WHERE uid = :uid LIMIT 1")
    suspend fun noteByUid(uid: String): NoteEntity?

    // --- Sync: re-resolve the local Long foreign keys from the stable link uids ---

    @Query(
        "UPDATE tasks SET projectId = " +
            "(SELECT id FROM projects WHERE projects.uid = tasks.projectUid AND projects.deleted = 0)"
    )
    suspend fun relinkTaskProjects()

    @Query(
        "UPDATE notes SET " +
            "projectId = (SELECT id FROM projects WHERE projects.uid = notes.projectUid), " +
            "eventId = (SELECT id FROM events WHERE events.uid = notes.eventUid), " +
            "taskId = (SELECT id FROM tasks WHERE tasks.uid = notes.taskUid)"
    )
    suspend fun relinkNotes()

    // --- Seeding helpers ---

    @Query("SELECT COUNT(*) FROM events")
    suspend fun eventCount(): Int

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount(): Int
}
