package com.personalagenda.app.data

import com.personalagenda.app.data.db.AgendaDao
import com.personalagenda.app.data.db.EventEntity
import com.personalagenda.app.data.db.FocusEntity
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
 * ViewModel observes, and suspend functions for mutations. The `weekWorkload`
 * mini-heatmap is still a lightweight derived constant for now.
 */
class AgendaRepository(private val dao: AgendaDao) {

    fun observeDay(date: LocalDate): Flow<DayAgenda> {
        val day = date.toEpochDay()
        return combine(
            dao.events(day),
            dao.tasks(day),
            dao.focus(day),
            dao.projects(),
            dao.latestNote(day),
        ) { events, tasks, focus, projects, notes ->
            DayAgenda(
                date = date,
                events = events.map { it.toDomain() },
                focus = focus.map { FocusItem(it.text) },
                tasks = tasks.map { Task(it.text, it.done, it.id) },
                projects = projects.map { it.toDomain() },
                quickNote = notes.firstOrNull()?.let { QuickNote(it.text, minuteToTime(it.minute)) },
                weekWorkload = MockData.day.weekWorkload,
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

    /** Event counts per day within a date range, for the calendar's month grid. */
    fun observeMonthCounts(start: LocalDate, end: LocalDate): Flow<Map<Long, Int>> =
        dao.eventCountsBetween(start.toEpochDay(), end.toEpochDay()).map { list ->
            list.associate { it.day to it.count }
        }

    /** All projects, for the Projects screen. */
    fun observeProjects(): Flow<List<Project>> =
        dao.projects().map { list -> list.map { it.toDomain() } }

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
                DatedTask(it.id, it.text, it.done, it.epochDay?.let(LocalDate::ofEpochDay))
            }
        }

    // --- Mutations ---

    suspend fun toggleTask(id: Long) = dao.toggleTask(id)

    suspend fun addTask(date: LocalDate?, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertTask(TaskEntity(epochDay = date?.toEpochDay(), text = trimmed))
    }

    suspend fun addFocus(date: LocalDate, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertFocus(FocusEntity(epochDay = date.toEpochDay(), text = trimmed))
    }

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
        )
    }

    suspend fun deleteEvent(id: Long) = dao.deleteEvent(id)

    suspend fun renameTask(id: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.updateTaskText(id, trimmed)
    }

    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    suspend fun updateNote(id: Long, text: String, links: NoteLinks) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.updateNoteFull(id, trimmed, links.projectId, links.eventId, links.taskId)
    }

    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

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
        )
    }

    suspend fun deleteProject(id: Long) = dao.deleteProject(id)

    /** Populate the database from [MockData] the first time the app runs. */
    suspend fun seedIfEmpty(date: LocalDate) {
        if (dao.eventCount() == 0) {
            val day = date.toEpochDay()
            MockData.day.events.forEach { addEventEntity(day, it) }
            MockData.day.tasks.forEach { dao.insertTask(TaskEntity(epochDay = day, text = it.text, done = it.done)) }
            MockData.day.focus.forEach { dao.insertFocus(FocusEntity(epochDay = day, text = it.text)) }
            MockData.day.quickNote?.let {
                dao.insertNote(NoteEntity(epochDay = day, text = it.text, minute = it.time.toMinuteOfDay()))
            }
        }
        if (dao.projectCount() == 0) {
            MockData.day.projects.forEach {
                dao.insertProject(ProjectEntity(name = it.name, progress = it.progress, category = it.category.name))
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
        )
    )
}

// --- Mapping helpers ---

private fun EventEntity.toDomain() = Event(
    start = minuteToTime(startMinute),
    end = minuteToTime(endMinute),
    title = title,
    subtitle = subtitle,
    category = Category.valueOf(category),
    location = location,
    id = id,
)

private fun ProjectEntity.toDomain() = Project(
    name = name,
    progress = progress,
    category = Category.valueOf(category),
    description = description,
    status = status,
    nextAction = nextAction,
    deadline = deadlineEpochDay?.let(LocalDate::ofEpochDay),
    id = id,
)

private fun Project.toEntity() = ProjectEntity(
    name = name.trim(),
    progress = progress,
    category = category.name,
    description = description?.trim()?.ifEmpty { null },
    status = status?.trim()?.ifEmpty { null },
    nextAction = nextAction?.trim()?.ifEmpty { null },
    deadlineEpochDay = deadline?.toEpochDay(),
)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun minuteToTime(minute: Int): LocalTime = LocalTime.of(minute / 60, minute % 60)
