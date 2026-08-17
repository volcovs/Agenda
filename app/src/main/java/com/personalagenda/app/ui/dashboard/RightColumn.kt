package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.DayAgenda
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.MockData
import com.personalagenda.app.data.Project
import com.personalagenda.app.data.Task
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

private enum class EventState { COMPLETED, ACTIVE, UPCOMING }

private fun Event.stateAt(now: LocalTime): EventState = when {
    !end.isAfter(now) -> EventState.COMPLETED
    !start.isAfter(now) -> EventState.ACTIVE
    else -> EventState.UPCOMING
}

@Composable
fun RightColumn(
    agenda: DayAgenda,
    now: LocalTime,
    isToday: Boolean,
    onToggleTask: (Long) -> Unit,
    onEventClick: (Event) -> Unit,
    onTaskLongPress: (Task) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Greeting(agenda, now, isToday, onPrevDay, onNextDay, onToday)
        Spacer(Modifier.height(24.dp))
        if (agenda.events.isEmpty()) {
            EmptyDay(onAddClick)
        } else {
            // "Right now" is only meaningful for today; other days show just the schedule.
            if (isToday) {
                RightNow(agenda, now)
                Spacer(Modifier.height(32.dp))
            }
            Timeline(agenda, now, isToday, onEventClick)
        }
        Spacer(Modifier.height(32.dp))
        Tasks(agenda, onToggleTask, onTaskLongPress)
        Spacer(Modifier.height(32.dp))
        ProjectsAndNote(agenda)
        Spacer(Modifier.height(28.dp))
        BottomSummary(agenda)
    }
}

@Composable
private fun Greeting(
    agenda: DayAgenda,
    now: LocalTime,
    isToday: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = AgendaTheme.colors
    val greeting = when {
        !isToday -> "VIEWING"
        now.hour in 5..11 -> "GOOD MORNING"
        now.hour in 12..17 -> "GOOD AFTERNOON"
        else -> "GOOD EVENING"
    }
    val dateLabel = agenda.date.format(dateFmt)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = AgendaTheme.type.greeting, color = colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(dateLabel, style = AgendaTheme.type.display, color = colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayArrow("‹", onPrevDay)
                Spacer(Modifier.width(6.dp))
                DayArrow("›", onNextDay)
                if (!isToday) {
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, colors.divider, RoundedCornerShape(50))
                            .clickable(onClick = onToday)
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text("Today", style = AgendaTheme.type.secondary, color = colors.textSecondary)
                    }
                } else {
                    contextualSubtitle(agenda, now)?.let { subtitle ->
                        Spacer(Modifier.width(14.dp))
                        Text(subtitle, style = AgendaTheme.type.body, color = colors.textSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.width(20.dp))
        AnalogClock(diameter = 132.dp)
    }
}

@Composable
private fun DayArrow(glyph: String, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = AgendaTheme.type.heading, color = colors.textSecondary)
    }
}

/** A calm one-liner under the date that adapts to the time of day. */
private fun contextualSubtitle(agenda: DayAgenda, now: LocalTime): String? {
    if (agenda.events.isEmpty()) return null
    val scheduled = scheduledMinutes(agenda)
    return when (now.hour) {
        in 5..11 -> if (scheduled < 240) "You have a relatively light day." else "A full day ahead."
        in 12..17 -> {
            val left = agenda.events.count { it.end.isAfter(now) }
            if (left == 0) "You're all done for today." else "$left more on the schedule today."
        }
        else -> null // evening summary is carried by the Right Now / Day Complete block
    }
}

private fun scheduledMinutes(agenda: DayAgenda): Long =
    agenda.events.sumOf { Duration.between(it.start, it.end).toMinutes() }

/** The signature component: "Here's where you are." */
@Composable
private fun RightNow(agenda: DayAgenda, now: LocalTime) {
    val colors = AgendaTheme.colors
    val active = agenda.events.firstOrNull { it.stateAt(now) == EventState.ACTIVE }
    val next = agenda.events.firstOrNull { it.stateAt(now) == EventState.UPCOMING }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(20.dp))
            .padding(24.dp),
    ) {
        when {
            active != null -> ActiveNow(active, now)
            next != null -> UpNextBig(next)
            else -> DayComplete(agenda)
        }
    }
}

