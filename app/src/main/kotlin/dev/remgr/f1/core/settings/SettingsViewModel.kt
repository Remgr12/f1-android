package dev.remgr.f1.core.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _apiUrl = MutableStateFlow(repository.getApiBaseUrl())
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _apiKey = MutableStateFlow(repository.getApiKey() ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    val themeMode: StateFlow<SettingsRepository.ThemeMode> = repository.themeModeFlow

    fun updateApiUrl(url: String) {
        _apiUrl.value = url
        repository.setApiBaseUrl(url)
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        repository.setApiKey(key)
    }

    fun updateThemeMode(mode: SettingsRepository.ThemeMode) {
        repository.setThemeMode(mode)
    }
}
