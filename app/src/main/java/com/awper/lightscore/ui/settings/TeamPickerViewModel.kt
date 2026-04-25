package com.awper.lightscore.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.awper.lightscore.TeamInfo
import com.awper.lightscore.fetchTeams
import com.awper.lightscore.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TeamPickerUiState(
    val allTeams: List<TeamInfo> = emptyList(),
    val selectedTeamIds: Set<String> = emptySet()
)

class TeamPickerViewModel(private val store: SettingsStore) : ViewModel() {
    private val _uiState = MutableStateFlow(TeamPickerUiState())
    val uiState: StateFlow<TeamPickerUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val teams = try { fetchTeams() } catch (_: Exception) { emptyList() }
            val favs = store.favorites.first()
            _uiState.value = TeamPickerUiState(allTeams = teams, selectedTeamIds = favs.toSet())
        }
    }

    fun toggleTeam(teamId: String) {
        val newSet = if (_uiState.value.selectedTeamIds.contains(teamId)) {
            _uiState.value.selectedTeamIds - teamId
        } else {
            _uiState.value.selectedTeamIds + teamId
        }
        _uiState.value = _uiState.value.copy(selectedTeamIds = newSet)
        viewModelScope.launch { store.setFavorites(newSet.toList()) }
    }
}

class TeamPickerViewModelFactory(private val store: SettingsStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TeamPickerViewModel(store) as T
    }
}