@Composable
private fun ActiveNow(event: Event, now: LocalTime) {
    val colors = AgendaTheme.colors
    val total = Duration.between(event.start, event.end).toMinutes().coerceAtLeast(1)
    val elapsed = Duration.between(event.start, now).toMinutes().coerceIn(0, total)
    val remaining = total - elapsed
    val accent = event.category.color(colors)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(8.dp))
            SectionLabel("Right now")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "${event.start.format(hm)} — ${event.end.format(hm)}",
            style = AgendaTheme.type.secondary,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(event.title, style = AgendaTheme.type.display.copy(), color = colors.textPrimary)
        if (event.subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(event.subtitle, style = AgendaTheme.type.body, color = colors.textSecondary)
        }
        Spacer(Modifier.height(18.dp))
        ThinProgressBar(
            progress = elapsed / total.toFloat(),
            track = colors.divider,
            fill = accent,
            height = 8,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Text("$elapsed min elapsed", style = AgendaTheme.type.secondary, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text("$remaining min remaining", style = AgendaTheme.type.secondary, color = colors.textPrimary)
        }
    }
}

@Composable
private fun UpNextBig(event: Event) {
    val colors = AgendaTheme.colors
    Column {
        SectionLabel("Up next")
        Spacer(Modifier.height(16.dp))
        Text(event.start.format(hm), style = AgendaTheme.type.secondary, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(event.title, style = AgendaTheme.type.display, color = colors.textPrimary)
        if (event.subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(event.subtitle, style = AgendaTheme.type.body, color = colors.textSecondary)
        }
    }
}

@Composable
private fun DayComplete(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    Column {
        SectionLabel("Day complete")
        Spacer(Modifier.height(16.dp))
        Text(
            "${agenda.events.size} events",
            style = AgendaTheme.type.heading,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${agenda.tasks.count { it.done }} of ${agenda.tasks.size} tasks completed",
            style = AgendaTheme.type.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Spacer(Modifier.height(16.dp))
        SectionLabel("Tomorrow")
        Spacer(Modifier.height(8.dp))
        Text(
            "${MockData.tomorrowCommitments} commitments",
            style = AgendaTheme.type.body,
            color = colors.textPrimary,
        )
    }
}

/** The calm "your day is clear" state, shown when no events are scheduled. */
@Composable
private fun EmptyDay(onAddClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp)) {
        Text("Your day is clear", style = AgendaTheme.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Text("Nothing scheduled yet.", style = AgendaTheme.type.body, color = colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        Text(
            "+ Add something",
            style = AgendaTheme.type.bodyStrong,
            color = colors.personal,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onAddClick)
                .padding(vertical = 6.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun BottomSummary(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    val scheduled = scheduledMinutes(agenda)
    val hours = scheduled / 60
    val mins = scheduled % 60
    val scheduledLabel = when {
        scheduled == 0L -> "0m"
        hours == 0L -> "${mins}m"
        mins == 0L -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }

    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            SummaryMetric(agenda.events.size.toString(), "events")
            SummaryMetric(agenda.tasks.count { !it.done }.toString(), "tasks")
            SummaryMetric(scheduledLabel, "scheduled")
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    val colors = AgendaTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = AgendaTheme.type.bodyStrong, color = colors.textPrimary)
        Spacer(Modifier.width(6.dp))
        Text(label, style = AgendaTheme.type.secondary, color = colors.textFaint)
    }
}

/** Journal-like timeline with a thin vertical spine and a NOW marker. */
@Composable
private fun Timeline(agenda: DayAgenda, now: LocalTime, isToday: Boolean, onEventClick: (Event) -> Unit) {
    SectionLabel(if (isToday) "Today" else "Schedule")
    Spacer(Modifier.height(16.dp))

    // For other days there is no "now": a past day is entirely done, a future day
    // entirely ahead. Only today uses the live clock (and shows the NOW marker).
    val stateNow = when {
        isToday -> now
        agenda.date.isBefore(LocalDate.now()) -> LocalTime.MAX
        else -> LocalTime.MIN
    }

    var nowShown = false
    Column {
        agenda.events.forEach { event ->
            if (isToday && !nowShown && event.start.isAfter(now)) {
                NowLine()
                nowShown = true
            }
            TimelineRow(event, event.stateAt(stateNow), onClick = { onEventClick(event) })
        }
        if (isToday && !nowShown) NowLine()
    }
}

@Composable
private fun TimelineRow(event: Event, state: EventState, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    val accent = event.category.color(colors)
    val faded = state == EventState.COMPLETED

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(if (event.subtitle != null) 64.dp else 48.dp)
    ) {
        Text(
            event.start.format(hm),
            style = AgendaTheme.type.secondary,
            color = if (faded) colors.textFaint else colors.textSecondary,
            modifier = Modifier.width(56.dp).padding(top = 1.dp),
        )
        // spine + marker
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp),
        ) {
            Box(
                Modifier
                    .size(if (state == EventState.ACTIVE) 12.dp else 9.dp)
                    .clip(CircleShape)
                    .then(
                        if (state == EventState.UPCOMING)
                            Modifier.border(2.dp, accent, CircleShape).background(colors.surface)
                        else Modifier.background(if (faded) colors.textFaint else accent)
                    ),
            )
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(colors.divider),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                event.title,
                style = if (state == EventState.ACTIVE) AgendaTheme.type.bodyStrong else AgendaTheme.type.body,
                color = if (faded) colors.textSecondary else colors.textPrimary,
                textDecoration = if (faded) TextDecoration.LineThrough else null,
            )
            if (event.subtitle != null) {
                Text(event.subtitle, style = AgendaTheme.type.secondary, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "${event.start.format(hm)}–${event.end.format(hm)}",
            style = AgendaTheme.type.tiny,
            color = colors.textFaint,
        )
    }
}

@Composable
private fun NowLine() {
    val colors = AgendaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Spacer(Modifier.width(56.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(colors.deadline))
        Spacer(Modifier.width(10.dp))
        Text("NOW", style = AgendaTheme.type.tiny, color = colors.deadline)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.deadline.copy(alpha = 0.4f)))
    }
}

