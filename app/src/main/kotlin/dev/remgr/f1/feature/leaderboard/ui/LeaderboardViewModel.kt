package dev.remgr.f1.feature.leaderboard.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: StandingsRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val drivers: List<DriverStanding>,
            val constructors: List<ConstructorStanding>,
        ) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    var selectedTab by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    fun selectTab(tab: Int) { selectedTab = tab }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                val year         = Year.now().value
                val drivers      = repository.getDriverStandings(year)
                val constructors = repository.getConstructorStandings(year)
                UiState.Success(drivers, constructors)
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}
