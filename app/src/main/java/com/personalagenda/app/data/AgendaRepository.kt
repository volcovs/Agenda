package com.personalagenda.app.data

import com.personalagenda.app.data.db.AgendaDao
import com.personalagenda.app.data.db.EventEntity
import com.personalagenda.app.data.db.NoteEntity
import com.personalagenda.app.data.db.ProjectEntity
import com.personalagenda.app.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room-backed source of truth for the agenda. Exposes reactive [Flow]s the
 * ViewModel observes, and suspend functions for mutations.
 */
class AgendaRepository(private val dao: AgendaDao) {

    // Sync groundwork: a fresh globally-unique id, and "now" in millis, stamped
    // on every write so sync has a stable identity and a last-write-wins clock.
    private fun newUid(): String = java.util.UUID.randomUUID().toString()
    private fun now(): Long = System.currentTimeMillis()

    fun observeDay(date: LocalDate): Flow<DayAgenda> {
        val day = date.toEpochDay()
        return combine(
            dao.events(day),
            dao.tasks(day),
            dao.projects(),
            dao.allTasks(),
            dao.latestNote(day),
        ) { events, tasks, projects, allTasks, notes ->
            val counts = projectTaskCounts(allTasks)
            DayAgenda(
                date = date,
                events = events.map { it.toDomain() },
                tasks = tasks.map { Task(it.text, it.done, it.id, it.projectId) },
                projects = projects.map { it.toDomain(counts) },
                quickNote = notes.firstOrNull()?.let { QuickNote(it.text, minuteToTime(it.minute)) },
            )
        }
    }

    /** Reflective weekly statistics (Mon..Sun starting at [weekStart]) for Insights. */
    fun observeWeekInsights(weekStart: LocalDate): Flow<InsightsData> {
        val start = weekStart.toEpochDay()
        val end = weekStart.plusDays(6).toEpochDay()
        return combine(
            dao.eventsBetween(start, end),
            dao.allTasks(),
        ) { events, tasks ->
            val perDay = IntArray(7)
            val byCategory = linkedMapOf<Category, Int>()
            var scheduled = 0
            for (e in events) {
                val minutes = (e.endMinute - e.startMinute).coerceAtLeast(0)
                scheduled += minutes
                val dayIndex = (e.epochDay - start).toInt()
                if (dayIndex in 0..6) perDay[dayIndex] += minutes
                val cat = Category.valueOf(e.category)
                byCategory[cat] = (byCategory[cat] ?: 0) + minutes
            }
            val completed = tasks.count { it.done && it.epochDay != null && it.epochDay in start..end }
            InsightsData(
                weekStart = weekStart,
                scheduledMinutes = scheduled,
                completedTasks = completed,
                events = events.size,
                activeDays = perDay.count { it > 0 },
                perDayMinutes = perDay.toList(),
                byCategoryMinutes = byCategory.entries
                    .filter { it.value > 0 }
                    .sortedByDescending { it.value }
                    .map { it.key to it.value },
            )
        }
    }

    /**
     * A run of [days] consecutive days from [start], each with its untimed tasks
     * and timed events — for the weekly-spread Calendar view.
     */
    fun observeSpread(start: LocalDate, days: Int): Flow<List<DaySpread>> {
        val startEpoch = start.toEpochDay()
        val endEpoch = start.plusDays((days - 1).toLong()).toEpochDay()
        return combine(
            dao.eventsBetween(startEpoch, endEpoch),
            dao.allTasks(),
        ) { events, tasks ->
            val eventsByDay = events.groupBy { it.epochDay }
            val tasksByDay = tasks.filter { it.epochDay != null }.groupBy { it.epochDay }
            (0 until days).map { offset ->
                val date = start.plusDays(offset.toLong())
                val ep = date.toEpochDay()
                DaySpread(
                    date = date,
                    events = eventsByDay[ep].orEmpty().sortedBy { it.startMinute }.map { it.toDomain() },
                    tasks = tasksByDay[ep].orEmpty().map { Task(it.text, it.done, it.id, it.projectId) },
                )
            }
        }
    }

