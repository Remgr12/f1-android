package dev.remgr.f1.feature.trackmap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.pastraces.domain.RaceRepository
import dev.remgr.f1.feature.pastraces.domain.model.Race
import dev.remgr.f1.feature.trackmap.domain.TrackMapRepository
import dev.remgr.f1.feature.trackmap.domain.TrackMapState
import dev.remgr.f1.feature.trackmap.domain.TrackSessionOption
import dev.remgr.f1.feature.trackmap.domain.model.TrackPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TrackMapViewModel @Inject constructor(
    private val repo: TrackMapRepository,
    private val raceRepo: RaceRepository,
) : ViewModel() {

    private val _trackOptions = MutableStateFlow(
        listOf(TrackSessionOption(sessionKey = null, label = "Latest session (Live)")),
    )
    val trackOptions: StateFlow<List<TrackSessionOption>> = _trackOptions.asStateFlow()

    private val _selectedSessionKey = MutableStateFlow<Int?>(null)
    val selectedSessionKey: StateFlow<Int?> = _selectedSessionKey.asStateFlow()

    private val _playbackTime = MutableStateFlow<Instant?>(null)
    private val _playbackPositions = MutableStateFlow<List<TrackPosition>?>(null)
    private val _raceResult = MutableStateFlow<Race?>(null)

    val state: StateFlow<TrackMapState> = combine(
        _selectedSessionKey.flatMapLatest { selected -> repo.getTrackMapState(selected) }.catch { /* log */ },
        _playbackTime,
        _playbackPositions,
        _raceResult
    ) { baseState, pbTime, pbPositions, race ->
        baseState.copy(
            currentPlaybackTime = pbTime ?: baseState.currentPlaybackTime,
            carPositions = pbPositions ?: baseState.carPositions,
            race = race
        )
    }.stateIn(
        scope   = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackMapState(emptyList(), emptyList()),
    )

    private var scrubJob: Job? = null

    init {
        viewModelScope.launch {
            val options = runCatching { repo.getTrackOptions() }.getOrDefault(emptyList())
            if (options.isNotEmpty()) {
                _trackOptions.value = options
            }
        }

        viewModelScope.launch {
            _selectedSessionKey.collectLatest { sessionKey ->
                if (sessionKey != null) {
                    val label = _trackOptions.value.find { it.sessionKey == sessionKey }?.label
                    val year = label?.substringBefore(" ")?.toIntOrNull()
                    if (year != null) {
                        _raceResult.value = runCatching { raceRepo.getRaces(year).find { it.sessionKey == sessionKey } }.getOrNull()
                    } else {
                        _raceResult.value = null
                    }
                } else {
                    _raceResult.value = null
                }
            }
        }
    }

    fun selectSession(sessionKey: Int?) {
        _playbackTime.value = null
        _playbackPositions.value = null
        _selectedSessionKey.value = sessionKey
    }

    fun scrubToTime(progress: Float) {
        val sessionKey = _selectedSessionKey.value ?: return
        val currentBaseState = state.value
        val start = currentBaseState.sessionStartTime ?: return
        val end = currentBaseState.sessionEndTime ?: return

        val durationMillis = end.toEpochMilli() - start.toEpochMilli()
        val targetMillis = start.toEpochMilli() + (durationMillis * progress).toLong()
        val targetTime = Instant.ofEpochMilli(targetMillis)

        _playbackTime.value = targetTime

        scrubJob?.cancel()
        scrubJob = viewModelScope.launch {
            delay(300) // debounce
            val positions = runCatching { repo.fetchPositionsAt(sessionKey, targetTime) }.getOrDefault(emptyList())
            _playbackPositions.value = positions
        }
    }
}
