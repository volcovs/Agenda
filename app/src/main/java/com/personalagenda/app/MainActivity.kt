package com.personalagenda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personalagenda.app.data.MockData
import com.personalagenda.app.ui.AppBottomNav
import com.personalagenda.app.ui.Section
import com.personalagenda.app.ui.dashboard.DashboardScreen
import com.personalagenda.app.ui.dashboard.DashboardUiState
import com.personalagenda.app.ui.dashboard.DashboardViewModel
import com.personalagenda.app.ui.calendar.CalendarRoute
import com.personalagenda.app.ui.dashboard.QuickAddResult
import com.personalagenda.app.ui.insights.InsightsScreen
import com.personalagenda.app.ui.notes.NotesScreen
import com.personalagenda.app.ui.projects.ProjectsScreen
import com.personalagenda.app.ui.search.SearchScreen
import com.personalagenda.app.ui.tasks.TasksScreen
import com.personalagenda.app.ui.theme.AgendaTheme
import com.personalagenda.app.ui.theme.AppThemeOption
import com.personalagenda.app.ui.theme.PersonalAgendaTheme
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as AgendaApp
        setContent {
            val theme by app.settingsStore.theme.collectAsState()
            PersonalAgendaTheme(option = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AgendaTheme.colors.background,
                ) {
                    AppRoot(theme = theme, onThemeChange = app.settingsStore::setTheme)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(theme: AppThemeOption, onThemeChange: (AppThemeOption) -> Unit) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    var sectionIndex by rememberSaveable { mutableStateOf(Section.AGENDA.ordinal) }
    val section = Section.entries[sectionIndex]
    var showSearch by remember { mutableStateOf(false) }

    val onQuickAdd: (QuickAddResult) -> Unit = { result ->
        when (result) {
            is QuickAddResult.NewEvent -> vm.addEvent(result.event)
            is QuickAddResult.AddTask -> vm.addTask(result.text, result.someday)
            is QuickAddResult.AddNote -> vm.addNote(result.text)
            is QuickAddResult.AddFocus -> vm.addFocus(result.text)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                Section.AGENDA -> DashboardScreen(
                    uiState = uiState,
                    onToggleTask = { id -> vm.toggleTask(id) },
                    onQuickAdd = onQuickAdd,
                    onUpdateEvent = { event -> vm.updateEvent(event) },
                    onDeleteEvent = { id -> vm.deleteEvent(id) },
                    onRenameTask = { id, text -> vm.renameTask(id, text) },
                    onDeleteTask = { id -> vm.deleteTask(id) },
                    currentTheme = theme,
                    onThemeChange = onThemeChange,
                    onOpenSearch = { showSearch = true },
                    onPrevDay = { vm.previousDay() },
                    onNextDay = { vm.nextDay() },
                    onToday = { vm.goToToday() },
                )
                Section.TASKS -> TasksScreen(
                    tasks = tasks,
                    today = vm.todayDate,
                    onToggle = { id -> vm.toggleTask(id) },
                    onRename = { id, text -> vm.renameTask(id, text) },
                    onDelete = { id -> vm.deleteTask(id) },
                    onQuickAdd = onQuickAdd,
                )
                Section.CALENDAR -> CalendarRoute()
                Section.NOTES -> NotesScreen(
                    notes = notes,
                    projects = projects,
                    tasks = tasks,
                    events = uiState.agenda.events,
                    onAdd = { text, links -> vm.addNote(text, links) },
                    onUpdate = { id, text, links -> vm.updateNote(id, text, links) },
                    onDelete = { id -> vm.deleteNote(id) },
                )
                Section.PROJECTS -> ProjectsScreen(
                    projects = projects,
                    onAdd = { project -> vm.addProject(project) },
                    onUpdate = { project -> vm.updateProject(project) },
                    onDelete = { id -> vm.deleteProject(id) },
                )
                Section.INSIGHTS -> InsightsScreen(insights)
            }
        }
        val portrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
        AppBottomNav(current = section, onSelect = { sectionIndex = it.ordinal }, compact = portrait)
    }

        if (showSearch) {
            SearchScreen(
                events = uiState.agenda.events,
                tasks = tasks,
                notes = notes,
                projects = projects,
                onOpenSection = { target -> sectionIndex = target.ordinal; showSearch = false },
                onClose = { showSearch = false },
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            )
        }
    }
}

// Landscape tablet previews (Android Studio → open MainActivity → Split/Design).
@Preview(name = "Dashboard · Light", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardLightPreview() {
    PersonalAgendaTheme(option = AppThemeOption.LIGHT) { DashboardScreen() }
}

@Preview(name = "Dashboard · Dark", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardDarkPreview() {
    PersonalAgendaTheme(option = AppThemeOption.DARK) { DashboardScreen() }
}

@Preview(name = "Morning", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardMorningPreview() {
    PersonalAgendaTheme {
        DashboardScreen(uiState = DashboardUiState(MockData.day, LocalTime.of(8, 0)))
    }
}

@Preview(name = "Evening", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardEveningPreview() {
    PersonalAgendaTheme {
        DashboardScreen(uiState = DashboardUiState(MockData.day, LocalTime.of(22, 0)))
    }
}

@Preview(name = "Empty day", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun DashboardEmptyPreview() {
    PersonalAgendaTheme {
        DashboardScreen(uiState = DashboardUiState(MockData.emptyDay, LocalTime.of(10, 0)))
    }
}
