package com.personalagenda.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.personalagenda.app.AgendaApp
import com.personalagenda.app.data.AgendaRepository
import com.personalagenda.app.data.DatedTask
import com.personalagenda.app.data.DayAgenda
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.InsightsData
import com.personalagenda.app.data.MockData
import com.personalagenda.app.data.NoteItem
import com.personalagenda.app.data.NoteLinks
import com.personalagenda.app.data.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Everything the dashboard needs to render, in one immutable snapshot. */
data class DashboardUiState(
    val agenda: DayAgenda = MockData.day,
    val now: LocalTime = MockData.demoNow,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: AgendaRepository) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    /** The day the dashboard is currently showing (defaults to today). */
    private val selectedDate = MutableStateFlow(today)

    init {
        viewModelScope.launch { repository.seedIfEmpty(today) }
    }

    /**
     * A live clock, truncated to the minute. Emits every second internally but
     * only pushes downstream when the minute changes, so the UI recomposes at
     * most once a minute rather than continuously.
     */
    private val minuteTicker = flow {
        while (true) {
            emit(LocalTime.now())
            delay(1_000)
        }
    }
        .map { it.withSecond(0).withNano(0) }
        .distinctUntilChanged()

    private val agendaFlow = selectedDate.flatMapLatest { repository.observeDay(it) }

    val uiState = combine(agendaFlow, minuteTicker) { agenda, now ->
        DashboardUiState(agenda = agenda, now = now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(agenda = MockData.emptyDay.copy(date = LocalDate.now()), now = LocalTime.now()),
    )

    /** All tasks across dates, for the Tasks screen. */
    val tasks: StateFlow<List<DatedTask>> = repository.observeAllTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** All notes across dates, for the Notes screen. */
    val notes: StateFlow<List<NoteItem>> = repository.observeAllNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** All projects, for the Projects screen. */
    val projects: StateFlow<List<Project>> = repository.observeProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Monday of the current week. */
    private val weekStart: LocalDate = today.minusDays((today.dayOfWeek.value - 1).toLong())

    /** Reflective weekly statistics, for the Insights screen. */
    val insights: StateFlow<InsightsData> = repository.observeWeekInsights(weekStart).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsData(weekStart = weekStart),
    )

    /** Today's date, used by screens for grouping. */
    val todayDate: LocalDate get() = today

    fun previousDay() { selectedDate.value = selectedDate.value.minusDays(1) }
    fun nextDay() { selectedDate.value = selectedDate.value.plusDays(1) }
    fun goToToday() { selectedDate.value = today }
    fun selectDate(date: LocalDate) { selectedDate.value = date }

    fun toggleTask(id: Long) = viewModelScope.launch { repository.toggleTask(id) }
    fun addTask(text: String, date: LocalDate?, projectId: Long? = null) =
        viewModelScope.launch { repository.addTask(date, text, projectId) }
    fun setTaskProject(id: Long, projectId: Long?) =
        viewModelScope.launch { repository.setTaskProject(id, projectId) }
    fun addNote(text: String, links: NoteLinks = NoteLinks()) =
        viewModelScope.launch { repository.setNote(selectedDate.value, text, uiState.value.now, links) }
    fun addEvent(event: Event) = viewModelScope.launch { repository.addEvent(selectedDate.value, event) }

    fun updateEvent(event: Event) = viewModelScope.launch { repository.updateEvent(event) }
    fun deleteEvent(id: Long) = viewModelScope.launch { repository.deleteEvent(id) }
    fun renameTask(id: Long, text: String) = viewModelScope.launch { repository.renameTask(id, text) }
    fun deleteTask(id: Long) = viewModelScope.launch { repository.deleteTask(id) }
    fun updateNote(id: Long, text: String, links: NoteLinks) =
        viewModelScope.launch { repository.updateNote(id, text, links) }
    fun deleteNote(id: Long) = viewModelScope.launch { repository.deleteNote(id) }
    fun addProject(project: Project) = viewModelScope.launch { repository.addProject(project) }
    fun updateProject(project: Project) = viewModelScope.launch { repository.updateProject(project) }
    fun deleteProject(id: Long) = viewModelScope.launch { repository.deleteProject(id) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AgendaApp
                DashboardViewModel(app.repository)
            }
        }
    }
}
