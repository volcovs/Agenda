package com.personalagenda.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.personalagenda.app.ui.theme.AgendaTheme

enum class Section(val label: String, val icon: ImageVector) {
    AGENDA("Agenda", Icons.Rounded.Today),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth),
    TASKS("Tasks", Icons.Rounded.CheckCircle),
    PROJECTS("Projects", Icons.Rounded.ViewKanban),
    NOTES("Notes", Icons.AutoMirrored.Rounded.Notes),
    INSIGHTS("Insights", Icons.Rounded.Insights),
}

/** Placeholder for sections not yet built, so the nav never dead-ends. */
@Composable
fun ComingSoon(section: Section, modifier: Modifier = Modifier) {
    val colors = AgendaTheme.colors
    Column(
        modifier
            .background(colors.background)
            .fillMaxSize()
            .padding(48.dp),
    ) {
        Text(section.label, style = AgendaTheme.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Text("Coming soon.", style = AgendaTheme.type.body, color = colors.textSecondary)
    }
}

@Composable
fun AppBottomNav(current: Section, onSelect: (Section) -> Unit, compact: Boolean = false) {
    val colors = AgendaTheme.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Section.entries.forEach { section ->
                val active = section == current
                val tint = if (active) colors.textPrimary else colors.textFaint
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(section) }
                        .padding(vertical = 6.dp),
                ) {
                    Icon(
                        section.icon,
                        contentDescription = section.label,
                        tint = tint,
                        modifier = Modifier.size(18.dp),
                    )
                    if (!compact) {
                        Spacer(Modifier.width(8.dp))
                        Text(section.label, style = AgendaTheme.type.secondary, color = tint)
                    }
                }
            }
        }
    }
}
