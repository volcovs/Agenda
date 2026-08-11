package com.personalagenda.app.data

import androidx.compose.ui.graphics.Color
import com.personalagenda.app.ui.theme.AgendaColors
import java.time.LocalDate
import java.time.LocalTime

/** The seven restrained category accents from the brief. */
enum class Category {
    PERSONAL, WORK, LEARNING, HEALTH, SOCIAL, PROJECTS, DEADLINE;

    fun color(c: AgendaColors): Color = when (this) {
        PERSONAL -> c.personal
        WORK -> c.work
        LEARNING -> c.learning
        HEALTH -> c.health
        SOCIAL -> c.social
        PROJECTS -> c.projects
        DEADLINE -> c.deadline
    }
}

data class Event(
    val start: LocalTime,
    val end: LocalTime,
    val title: String,
    val subtitle: String? = null,
    val category: Category,
    val location: String? = null,
    val id: Long = 0,
)

data class FocusItem(val text: String)

data class Task(val text: String, val done: Boolean = false, val id: Long = 0)

/** A task carrying its date — used by the Tasks screen to group across days.
 *  A null [date] means the task is "Someday" (undated). */
data class DatedTask(val id: Long, val text: String, val done: Boolean, val date: LocalDate?)

data class Project(
    val name: String,
    val progress: Float, // 0f..1f
    val category: Category,
    val description: String? = null,
    val status: String? = null,
    val nextAction: String? = null,
    val deadline: LocalDate? = null,
    val id: Long = 0,
)

data class QuickNote(val text: String, val time: LocalTime)

/** Optional links from a note to other objects. */
data class NoteLinks(
    val projectId: Long? = null,
    val eventId: Long? = null,
    val taskId: Long? = null,
)

/** A note with its full timestamp and optional links — used by the Notes screen. */
data class NoteItem(
    val id: Long,
    val text: String,
    val date: LocalDate,
    val time: LocalTime,
    val links: NoteLinks = NoteLinks(),
)

/**
 * Reflective weekly statistics for the Insights screen. Calm summary numbers,
 * deliberately no streaks/scores/gamification.
 */
data class InsightsData(
    val weekStart: LocalDate,
    val scheduledMinutes: Int = 0,
    val completedTasks: Int = 0,
    val events: Int = 0,
    val activeDays: Int = 0,
    val perDayMinutes: List<Int> = List(7) { 0 },      // Mon..Sun
    val byCategoryMinutes: List<Pair<Category, Int>> = emptyList(), // desc, only > 0
)

/** A single day's worth of agenda content. */
data class DayAgenda(
    val date: LocalDate,
    val events: List<Event>,
    val focus: List<FocusItem>,
    val tasks: List<Task>,
    val projects: List<Project>,
    val quickNote: QuickNote?,
    val weekWorkload: List<Pair<String, Float>>, // day label -> 0f..1f busyness
)

/**
 * Sample content used to seed a fresh install and to drive previews. Neutral,
 * everyday examples — swap for your own once the app is running.
 */
object MockData {
    val day = DayAgenda(
        date = LocalDate.of(2026, 8, 10),
        events = listOf(
            Event(
                start = LocalTime.of(8, 0), end = LocalTime.of(8, 45),
                title = "Morning run", subtitle = "Riverside loop",
                category = Category.HEALTH,
            ),
            Event(
                start = LocalTime.of(9, 30), end = LocalTime.of(11, 0),
                title = "Deep work", subtitle = "Quarterly report",
                category = Category.WORK,
            ),
            Event(
                start = LocalTime.of(12, 30), end = LocalTime.of(13, 15),
                title = "Lunch", category = Category.PERSONAL,
            ),
            Event(
                start = LocalTime.of(14, 0), end = LocalTime.of(15, 30),
                title = "Design review", subtitle = "New dashboard",
                category = Category.WORK,
            ),
            Event(
                start = LocalTime.of(16, 30), end = LocalTime.of(17, 0),
                title = "Coffee with Sam", category = Category.SOCIAL,
            ),
            Event(
                start = LocalTime.of(18, 30), end = LocalTime.of(19, 30),
                title = "Dinner", category = Category.PERSONAL,
            ),
            Event(
                start = LocalTime.of(20, 0), end = LocalTime.of(21, 0),
                title = "Evening reading", subtitle = "30 pages",
                category = Category.LEARNING,
            ),
        ),
        focus = listOf(
            FocusItem("Finish the quarterly report"),
            FocusItem("Review design feedback"),
            FocusItem("Plan the week ahead"),
        ),
        tasks = listOf(
            Task("Reply to emails"),
            Task("Book dentist appointment"),
            Task("Buy groceries"),
            Task("Water the plants", done = true),
        ),
        projects = listOf(
            Project("Website Redesign", 0.8f, Category.WORK),
            Project("Home Garden", 0.5f, Category.HEALTH),
            Project("Photography", 0.3f, Category.PERSONAL),
        ),
        quickNote = QuickNote(
            "Send the invoice before Friday.",
            LocalTime.of(14, 42),
        ),
        weekWorkload = listOf(
            "Mon" to 0.9f,
            "Tue" to 0.5f,
            "Wed" to 0.8f,
            "Thu" to 0.3f,
            "Fri" to 0.6f,
        ),
    )

    /**
     * A fixed "current time" so the RIGHT NOW component always demonstrates the
     * active state (mid-way through an event) — used only by @Preview.
     * The running app uses LocalTime.now().
     */
    val demoNow: LocalTime = LocalTime.of(14, 37)

    /** Number of commitments tomorrow — shown in the evening "day complete" state. */
    const val tomorrowCommitments: Int = 2

    /** A cleared day, for the "your day is clear" empty state. */
    val emptyDay = DayAgenda(
        date = LocalDate.of(2026, 8, 15),
        events = emptyList(),
        focus = emptyList(),
        tasks = emptyList(),
        projects = day.projects,
        quickNote = null,
        weekWorkload = day.weekWorkload,
    )
}