@Composable
private fun Tasks(
    agenda: DayAgenda,
    onToggleTask: (Long) -> Unit,
    onTaskLongPress: (Task) -> Unit,
) {
    if (agenda.tasks.isEmpty()) return
    SectionLabel("Tasks")
    Spacer(Modifier.height(16.dp))
    agenda.tasks.forEach { task ->
        TaskRow(
            task,
            onClick = { onToggleTask(task.id) },
            onLongClick = { onTaskLongPress(task) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(task: Task, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 7.dp),
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (task.done) Modifier.background(colors.textSecondary)
                    else Modifier.border(1.5.dp, colors.textFaint, RoundedCornerShape(4.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) {
                Text("✓", style = AgendaTheme.type.tiny, color = colors.background)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            task.text,
            style = AgendaTheme.type.body,
            color = if (task.done) colors.textFaint else colors.textPrimary,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun ProjectsAndNote(agenda: DayAgenda) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1.3f)) { Projects(agenda) }
        Column(Modifier.weight(1f)) { QuickNote(agenda) }
    }
}

@Composable
private fun Projects(agenda: DayAgenda) {
    if (agenda.projects.isEmpty()) return
    val colors = AgendaTheme.colors
    SectionLabel("Projects")
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        agenda.projects.forEach { p ->
            Column(Modifier.weight(1f)) { ProjectItem(p) }
        }
    }
}

@Composable
private fun ProjectItem(project: Project) {
    val colors = AgendaTheme.colors
    Text(project.name, style = AgendaTheme.type.secondary, color = colors.textPrimary, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    ThinProgressBar(
        progress = project.effectiveProgress,
        track = colors.divider,
        fill = project.category.color(colors),
        height = 6,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        if (project.hasTasks) "${project.linkedTasksDone}/${project.linkedTasksTotal} tasks" else "No tasks yet",
        style = AgendaTheme.type.tiny,
        color = colors.textFaint,
    )
}

@Composable
private fun QuickNote(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    val note = agenda.quickNote ?: return
    SectionLabel("Quick note")
    Spacer(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            "“${note.text}”",
            style = AgendaTheme.type.body,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            note.time.format(hm),
            style = AgendaTheme.type.tiny,
            color = colors.textFaint,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
