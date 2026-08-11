package com.personalagenda.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.personalagenda.app.AgendaApp
import com.personalagenda.app.data.AgendaRepository
import com.personalagenda.app.data.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth,
    val selected: LocalDate,
    val counts: Map<Long, Int> = emptyMap(),
    val selectedEvents: List<Event> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: AgendaRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    private val counts = month.flatMapLatest { ym ->
        repository.observeMonthCounts(ym.atDay(1), ym.atEndOfMonth())
    }

    private val selectedEvents = selected.flatMapLatest { date ->
        repository.observeDay(date).map { it.events }
    }

    val uiState: StateFlow<CalendarUiState> =
        combine(month, selected, counts, selectedEvents) { m, s, c, e ->
            CalendarUiState(month = m, selected = s, counts = c, selectedEvents = e)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(YearMonth.now(), LocalDate.now()),
        )

    fun updateEvent(event: Event) = viewModelScope.launch { repository.updateEvent(event) }
    fun deleteEvent(id: Long) = viewModelScope.launch { repository.deleteEvent(id) }

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun selectDate(date: LocalDate) { selected.value = date }
    fun goToToday() {
        val today = LocalDate.now()
        month.value = YearMonth.from(today)
        selected.value = today
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AgendaApp
                CalendarViewModel(app.repository)
            }
        }
    }
}
