package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.DayAgenda
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val hm = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun LeftColumn(
    agenda: DayAgenda,
    onSelectDay: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MonthCalendar(agenda, onSelectDay)
        ColumnDivider()
        DayPreview(agenda)
    }
}

@Composable
private fun ColumnDivider() {
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AgendaTheme.colors.divider)
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun MonthCalendar(agenda: DayAgenda, onSelectDay: (LocalDate) -> Unit) {
    val colors = AgendaTheme.colors
    val date = agenda.date
    val today = LocalDate.now()
    val monthLabel = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase() +
        " " + date.year

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(monthLabel, style = AgendaTheme.type.heading, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        MonthArrow("‹") { onSelectDay(date.minusMonths(1)) }
        Spacer(Modifier.width(8.dp))
        MonthArrow("›") { onSelectDay(date.plusMonths(1)) }
    }

    Spacer(Modifier.height(18.dp))

    val weekdays = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { d ->
            Text(
                d,
                style = AgendaTheme.type.tiny,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    // Monday-first grid.
    val firstOfMonth = date.withDayOfMonth(1)
    val lead = (firstOfMonth.dayOfWeek.value + 6) % 7 // Monday=0
    val daysInMonth = date.lengthOfMonth()
    val cells = lead + daysInMonth
    val rows = (cells + 6) / 7

    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val cellIndex = r * 7 + c
                val dayNum = cellIndex - lead + 1
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (dayNum in 1..daysInMonth) {
                        val cellDate = date.withDayOfMonth(dayNum)
                        DayCell(
                            day = dayNum,
                            isSelected = dayNum == date.dayOfMonth,
                            isToday = cellDate == today,
                            isWeekend = c >= 5,
                            onClick = { onSelectDay(cellDate) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(glyph: String, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = AgendaTheme.type.heading, color = colors.textSecondary)
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .then(
                when {
                    isSelected -> Modifier.background(colors.textPrimary)
                    isToday -> Modifier.border(1.5.dp, colors.personal, CircleShape)
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = AgendaTheme.type.secondary,
            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isSelected -> colors.background
                isToday -> colors.personal
                isWeekend -> colors.textFaint
                else -> colors.textPrimary
            },
        )
    }
}

/**
 * A compact list of the displayed day's first few events. Labelled "Today" only
 * when the displayed day really is today — otherwise it shows the weekday.
 */
@Composable
private fun DayPreview(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    val isToday = agenda.date == LocalDate.now()
    val label = if (isToday) "Today" else agenda.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    SectionLabel(label)
    Spacer(Modifier.height(14.dp))
    if (agenda.events.isEmpty()) {
        Text("Nothing scheduled.", style = AgendaTheme.type.body, color = colors.textFaint)
        return
    }
    agenda.events.take(4).forEach { e ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text(
                e.start.format(hm),
                style = AgendaTheme.type.secondary,
                color = colors.textSecondary,
                modifier = Modifier.width(56.dp),
            )
            Text(e.title, style = AgendaTheme.type.body, color = colors.textPrimary)
        }
    }
}
