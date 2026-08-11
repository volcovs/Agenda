package com.personalagenda.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.InsightsData
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.ThinProgressBar
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rangeFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

private fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        min == 0 -> "0m"
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@Composable
fun InsightsScreen(data: InsightsData, modifier: Modifier = Modifier) {
    val colors = AgendaTheme.colors
    val weekEnd = data.weekStart.plusDays(6)

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Text("Insights", style = AgendaTheme.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "This week · ${data.weekStart.format(rangeFmt)} – ${weekEnd.format(rangeFmt)}",
            style = AgendaTheme.type.secondary,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(28.dp))

        // Stat tiles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatTile("Scheduled time", formatMinutes(data.scheduledMinutes), Modifier.weight(1f))
            StatTile("Completed tasks", data.completedTasks.toString(), Modifier.weight(1f))
            StatTile("Events", data.events.toString(), Modifier.weight(1f))
            StatTile("Active days", data.activeDays.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel("Workload")
        Spacer(Modifier.height(20.dp))
        WorkloadChart(data.perDayMinutes)

        Spacer(Modifier.height(36.dp))
        SectionLabel("Time by category")
        Spacer(Modifier.height(20.dp))
        if (data.byCategoryMinutes.isEmpty()) {
            Text("No scheduled time this week.", style = AgendaTheme.type.body, color = colors.textSecondary)
        } else {
            val total = data.byCategoryMinutes.sumOf { it.second }.coerceAtLeast(1)
            data.byCategoryMinutes.forEach { (category, minutes) ->
                CategoryRow(category, minutes, total)
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(
            "A quiet look back — not a scorecard.",
            style = AgendaTheme.type.secondary,
            color = colors.textFaint,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AgendaTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Text(value, style = AgendaTheme.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = AgendaTheme.type.secondary, color = colors.textSecondary)
    }
}

@Composable
private fun WorkloadChart(perDayMinutes: List<Int>) {
    val colors = AgendaTheme.colors
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val max = (perDayMinutes.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(
        Modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        perDayMinutes.forEachIndexed { i, minutes ->
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (minutes > 0) formatMinutes(minutes) else "",
                    style = AgendaTheme.type.tiny,
                    color = colors.textFaint,
                )
                Spacer(Modifier.height(6.dp))
                // Bar: proportion of the tallest day, leaving room for the label above.
                val fraction = minutes.toFloat() / max
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.02f, 1f) * 0.8f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (minutes > 0) colors.personal else colors.divider),
                )
                Spacer(Modifier.height(8.dp))
                Text(labels[i], style = AgendaTheme.type.secondary, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: com.personalagenda.app.data.Category, minutes: Int, total: Int) {
    val colors = AgendaTheme.colors
    val accent = category.color(colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(
            category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = AgendaTheme.type.body,
            color = colors.textPrimary,
            modifier = Modifier.width(120.dp),
        )
        ThinProgressBar(
            progress = minutes.toFloat() / total,
            track = colors.divider,
            fill = accent,
            height = 8,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            formatMinutes(minutes),
            style = AgendaTheme.type.secondary,
            color = colors.textSecondary,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End,
        )
    }
}
