package com.personalagenda.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.DatedTask
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.NoteItem
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val stampFmt = DateTimeFormatter.ofPattern("MMM d · HH:mm", Locale.ENGLISH)

@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    projects: List<Project>,
    tasks: List<DatedTask>,
    events: List<Event>,
    onAdd: (String, com.personalagenda.app.data.NoteLinks) -> Unit,
    onUpdate: (Long, String, com.personalagenda.app.data.NoteLinks) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NoteItem?>(null) }
    val projectsById = projects.associateBy { it.id }
    val tasksById = tasks.associateBy { it.id }
    val eventsById = events.associateBy { it.id }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Notes", style = AgendaTheme.type.display, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (notes.isEmpty()) "No notes yet" else "${notes.size} notes",
                    style = AgendaTheme.type.secondary,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            AddButton(onClick = { showAdd = true })
        }

        Spacer(Modifier.height(28.dp))

        if (notes.isEmpty()) {
            Text("Capture a thought with the + button.", style = AgendaTheme.type.body, color = colors.textSecondary)
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                notes.forEach { note ->
                    val tags = buildList {
                        projectsById[note.links.projectId]?.let { add(it.name to it.category.color(colors)) }
                        eventsById[note.links.eventId]?.let { add(it.title to it.category.color(colors)) }
                        tasksById[note.links.taskId]?.let { add(it.text to colors.textSecondary) }
                    }
                    NoteCard(note, tags, onClick = { editing = note })
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    if (showAdd) {
        NoteEditorDialog(
            projects = projects,
            tasks = tasks,
            events = events,
            onSave = { text, links -> onAdd(text, links) },
            onDismiss = { showAdd = false },
        )
    }

    editing?.let { note ->
        NoteEditorDialog(
            projects = projects,
            tasks = tasks,
            events = events,
            initialText = note.text,
            initialLinks = note.links,
            onSave = { text, links -> onUpdate(note.id, text, links) },
            onDelete = { onDelete(note.id) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    tags: List<Pair<String, androidx.compose.ui.graphics.Color>>,
    onClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text(
            note.text,
            style = AgendaTheme.type.body,
            color = colors.textPrimary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                note.date.atTime(note.time).format(stampFmt),
                style = AgendaTheme.type.tiny,
                color = colors.textFaint,
            )
            tags.forEach { (label, color) ->
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = AgendaTheme.type.tiny, color = colors.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.textPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add", tint = colors.background, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Add", style = AgendaTheme.type.secondary, color = colors.background)
    }
}
