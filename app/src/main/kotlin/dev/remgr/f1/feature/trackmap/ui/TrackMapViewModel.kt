package dev.remgr.f1.feature.trackmap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.trackmap.domain.TrackMapRepository
import dev.remgr.f1.feature.trackmap.domain.TrackMapState
import dev.remgr.f1.feature.trackmap.domain.TrackSessionOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TrackMapViewModel @Inject constructor(
    private val repo: TrackMapRepository,
) : ViewModel() {

    private val _trackOptions = MutableStateFlow(
        listOf(TrackSessionOption(sessionKey = null, label = "Latest session (Live)")),
    )
    val trackOptions: StateFlow<List<TrackSessionOption>> = _trackOptions.asStateFlow()

    private val _selectedSessionKey = MutableStateFlow<Int?>(null)
    val selectedSessionKey: StateFlow<Int?> = _selectedSessionKey.asStateFlow()

    val state: StateFlow<TrackMapState> = _selectedSessionKey
        .flatMapLatest { selected -> repo.getTrackMapState(selected) }
        .catch { /* log */ }
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrackMapState(emptyList(), emptyList()),
        )

    init {
        viewModelScope.launch {
            val options = runCatching { repo.getTrackOptions() }.getOrDefault(emptyList())
            if (options.isNotEmpty()) {
                _trackOptions.value = options
            }
        }
    }

    fun selectSession(sessionKey: Int?) {
        _selectedSessionKey.value = sessionKey
    }
}
