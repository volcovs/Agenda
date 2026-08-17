package com.personalagenda.app.ui.dashboard

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
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.data.Project
import com.personalagenda.app.data.Task
import com.personalagenda.app.ui.theme.AgendaTheme

@Composable
fun TaskEditorDialog(
    task: Task,
    projects: List<Project> = emptyList(),
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onSetProject: (Long, Long?) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    val colors = AgendaTheme.colors
    var text by remember { mutableStateOf(task.text) }
    var projectId by remember { mutableStateOf(task.projectId) }
    val canSave = text.isNotBlank()

    fun save() {
        if (!canSave) return
        onRename(task.id, text.trim())
        if (projectId != task.projectId) onSetProject(task.id, projectId)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .padding(28.dp),
        ) {
            SectionLabel("Edit task")
            Spacer(Modifier.height(20.dp))

            UnderlinedField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Task",
                singleLine = true,
                imeAction = ImeAction.Done,
                onImeAction = { save() },
            )

            if (projects.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                FieldLabel("Project")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskProjectChip("None", selected = projectId == null) { projectId = null }
                    projects.forEach { p ->
                        TaskProjectChip(p.name, selected = projectId == p.id) { projectId = p.id }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDelete(task.id); onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text("Delete", style = AgendaTheme.type.bodyStrong, color = colors.deadline)
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
private fun TaskProjectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) Modifier.background(colors.textPrimary)
                else Modifier.border(1.dp, colors.divider, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = AgendaTheme.type.secondary,
            color = if (selected) colors.background else colors.textSecondary,
        )
    }
}
