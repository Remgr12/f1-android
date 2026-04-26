package dev.remgr.f1.feature.liveracehub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.liveracehub.domain.LiveSessionRepository
import dev.remgr.f1.feature.liveracehub.domain.RaceControlMessage
import dev.remgr.f1.feature.liveracehub.domain.model.LiveDriver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LiveRaceHubViewModel @Inject constructor(
    repo: LiveSessionRepository,
) : ViewModel() {

    val liveStandings: StateFlow<List<LiveDriver>> = repo
        .getLiveStandings()
        .catch { /* swallow — show stale data */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val raceControlMessages: StateFlow<List<RaceControlMessage>> = repo
        .getRaceControlMessages()
        .catch { }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