    /** Event counts per day within a date range, for the calendar's month grid. */
    fun observeMonthCounts(start: LocalDate, end: LocalDate): Flow<Map<Long, Int>> =
        dao.eventCountsBetween(start.toEpochDay(), end.toEpochDay()).map { list ->
            list.associate { it.day to it.count }
        }

    /** All projects, for the Projects screen — with live task-completion counts. */
    fun observeProjects(): Flow<List<Project>> =
        combine(dao.projects(), dao.allTasks()) { projects, tasks ->
            val counts = projectTaskCounts(tasks)
            projects.map { it.toDomain(counts) }
        }

    /** All notes across every date, for the Notes screen. */
    fun observeAllNotes(): Flow<List<NoteItem>> =
        dao.allNotes().map { list ->
            list.map {
                NoteItem(
                    id = it.id,
                    text = it.text,
                    date = LocalDate.ofEpochDay(it.epochDay),
                    time = minuteToTime(it.minute),
                    links = NoteLinks(it.projectId, it.eventId, it.taskId),
                )
            }
        }

    /** All tasks across every date, for the Tasks screen. */
    fun observeAllTasks(): Flow<List<DatedTask>> =
        dao.allTasks().map { list ->
            list.map {
                DatedTask(it.id, it.text, it.done, it.epochDay?.let(LocalDate::ofEpochDay), it.projectId)
            }
        }

    // --- Mutations ---

    suspend fun toggleTask(id: Long) = dao.toggleTask(id, now())

    suspend fun addTask(date: LocalDate?, text: String, projectId: Long? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertTask(
            TaskEntity(
                epochDay = date?.toEpochDay(), text = trimmed, projectId = projectId,
                projectUid = projectId?.let { dao.projectUidById(it) },
                uid = newUid(), updatedAt = now(),
            )
        )
    }

    suspend fun setTaskProject(id: Long, projectId: Long?) =
        dao.updateTaskProject(id, projectId, projectId?.let { dao.projectUidById(it) }, now())

    suspend fun setNote(date: LocalDate, text: String, now: LocalTime, links: NoteLinks = NoteLinks()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertNote(
            NoteEntity(
                epochDay = date.toEpochDay(),
                text = trimmed,
                minute = now.toMinuteOfDay(),
                projectId = links.projectId,
                eventId = links.eventId,
                taskId = links.taskId,
                projectUid = links.projectId?.let { dao.projectUidById(it) },
                eventUid = links.eventId?.let { dao.eventUidById(it) },
                taskUid = links.taskId?.let { dao.taskUidById(it) },
                uid = newUid(),
                updatedAt = now(),
            )
        )
    }

    suspend fun addEvent(date: LocalDate, event: Event) {
        dao.insertEvent(
            EventEntity(
                epochDay = date.toEpochDay(),
                startMinute = event.start.toMinuteOfDay(),
                endMinute = event.end.toMinuteOfDay(),
                title = event.title,
                subtitle = event.subtitle,
                category = event.category.name,
                location = event.location,
                uid = newUid(),
                updatedAt = now(),
            )
        )
    }

    suspend fun updateEvent(event: Event) {
        dao.updateEvent(
            id = event.id,
            startMinute = event.start.toMinuteOfDay(),
            endMinute = event.end.toMinuteOfDay(),
            title = event.title,
            subtitle = event.subtitle,
            category = event.category.name,
            location = event.location,
            now = now(),
        )
    }

    suspend fun deleteEvent(id: Long) = dao.deleteEvent(id, now())

    suspend fun renameTask(id: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.updateTaskText(id, trimmed, now())
    }

    suspend fun deleteTask(id: Long) = dao.deleteTask(id, now())

