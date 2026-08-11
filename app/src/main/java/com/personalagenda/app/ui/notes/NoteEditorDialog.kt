package com.personalagenda.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.data.DatedTask
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.NoteLinks
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.dashboard.FieldLabel
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.TextAction
import com.personalagenda.app.ui.dashboard.UnderlinedField
import com.personalagenda.app.ui.theme.AgendaTheme

@Composable
fun NoteEditorDialog(
    projects: List<Project>,
    tasks: List<DatedTask>,
    events: List<Event>,
    initialText: String = "",
    initialLinks: NoteLinks = NoteLinks(),
    onSave: (String, NoteLinks) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val colors = AgendaTheme.colors
    var text by remember { mutableStateOf(initialText) }
    var projectId by remember { mutableStateOf(initialLinks.projectId) }
    var taskId by remember { mutableStateOf(initialLinks.taskId) }
    var eventId by remember { mutableStateOf(initialLinks.eventId) }
    val canSave = text.isNotBlank()

    fun save() {
        if (!canSave) return
        onSave(text.trim(), NoteLinks(projectId = projectId, eventId = eventId, taskId = taskId))
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
        ) {
            SectionLabel(if (onDelete == null) "New note" else "Edit note")
            Spacer(Modifier.height(20.dp))

            UnderlinedField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Write a note…",
                singleLine = false,
                imeAction = ImeAction.Default,
            )

            if (projects.isNotEmpty()) {
                LinkSection("Project") {
                    LinkChip("None", projectId == null) { projectId = null }
                    projects.forEach { p ->
                        LinkChip(p.name, projectId == p.id, p.category.color(colors)) { projectId = p.id }
                    }
                }
            }
            if (events.isNotEmpty()) {
                LinkSection("Event") {
                    LinkChip("None", eventId == null) { eventId = null }
                    events.forEach { e ->
                        LinkChip(e.title, eventId == e.id, e.category.color(colors)) { eventId = e.id }
                    }
                }
            }
            if (tasks.isNotEmpty()) {
                LinkSection("Task") {
                    LinkChip("None", taskId == null) { taskId = null }
                    tasks.forEach { t ->
                        LinkChip(t.text, taskId == t.id) { taskId = t.id }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onDelete(); onDismiss() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text("Delete", style = AgendaTheme.type.bodyStrong, color = colors.deadline)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", muted = true, enabled = true, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                TextAction("Save", muted = false, enabled = canSave, onClick = { save() })
            }
        }
    }
}

@Composable
private fun LinkSection(label: String, chips: @Composable () -> Unit) {
    Spacer(Modifier.height(24.dp))
    FieldLabel("Link to $label".uppercase())
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { chips() }
}

@Composable
private fun LinkChip(
    label: String,
    selected: Boolean,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    val tint = accent ?: colors.textSecondary
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (selected) tint else colors.divider, RoundedCornerShape(50))
            .background(if (selected) tint.copy(alpha = 0.12f) else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = AgendaTheme.type.secondary,
            color = if (selected) colors.textPrimary else colors.textSecondary,
        )
    }
}
