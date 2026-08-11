package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val hm = DateTimeFormatter.ofPattern("HH:mm")

// Days that carry activity in the mock month, for tiny calendar indicators.
private val busyDays = setOf(4, 6, 10, 12, 14, 20, 25)
private val deadlineDays = setOf(18, 28)

@Composable
fun LeftColumn(agenda: DayAgenda, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        MonthCalendar(agenda)
        ColumnDivider()
        TodayPreview(agenda)
        ColumnDivider()
        ThisWeek(agenda)
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
private fun MonthCalendar(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    val date = agenda.date
    val monthLabel = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase() +
        " " + date.year

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(monthLabel, style = AgendaTheme.type.heading, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Text("‹", style = AgendaTheme.type.heading, color = colors.textSecondary)
        Spacer(Modifier.width(20.dp))
        Text("›", style = AgendaTheme.type.heading, color = colors.textSecondary)
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
                        DayCell(
                            day = dayNum,
                            isToday = dayNum == date.dayOfMonth,
                            isWeekend = c >= 5,
                            busy = dayNum in busyDays,
                            deadline = dayNum in deadlineDays,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isWeekend: Boolean,
    busy: Boolean,
    deadline: Boolean,
) {
    val colors = AgendaTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .then(
                    if (isToday) Modifier.background(colors.textPrimary) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.toString(),
                style = AgendaTheme.type.secondary,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isToday -> colors.background
                    isWeekend -> colors.textFaint
                    else -> colors.textPrimary
                },
            )
        }
        // tiny restrained indicators
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            when {
                deadline -> Dot(colors.deadline)
                busy -> {
                    Dot(colors.textFaint)
                    Dot(colors.textFaint)
                    Dot(colors.textFaint)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(3.dp).clip(CircleShape).background(color))
}

@Composable
private fun TodayPreview(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    SectionLabel("Today")
    Spacer(Modifier.height(14.dp))
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

@Composable
private fun ThisWeek(agenda: DayAgenda) {
    val colors = AgendaTheme.colors
    SectionLabel("This week")
    Spacer(Modifier.height(14.dp))
    agenda.weekWorkload.forEach { (label, load) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        ) {
            Text(
                label,
                style = AgendaTheme.type.secondary,
                color = colors.textSecondary,
                modifier = Modifier.width(44.dp),
            )
            ThinProgressBar(
                progress = load,
                track = colors.divider,
                fill = colors.textSecondary,
                height = 8,
            )
        }
    }
}
