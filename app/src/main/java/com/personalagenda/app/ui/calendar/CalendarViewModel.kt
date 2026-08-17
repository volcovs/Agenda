package com.personalagenda.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.personalagenda.app.AgendaApp
import com.personalagenda.app.data.AgendaRepository
import com.personalagenda.app.data.DaySpread
import com.personalagenda.app.data.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The weekly-spread Calendar shows [SPREAD_DAYS] days starting from a Monday. */
const val SPREAD_DAYS = 12

data class CalendarUiState(
    val start: LocalDate,
    val days: List<DaySpread> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: AgendaRepository) : ViewModel() {

    /** Monday of the week containing [date]. */
    private fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - 1).toLong())

    private val start = MutableStateFlow(mondayOf(LocalDate.now()))

    val uiState: StateFlow<CalendarUiState> =
        start.flatMapLatest { s ->
            repository.observeSpread(s, SPREAD_DAYS).map { CalendarUiState(start = s, days = it) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(mondayOf(LocalDate.now())),
        )

    fun previous() { start.value = start.value.minusDays(SPREAD_DAYS.toLong()) }
    fun next() { start.value = start.value.plusDays(SPREAD_DAYS.toLong()) }
    fun goToToday() { start.value = mondayOf(LocalDate.now()) }

    fun toggleTask(id: Long) = viewModelScope.launch { repository.toggleTask(id) }
    fun updateEvent(event: Event) = viewModelScope.launch { repository.updateEvent(event) }
    fun deleteEvent(id: Long) = viewModelScope.launch { repository.deleteEvent(id) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AgendaApp
                CalendarViewModel(app.repository)
            }
        }
    }
}
