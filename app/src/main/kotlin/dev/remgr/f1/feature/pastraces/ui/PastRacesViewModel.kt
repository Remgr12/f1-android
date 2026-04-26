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

data class YearRaces(
    val year: Int,
    val meetings: List<MeetingRaces>
)

@HiltViewModel
class PastRacesViewModel @Inject constructor(
    private val repository: RaceRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val years: List<YearRaces>) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                val currentYear = Year.now().value
                val yearsToFetch = (currentYear downTo 2023).toList()
                
                yearsToFetch.map { year ->
                    val races = repository.getRaces(year)
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
                    YearRaces(year, grouped)
                }
            }
                .onSuccess { years -> _uiState.value = UiState.Success(years) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}