    suspend fun updateNote(id: Long, text: String, links: NoteLinks) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.updateNoteFull(
            id, trimmed, links.projectId, links.eventId, links.taskId,
            links.projectId?.let { dao.projectUidById(it) },
            links.eventId?.let { dao.eventUidById(it) },
            links.taskId?.let { dao.taskUidById(it) },
            now(),
        )
    }

    suspend fun deleteNote(id: Long) = dao.deleteNote(id, now())

    suspend fun addProject(project: Project) {
        if (project.name.isBlank()) return
        dao.insertProject(project.toEntity())
    }

    suspend fun updateProject(project: Project) {
        dao.updateProject(
            id = project.id,
            name = project.name.trim(),
            progress = project.progress,
            category = project.category.name,
            description = project.description?.trim()?.ifEmpty { null },
            status = project.status?.trim()?.ifEmpty { null },
            nextAction = project.nextAction?.trim()?.ifEmpty { null },
            deadlineEpochDay = project.deadline?.toEpochDay(),
            now = now(),
        )
    }

    suspend fun deleteProject(id: Long) {
        dao.unlinkTasksFromProject(id, now())
        dao.deleteProject(id, now())
    }

    /** Populate the database from [MockData] the first time the app runs. */
    suspend fun seedIfEmpty(date: LocalDate) {
        // Seed projects first so tasks can link to them (JIRA-style progress demo).
        var firstProjectId: Long? = null
        var firstProjectUid: String? = null
        if (dao.projectCount() == 0) {
            MockData.day.projects.forEachIndexed { index, p ->
                val projUid = newUid()
                val id = dao.insertProject(
                    ProjectEntity(
                        name = p.name, progress = p.progress, category = p.category.name,
                        uid = projUid, updatedAt = now(),
                    )
                )
                if (index == 0) {
                    firstProjectId = id
                    firstProjectUid = projUid
                }
            }
        }
        if (dao.eventCount() == 0) {
            val day = date.toEpochDay()
            MockData.day.events.forEach { addEventEntity(day, it) }
            // Link the seed tasks to the first project to demonstrate task-driven progress.
            MockData.day.tasks.forEach {
                dao.insertTask(
                    TaskEntity(
                        epochDay = day, text = it.text, done = it.done,
                        projectId = firstProjectId, projectUid = firstProjectUid,
                        uid = newUid(), updatedAt = now(),
                    )
                )
            }
            MockData.day.quickNote?.let {
                dao.insertNote(
                    NoteEntity(
                        epochDay = day, text = it.text, minute = it.time.toMinuteOfDay(),
                        uid = newUid(), updatedAt = now(),
                    )
                )
            }
        }
    }

    private suspend fun addEventEntity(day: Long, e: Event) = dao.insertEvent(
        EventEntity(
            epochDay = day,
            startMinute = e.start.toMinuteOfDay(),
            endMinute = e.end.toMinuteOfDay(),
            title = e.title,
            subtitle = e.subtitle,
            category = e.category.name,
            location = e.location,
            uid = newUid(),
            updatedAt = now(),
        )
    )
}

// --- Mapping helpers ---

/** (total, done) linked-task counts per project id, from the full task list. */
private fun projectTaskCounts(tasks: List<TaskEntity>): Map<Long, Pair<Int, Int>> =
    tasks.asSequence()
        .filter { it.projectId != null }
        .groupBy { it.projectId!! }
        .mapValues { (_, list) -> list.size to list.count { it.done } }

private fun EventEntity.toDomain() = Event(
    start = minuteToTime(startMinute),
    end = minuteToTime(endMinute),
    title = title,
    subtitle = subtitle,
    category = Category.valueOf(category),
    location = location,
    id = id,
)

private fun ProjectEntity.toDomain(counts: Map<Long, Pair<Int, Int>> = emptyMap()): Project {
    val (total, done) = counts[id] ?: (0 to 0)
    return Project(
        name = name,
        progress = progress,
        category = Category.valueOf(category),
        description = description,
        status = status,
        nextAction = nextAction,
        deadline = deadlineEpochDay?.let(LocalDate::ofEpochDay),
        id = id,
        linkedTasksTotal = total,
        linkedTasksDone = done,
    )
}

private fun Project.toEntity() = ProjectEntity(
    name = name.trim(),
    progress = progress,
    category = category.name,
    description = description?.trim()?.ifEmpty { null },
    status = status?.trim()?.ifEmpty { null },
    nextAction = nextAction?.trim()?.ifEmpty { null },
    deadlineEpochDay = deadline?.toEpochDay(),
    uid = java.util.UUID.randomUUID().toString(),
    updatedAt = System.currentTimeMillis(),
)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun minuteToTime(minute: Int): LocalTime = LocalTime.of(minute / 60, minute % 60)
