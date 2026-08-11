package com.personalagenda.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personalagenda.app.data.Event
import com.personalagenda.app.ui.dashboard.EventEditorDialog
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val dayTitleFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
private val monthTitleFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** Obtains the CalendarViewModel and renders the stateless screen. */
@Composable
fun CalendarRoute() {
    val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
    val state by vm.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        today = LocalDate.now(),
        onPreviousMonth = vm::previousMonth,
        onNextMonth = vm::nextMonth,
        onToday = vm::goToToday,
        onSelectDate = vm::selectDate,
        onUpdateEvent = vm::updateEvent,
        onDeleteEvent = vm::deleteEvent,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onUpdateEvent: (Event) -> Unit = {},
    onDeleteEvent: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    Row(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        // Month grid ~58%
        MonthGrid(
            state = state,
            today = today,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onToday = onToday,
            onSelectDate = onSelectDate,
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(horizontal = 28.dp)
                .background(colors.divider),
        )
        // Selected-day detail ~42%
        DayDetail(
            date = state.selected,
            events = state.selectedEvents,
            onEventClick = { editingEvent = it },
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        )
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
private fun MonthGrid(
    state: CalendarUiState,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    Column(modifier) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                state.month.atDay(1).format(monthTitleFmt).uppercase(),
                style = AgendaTheme.type.heading,
                color = colors.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            NavArrow("‹", onPreviousMonth)
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.divider, RoundedCornerShape(50))
                    .clickable(onClick = onToday)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("Today", style = AgendaTheme.type.secondary, color = colors.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            NavArrow("›", onNextMonth)
        }

        Spacer(Modifier.height(20.dp))

        // Weekday labels
        Row(Modifier.fillMaxWidth()) {
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { d ->
                Text(
                    d,
                    style = AgendaTheme.type.tiny,
                    color = colors.textFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Grid
        val firstOfMonth = state.month.atDay(1)
        val lead = (firstOfMonth.dayOfWeek.value + 6) % 7 // Monday = 0
        val daysInMonth = state.month.lengthOfMonth()
        val rows = ((lead + daysInMonth) + 6) / 7

        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        if (dayNum in 1..daysInMonth) {
                            val date = state.month.atDay(dayNum)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == state.selected,
                                isWeekend = c >= 5,
                                count = state.counts[date.toEpochDay()] ?: 0,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavArrow(glyph: String, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = AgendaTheme.type.heading, color = colors.textSecondary)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isWeekend: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected && !isToday)
                    Modifier.border(1.dp, colors.textSecondary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(if (isToday) Modifier.background(colors.textPrimary) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = AgendaTheme.type.secondary,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isToday -> colors.background
                    isWeekend -> colors.textFaint
                    else -> colors.textPrimary
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(minOf(count, 3)) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(colors.textFaint))
            }
        }
    }
}

@Composable
private fun DayDetail(
    date: LocalDate,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    Column(modifier) {
        SectionLabel(if (events.isEmpty()) "No events" else "${events.size} events")
        Spacer(Modifier.height(8.dp))
        Text(date.format(dayTitleFmt), style = AgendaTheme.type.heading, color = colors.textPrimary)
        Spacer(Modifier.height(20.dp))

        if (events.isEmpty()) {
            Text("Nothing scheduled.", style = AgendaTheme.type.body, color = colors.textSecondary)
        } else {
            events.forEach { event ->
                DayDetailRow(event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
private fun DayDetailRow(event: Event, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        Text(
            event.start.format(hm),
            style = AgendaTheme.type.secondary,
            color = colors.textSecondary,
            modifier = Modifier.width(56.dp),
        )
        CategoryDot(event.category.color(colors))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(event.title, style = AgendaTheme.type.body, color = colors.textPrimary)
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
private fun CategoryDot(color: Color) {
    Box(Modifier.padding(top = 6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}
