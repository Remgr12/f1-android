package dev.remgr.f1.feature.leaderboard.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.time.Year
import javax.inject.Inject

data class ConstructorSeasonRow(
    val year: Int,
    val position: Int,
    val points: Int,
    val wins: Int,
    val driverAcronyms: List<String>,
)

data class ConstructorDetailState(
    val teamName: String,
    val teamColour: String,
    val seasons: List<ConstructorSeasonRow>,
)

@HiltViewModel
class ConstructorDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StandingsRepository,
) : ViewModel() {

    private val teamName: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["teamName"]), "UTF-8"
    )

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val detail: ConstructorDetailState) : UiState
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

                var latestColour = "FFFFFF"
                val seasons = mutableListOf<ConstructorSeasonRow>()

                for (year in years) {
                    val standings = runCatching { repository.getConstructorStandings(year) }.getOrNull() ?: continue
                    val constructor = standings.find { it.teamName().equals(teamName, ignoreCase = true) } ?: continue
                    latestColour = constructor.teamColour()
                    seasons += ConstructorSeasonRow(
                        year           = year,
                        position       = constructor.position(),
                        points         = constructor.points(),
                        wins           = constructor.wins(),
                        driverAcronyms = constructor.driverAcronyms(),
                    )
                }

                if (seasons.isEmpty()) error("Constructor $teamName not found in any season")
                ConstructorDetailState(
                    teamName   = teamName,
                    teamColour = latestColour,
                    seasons    = seasons,
                )
            }.onSuccess { _uiState.value = UiState.Success(it) }
             .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}
