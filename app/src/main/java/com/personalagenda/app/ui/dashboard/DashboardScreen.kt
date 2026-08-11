package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.Task
import com.personalagenda.app.ui.settings.SettingsDialog
import com.personalagenda.app.ui.theme.AgendaTheme
import com.personalagenda.app.ui.theme.AppThemeOption
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerDateFmt = DateTimeFormatter.ofPattern("EEEE · MMMM d, yyyy", Locale.ENGLISH)

@Composable
fun DashboardScreen(
    uiState: DashboardUiState = DashboardUiState(),
    modifier: Modifier = Modifier,
    onToggleTask: (Long) -> Unit = {},
    onQuickAdd: (QuickAddResult) -> Unit = {},
    onUpdateEvent: (Event) -> Unit = {},
    onDeleteEvent: (Long) -> Unit = {},
    onRenameTask: (Long, String) -> Unit = { _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    currentTheme: AppThemeOption = AppThemeOption.SYSTEM,
    onThemeChange: (AppThemeOption) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onPrevDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onToday: () -> Unit = {},
) {
    val colors = AgendaTheme.colors
    val isToday = uiState.agenda.date == java.time.LocalDate.now()
    var showQuickAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        TopHeader(
            dateLabel = uiState.agenda.date.format(headerDateFmt),
            onAdd = { showQuickAdd = true },
            onSearch = onOpenSearch,
            onSettings = { showSettings = true },
        )
        Spacer(Modifier.height(12.dp))

        val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        if (portrait) {
            // Single stacked column: today's breakdown first, then calendar context.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            ) {
                RightColumn(
                    agenda = uiState.agenda,
                    now = uiState.now,
                    isToday = isToday,
                    onToggleTask = onToggleTask,
                    onEventClick = { editingEvent = it },
                    onTaskLongPress = { editingTask = it },
                    onPrevDay = onPrevDay,
                    onNextDay = onNextDay,
                    onToday = onToday,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                Spacer(Modifier.height(28.dp))
                LeftColumn(agenda = uiState.agenda, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // Left column ~36%
                LeftColumn(
                    agenda = uiState.agenda,
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, end = 28.dp, bottom = 16.dp),
                )
                // vertical divider
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .background(colors.divider),
                )
                // Right column ~64%
                RightColumn(
                    agenda = uiState.agenda,
                    now = uiState.now,
                    isToday = isToday,
                    onToggleTask = onToggleTask,
                    onEventClick = { editingEvent = it },
                    onTaskLongPress = { editingTask = it },
                    onPrevDay = onPrevDay,
                    onNextDay = onNextDay,
                    onToday = onToday,
                    modifier = Modifier
                        .weight(0.64f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 32.dp, top = 16.dp, bottom = 16.dp),
                )
            }
        }
    }

    if (showQuickAdd) {
        QuickAddDialog(
            onDismiss = { showQuickAdd = false },
            onSubmit = onQuickAdd,
        )
    }

    editingEvent?.let { event ->
        EventEditorDialog(
            event = event,
            onSave = onUpdateEvent,
            onDelete = onDeleteEvent,
            onDismiss = { editingEvent = null },
        )
    }

    editingTask?.let { task ->
        TaskEditorDialog(
            task = task,
            onRename = onRenameTask,
            onDelete = onDeleteTask,
            onDismiss = { editingTask = null },
        )
    }

    if (showSettings) {
        SettingsDialog(
            current = currentTheme,
            systemDark = isSystemInDarkTheme(),
            onSelect = onThemeChange,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun TopHeader(
    dateLabel: String,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Personal Agenda", style = AgendaTheme.type.bodyStrong, color = colors.textPrimary)
        Spacer(Modifier.width(24.dp))
        Text(dateLabel, style = AgendaTheme.type.secondary, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        HeaderIcon(Icons.Rounded.Search, "Search", onSearch)
        Spacer(Modifier.width(8.dp))
        HeaderIcon(Icons.Rounded.Settings, "Settings", onSettings)
        Spacer(Modifier.width(12.dp))
        AddButton(onClick = onAdd)
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
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
