package dev.remgr.f1.feature.pastraces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.pastraces.domain.RaceRepository
import dev.remgr.f1.feature.pastraces.domain.model.Race
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

data class MeetingRaces(
    val meetingKey: Int,
    val meetingName: String,
    val location: String,
    val countryName: String,
    val circuitName: String,
    val sessions: List<Race>
)

data class YearState(
    val year: Int,
    val meetings: List<MeetingRaces> = emptyList(),
    val isLoading: Boolean = false,
    val isExpanded: Boolean = false
)

@HiltViewModel
class PastRacesViewModel @Inject constructor(
    private val repository: RaceRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val years: List<YearState>) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { 
        val currentYear = Year.now().value
        val initialYears = (currentYear downTo 2023).map { year ->
            YearState(year = year, isExpanded = year == currentYear)
        }
        _uiState.value = UiState.Success(initialYears)
        loadYear(currentYear)
    }

    fun retry() {
        val currentYear = Year.now().value
        loadYear(currentYear)
    }

    fun toggleYear(year: Int) {
        val s = _uiState.value as? UiState.Success ?: return
        val currentYears = s.years.toMutableList()
        val index = currentYears.indexOfFirst { it.year == year }
        if (index == -1) return

        val target = currentYears[index]
        val nowExpanded = !target.isExpanded
        
        currentYears[index] = target.copy(isExpanded = nowExpanded)
        _uiState.value = s.copy(years = currentYears)

        if (nowExpanded && target.meetings.isEmpty() && !target.isLoading) {
            loadYear(year)
        }
    }

    private fun loadYear(year: Int) {
        viewModelScope.launch {
            updateYearState(year) { it.copy(isLoading = true) }
            
            runCatching { repository.getRaces(year) }
                .onSuccess { races -> 
                    val grouped = races.groupBy { it.meetingKey() }.map { (meetingKey, sessions) ->
                        val first = sessions.first()
                        val separator = " — "
                        val meetingName = if (first.raceName().contains(separator)) {
                            first.raceName().substringBeforeLast(separator)
                        } else {
                            first.raceName()
                        }
                        MeetingRaces(
                            meetingKey = meetingKey,
                            meetingName = meetingName,
                            location = first.location(),
                            countryName = first.countryName(),
                            circuitName = first.circuitName(),
                            sessions = sessions.sortedBy { it.dateStart() }
                        )
                    }.sortedByDescending { it.sessions.last().dateStart() }
                    
                    updateYearState(year) { it.copy(meetings = grouped, isLoading = false) }
                }
                .onFailure { _ ->
                    updateYearState(year) { it.copy(isLoading = false) }
                }
        }
    }

    private fun updateYearState(year: Int, transform: (YearState) -> YearState) {
        val s = _uiState.value as? UiState.Success ?: return
        val updated = s.years.map { if (it.year == year) transform(it) else it }
        _uiState.value = s.copy(years = updated)
    }
}
