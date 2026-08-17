package com.personalagenda.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities. Dates are stored as epoch-day (Long) and times as minute-of-day
 * (Int) so no TypeConverters are needed — mapping to/from java.time happens in the
 * repository. Category is stored as the enum name.
 *
 * Sync groundwork: every synced table carries a stable [uid] (a globally-unique
 * string, independent of the local autoincrement [id]), an [updatedAt] millis
 * timestamp (last-write-wins clock) and a [deleted] soft-delete tombstone so
 * deletions propagate across devices. Foreign keys are additionally stored as the
 * target's [uid] (e.g. [TaskEntity.projectUid]) so links survive the local ids
 * differing between devices; the Long id columns remain the local working refs.
 * `focus_items` is legacy/unused and is NOT synced.
 */

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startMinute: Int,
    val endMinute: Int,
    val title: String,
    val subtitle: String?,
    val category: String,
    val location: String?,
    val uid: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Nullable: a null date means the task is "Someday" (undated).
    val epochDay: Long?,
    val text: String,
    val done: Boolean = false,
    // Optional link to a project (null = unlinked). [projectId] is the local ref;
    // [projectUid] is the stable ref used for sync.
    val projectId: Long? = null,
    val uid: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
    val projectUid: String? = null,
)

@Entity(tableName = "focus_items")
data class FocusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val text: String,
    val uid: String? = null,
    val updatedAt: Long? = null,
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val progress: Float,
    val category: String,
    val description: String? = null,
    val status: String? = null,
    val nextAction: String? = null,
    val deadlineEpochDay: Long? = null,
    val uid: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val text: String,
    val minute: Int,
    // Optional links (null = unlinked). Long ids are local refs; *Uid are sync refs.
    val projectId: Long? = null,
    val eventId: Long? = null,
    val taskId: Long? = null,
    val uid: String? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
    val projectUid: String? = null,
    val eventUid: String? = null,
    val taskUid: String? = null,
)
