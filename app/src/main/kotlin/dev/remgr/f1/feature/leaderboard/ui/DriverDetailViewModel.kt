package dev.remgr.f1.feature.leaderboard.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

data class DriverSeasonRow(
    val year: Int,
    val position: Int,
    val points: Int,
    val wins: Int,
    val teamName: String,
    val teamColour: String,
)

data class DriverDetailState(
    val driverNumber: Int,
    val fullName: String,
    val nameAcronym: String,
    val teamName: String,
    val teamColour: String,
    val headshotUrl: String?,
    val seasons: List<DriverSeasonRow>,
)

@HiltViewModel
class DriverDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StandingsRepository,
) : ViewModel() {

    private val driverNumber: Int = checkNotNull(savedStateHandle["driverNumber"])

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val detail: DriverDetailState) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                val currentYear = Year.now().value
                val years = (currentYear downTo 2023).toList()

                var latestDriver: DriverStanding? = null
                val seasons = mutableListOf<DriverSeasonRow>()

                for (year in years) {
                    val standings = runCatching {
                        if (year == currentYear) repository.getDriverStandings(year)
                        else repository.getCachedDriverStandings(year)
                    }.getOrNull() ?: continue
                    val driver = standings.find { it.driverNumber() == driverNumber } ?: continue
                    if (latestDriver == null) latestDriver = driver
                    seasons += DriverSeasonRow(
                        year       = year,
                        position   = driver.position(),
                        points     = driver.points(),
                        wins       = driver.wins(),
                        teamName   = driver.teamName(),
                        teamColour = driver.teamColour(),
                    )
                }

                val base = latestDriver ?: error("Driver $driverNumber not found in any season")
                DriverDetailState(
                    driverNumber = driverNumber,
                    fullName     = base.fullName(),
                    nameAcronym  = base.nameAcronym(),
                    teamName     = base.teamName(),
                    teamColour   = base.teamColour(),
                    headshotUrl  = base.headshotUrl(),
                    seasons      = seasons,
                )
            }.onSuccess { _uiState.value = UiState.Success(it) }
             .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}
