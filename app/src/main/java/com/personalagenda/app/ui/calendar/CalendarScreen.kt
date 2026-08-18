package com.personalagenda.app.ui.calendar

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personalagenda.app.data.DaySpread
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.Task
import com.personalagenda.app.ui.dashboard.EventEditorDialog
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val cellDateFmt = DateTimeFormatter.ofPattern("EEE d.MM", Locale.ENGLISH)
private val rangeFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

/** Obtains the CalendarViewModel and renders the stateless screen. */
@Composable
fun CalendarRoute() {
    val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        today = LocalDate.now(),
        onPrevious = vm::previous,
        onNext = vm::next,
        onToday = vm::goToToday,
        onToggleTask = vm::toggleTask,
        onUpdateEvent = vm::updateEvent,
        onDeleteEvent = vm::deleteEvent,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggleTask: (Long) -> Unit = {},
    onUpdateEvent: (Event) -> Unit = {},
    onDeleteEvent: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val columns = if (portrait) 2 else 4

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Header(state = state, onPrevious = onPrevious, onNext = onNext, onToday = onToday)
        Spacer(Modifier.height(16.dp))

        // A grid of day cells, filling the page. Rows share the height equally.
        Column(Modifier.fillMaxSize()) {
            state.days.chunked(columns).forEach { rowDays ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowDays.forEach { day ->
                        DayCell(
                            day = day,
                            isToday = day.date == today,
                            twoColumns = !portrait,
                            onToggleTask = onToggleTask,
                            onEventClick = { editingEvent = it },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    // Pad a short final row so cells keep their width.
                    repeat(columns - rowDays.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    editingEvent?.let { event ->
        EventEditorDialog(
            event = event,
            onSave = onUpdateEvent,
            onDelete = onDeleteEvent,
            onDismiss = { editingEvent = null },
        )
    }
}

@Composable
private fun Header(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = AgendaTheme.colors
    val end = state.start.plusDays((SPREAD_DAYS - 1).toLong())
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Calendar", style = AgendaTheme.type.display, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                "${state.start.format(rangeFmt)} – ${end.format(rangeFmt)}",
                style = AgendaTheme.type.secondary,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        NavArrow("‹", onPrevious)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, colors.divider, RoundedCornerShape(50))
                .clickable(onClick = onToday)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text("This week", style = AgendaTheme.type.secondary, color = colors.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        NavArrow("›", onNext)
    }
}

@Composable
private fun NavArrow(glyph: String, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = AgendaTheme.type.heading, color = colors.textSecondary)
    }
}

@Composable
private fun DayCell(
    day: DaySpread,
    isToday: Boolean,
    twoColumns: Boolean,
    onToggleTask: (Long) -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isToday) colors.personal.copy(alpha = 0.08f) else colors.surface)
            .border(1.dp, if (isToday) colors.personal else colors.divider, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        // Date, top-left: e.g. "MON 18.08".
        Text(
            day.date.format(cellDateFmt).uppercase(),
            style = AgendaTheme.type.secondary,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) colors.personal else colors.textPrimary,
        )
        Spacer(Modifier.height(10.dp))

        if (day.tasks.isEmpty() && day.events.isEmpty()) return@Column

        // Content fills the remaining cell height and scrolls when a busy day
        // (e.g. many tasks/events) overflows, so nothing is cut off.
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (twoColumns) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(Modifier.weight(1f)) { UntimedList(day.tasks, onToggleTask) }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { TimedList(day.events, onEventClick) }
                }
            } else {
                UntimedList(day.tasks, onToggleTask)
                if (day.tasks.isNotEmpty() && day.events.isNotEmpty()) Spacer(Modifier.height(10.dp))
                TimedList(day.events, onEventClick)
            }
        }
    }
}

/** Left column: items with no time (tasks). Tapping one toggles it done. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UntimedList(tasks: List<Task>, onToggleTask: (Long) -> Unit) {
    val colors = AgendaTheme.colors
    tasks.forEach { task ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onToggleTask(task.id) }, onLongClick = {})
                .padding(vertical = 3.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (task.done) colors.textFaint else colors.textSecondary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                task.text,
                style = AgendaTheme.type.secondary,
                color = if (task.done) colors.textFaint else colors.textPrimary,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Right column: timed items (events) showing start–end (or just start). */
@Composable
private fun TimedList(events: List<Event>, onEventClick: (Event) -> Unit) {
    val colors = AgendaTheme.colors
    events.forEach { event ->
        val time = if (event.end == event.start) event.start.format(hm)
        else "${event.start.format(hm)}–${event.end.format(hm)}"
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onEventClick(event) }
                .padding(vertical = 3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(event.category.color(colors)))
                Spacer(Modifier.width(8.dp))
                Text(time, style = AgendaTheme.type.tiny, color = colors.textSecondary)
            }
            Text(
                event.title,
                style = AgendaTheme.type.secondary,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}
