package com.personalagenda.app.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.dashboard.ThinProgressBar
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dueFmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    onAdd: (Project) -> Unit,
    onUpdate: (Project) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Project?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Projects", style = AgendaTheme.type.display, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (projects.isEmpty()) "No projects yet" else "${projects.size} projects",
                    style = AgendaTheme.type.secondary,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            AddButton(onClick = { showAdd = true })
        }

        Spacer(Modifier.height(28.dp))

        if (projects.isEmpty()) {
            Text("Add a project with the + button.", style = AgendaTheme.type.body, color = colors.textSecondary)
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                projects.chunked(2).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        rowItems.forEach { project ->
                            Box(Modifier.weight(1f)) {
                                ProjectCard(project, onClick = { editing = project })
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showAdd) {
        ProjectEditorDialog(initial = null, onSave = onAdd, onDismiss = { showAdd = false })
    }

    editing?.let { project ->
        ProjectEditorDialog(
            initial = project,
            onSave = onUpdate,
            onDelete = { onDelete(project.id) },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    val accent = project.category.color(colors)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(10.dp))
            Text(project.name, style = AgendaTheme.type.heading, color = colors.textPrimary)
            Spacer(Modifier.weight(1f))
            if (project.status != null) {
                Text(project.status, style = AgendaTheme.type.tiny, color = colors.textFaint)
            }
        }

        if (project.description != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                project.description,
                style = AgendaTheme.type.secondary,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(16.dp))
        if (project.hasTasks) {
            ThinProgressBar(progress = project.effectiveProgress, track = colors.divider, fill = accent, height = 6)
            Spacer(Modifier.height(6.dp))
            Text(
                "${(project.effectiveProgress * 100).roundToInt()}% · ${project.linkedTasksDone}/${project.linkedTasksTotal} tasks",
                style = AgendaTheme.type.tiny,
                color = colors.textFaint,
            )
        } else {
            ThinProgressBar(progress = 0f, track = colors.divider, fill = accent, height = 6)
            Spacer(Modifier.height(6.dp))
            Text("No tasks yet", style = AgendaTheme.type.tiny, color = colors.textFaint)
        }

        if (project.nextAction != null) {
            Spacer(Modifier.height(14.dp))
            Text("NEXT", style = AgendaTheme.type.tiny, color = colors.textFaint)
            Spacer(Modifier.height(2.dp))
            Text(project.nextAction, style = AgendaTheme.type.body, color = colors.textPrimary)
        }

        if (project.deadline != null) {
            Spacer(Modifier.height(12.dp))
            Text("Due ${project.deadline.format(dueFmt)}", style = AgendaTheme.type.secondary, color = colors.deadline)
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
