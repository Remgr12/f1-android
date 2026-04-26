package dev.remgr.f1.feature.racedetail.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.remgr.f1.feature.racedetail.domain.RaceDetailRepository
import dev.remgr.f1.feature.racedetail.domain.model.RaceDetailModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RaceDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: RaceDetailRepository,
) : ViewModel() {

    private val meetingKey = checkNotNull(savedState.get<Int>("meetingKey"))
    private val sessionKey = checkNotNull(savedState.get<Int>("sessionKey"))

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val detail: RaceDetailModel) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    var selectedTab by mutableIntStateOf(0)
        private set

    init { load() }

    fun selectTab(tab: Int) { selectedTab = tab }
    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { repository.getRaceDetail(meetingKey, sessionKey) }
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Error") }
        }
    }
}
