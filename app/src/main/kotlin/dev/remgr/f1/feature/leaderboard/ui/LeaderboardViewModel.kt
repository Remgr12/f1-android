package dev.remgr.f1.feature.leaderboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import java.time.Year
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: StandingsRepository,
    private val service: OpenF1Service,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(
            val drivers: List<DriverStanding>,
            val constructors: List<ConstructorStanding>,
            val nextGp: NextGpInfo?,
        ) : UiState
        data class Error(val message: String) : UiState
    }

    data class NextGpInfo(
        val name: String,
        val daysUntil: Long,
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                val year         = Year.now().value
                val drivers      = repository.getDriverStandings(year)
                val constructors = repository.getConstructorStandings(year)
                
                val nextGp = try {
                    val meetings = service.getMeetings(mapOf("year" to year.toString()))
                    val now = Instant.now()
                    val next = meetings
                        .filter { 
                            runCatching { OffsetDateTime.parse(it.dateStart).toInstant() }
                                .getOrNull()?.isAfter(now) == true 
                        }
                        .minByOrNull { it.dateStart }
                    
                    next?.let {
                        val startTime = OffsetDateTime.parse(it.dateStart).toInstant()
                        val days = ChronoUnit.DAYS.between(now, startTime)
                        NextGpInfo(it.meetingName, days)
                    }
                } catch (e: Exception) {
                    null
                }

                UiState.Success(drivers, constructors, nextGp)
            }.onSuccess { _uiState.value = it }
             .onFailure { _uiState.value = UiState.Error(it.message ?: "Unknown error") }
        }
    }
}

