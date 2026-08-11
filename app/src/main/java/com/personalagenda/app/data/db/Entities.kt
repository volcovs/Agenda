package com.personalagenda.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities. Dates are stored as epoch-day (Long) and times as minute-of-day
 * (Int) so no TypeConverters are needed — mapping to/from java.time happens in the
 * repository. Category is stored as the enum name.
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
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Nullable: a null date means the task is "Someday" (undated).
    val epochDay: Long?,
    val text: String,
    val done: Boolean = false,
)

@Entity(tableName = "focus_items")
data class FocusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val text: String,
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
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val text: String,
    val minute: Int,
    // Optional links (null = unlinked).
    val projectId: Long? = null,
    val eventId: Long? = null,
    val taskId: Long? = null,
)
