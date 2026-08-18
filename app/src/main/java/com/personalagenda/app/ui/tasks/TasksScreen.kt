package com.personalagenda.app.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.DatedTask
import com.personalagenda.app.data.Project
import com.personalagenda.app.data.Task
import com.personalagenda.app.ui.dashboard.QuickAddDialog
import com.personalagenda.app.ui.dashboard.QuickAddResult
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.TaskEditorDialog
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

@Composable
fun TasksScreen(
    tasks: List<DatedTask>,
    today: LocalDate,
    projects: List<Project> = emptyList(),
    onToggle: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onSetTaskProject: (Long, Long?) -> Unit = { _, _ -> },
    onQuickAdd: (QuickAddResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DatedTask?>(null) }
    val projectName: (Long?) -> String? = { id -> id?.let { pid -> projects.firstOrNull { it.id == pid }?.name } }

    val todayTasks = tasks.filter { !it.done && it.date != null && !it.date.isAfter(today) }
    val upcoming = tasks.filter { !it.done && it.date != null && it.date.isAfter(today) }
        .sortedBy { it.date }
    val someday = tasks.filter { !it.done && it.date == null }
    val completed = tasks.filter { it.done }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Tasks", style = AgendaTheme.type.display, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${todayTasks.size} today · ${upcoming.size} upcoming · " +
                        "${someday.size} someday · ${completed.size} done",
                    style = AgendaTheme.type.secondary,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            AddButton(onClick = { showAdd = true })
        }

        Spacer(Modifier.height(28.dp))

        if (tasks.isEmpty()) {
            EmptyTasks()
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                TaskGroup("Today", todayTasks, showDate = false, projectName, onToggle) { editing = it }
                TaskGroup("Upcoming", upcoming, showDate = true, projectName, onToggle) { editing = it }
                TaskGroup("Someday", someday, showDate = false, projectName, onToggle) { editing = it }
                TaskGroup("Completed", completed, showDate = false, projectName, onToggle) { editing = it }
            }
        }
    }

    if (showAdd) {
        QuickAddDialog(
            onDismiss = { showAdd = false },
            onSubmit = onQuickAdd,
            defaultDate = today,
            projects = projects,
        )
    }

    editing?.let { dt ->
        TaskEditorDialog(
            task = Task(text = dt.text, done = dt.done, id = dt.id, projectId = dt.projectId),
            projects = projects,
            onRename = onRename,
            onDelete = onDelete,
            onSetProject = onSetTaskProject,
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TaskGroup(
    label: String,
    items: List<DatedTask>,
    showDate: Boolean,
    projectName: (Long?) -> String?,
    onToggle: (Long) -> Unit,
    onLongPress: (DatedTask) -> Unit,
) {
    if (items.isEmpty()) return
    SectionLabel(label)
    Spacer(Modifier.height(12.dp))
    items.forEach { task ->
        TaskRow(
            task,
            showDate,
            projectName(task.projectId),
            onClick = { onToggle(task.id) },
            onLongClick = { onLongPress(task) },
        )
    }
    Spacer(Modifier.height(28.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: DatedTask,
    showDate: Boolean,
    projectName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .then(
                    if (task.done) Modifier.background(colors.textSecondary)
                    else Modifier.border(1.5.dp, colors.textFaint, RoundedCornerShape(5.dp))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) Text("✓", style = AgendaTheme.type.tiny, color = colors.background)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            task.text,
            style = AgendaTheme.type.body,
            color = if (task.done) colors.textFaint else colors.textPrimary,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
        )
        if (projectName != null) {
            Spacer(Modifier.width(10.dp))
            ProjectTag(projectName)
        }
        if (showDate && task.date != null) {
            Spacer(Modifier.weight(1f))
            Text(task.date.format(dateFmt), style = AgendaTheme.type.secondary, color = colors.textFaint)
        }
    }
}

@Composable
private fun ProjectTag(name: String) {
    val colors = AgendaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(name, style = AgendaTheme.type.tiny, color = colors.textSecondary)
    }
}

@Composable
private fun EmptyTasks() {
    val colors = AgendaTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
        Text("No tasks yet", style = AgendaTheme.type.display, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Text("Add your first task with the + button.", style = AgendaTheme.type.body, color = colors.textSecondary)
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
