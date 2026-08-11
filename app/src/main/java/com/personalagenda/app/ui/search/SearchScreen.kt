package com.personalagenda.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.DatedTask
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.NoteItem
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.Section
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.UnderlinedField
import com.personalagenda.app.ui.theme.AgendaTheme

@Composable
fun SearchScreen(
    events: List<Event>,
    tasks: List<DatedTask>,
    notes: List<NoteItem>,
    projects: List<Project>,
    onOpenSection: (Section) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    fun String?.hit() = !this.isNullOrBlank() && this.contains(q, ignoreCase = true)

    val eventHits = if (q.isEmpty()) emptyList() else events.filter { it.title.hit() || it.subtitle.hit() }
    val taskHits = if (q.isEmpty()) emptyList() else tasks.filter { it.text.hit() }
    val noteHits = if (q.isEmpty()) emptyList() else notes.filter { it.text.hit() }
    val projectHits = if (q.isEmpty()) emptyList() else projects.filter { it.name.hit() || it.description.hit() }
    val total = eventHits.size + taskHits.size + noteHits.size + projectHits.size

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                UnderlinedField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search events, tasks, notes, projects…",
                    singleLine = true,
                    imeAction = ImeAction.Search,
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            when {
                q.isEmpty() -> Text(
                    "Type to search across everything.",
                    style = AgendaTheme.type.body,
                    color = colors.textSecondary,
                )
                total == 0 -> Text(
                    "No matches for “$q”.",
                    style = AgendaTheme.type.body,
                    color = colors.textSecondary,
                )
                else -> {
                    ResultGroup("Events", eventHits.map { it.title to it.subtitle }, Section.AGENDA, onOpenSection)
                    ResultGroup("Tasks", taskHits.map { it.text to null }, Section.TASKS, onOpenSection)
                    ResultGroup("Notes", noteHits.map { it.text to null }, Section.NOTES, onOpenSection)
                    ResultGroup("Projects", projectHits.map { it.name to it.description }, Section.PROJECTS, onOpenSection)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultGroup(
    label: String,
    items: List<Pair<String, String?>>,
    section: Section,
    onOpen: (Section) -> Unit,
) {
    if (items.isEmpty()) return
    val colors = AgendaTheme.colors
    SectionLabel("$label · ${items.size}")
    Spacer(Modifier.height(12.dp))
    items.forEach { (primary, secondary) ->
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { onOpen(section) }
                .padding(vertical = 10.dp),
        ) {
            Text(primary, style = AgendaTheme.type.body, color = colors.textPrimary, maxLines = 1)
            if (!secondary.isNullOrBlank()) {
                Text(secondary, style = AgendaTheme.type.secondary, color = colors.textSecondary, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}
