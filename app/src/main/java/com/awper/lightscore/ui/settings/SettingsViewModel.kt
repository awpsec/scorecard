package com.awper.lightscore.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.awper.lightscore.TeamInfo
import com.awper.lightscore.background.WorkScheduler
import com.awper.lightscore.fetchTeams
import com.awper.lightscore.settings.SettingsStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoUpdateCellular: Boolean = true,
    val updateBackground: Boolean = true,
    val updateFavoritesBackground: Boolean = false,
    val autoUpdateFavoritesCellular: Boolean = true,
    val keepScreenAwake: Boolean = false,
    val lowDataMode: Boolean = false,
    val pinFavorites: Boolean = true,
    val favoriteTeamIds: List<String> = emptyList(),
    val favoriteTeamAbbrevs: List<String> = emptyList()
)

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var teamsById: Map<String, TeamInfo> = emptyMap()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            teamsById = try { fetchTeams().associateBy { it.id.toString() } } catch (_: Exception) { emptyMap() }
            _uiState.value = _uiState.value.copy(
                favoriteTeamAbbrevs = abbreviationsFor(_uiState.value.favoriteTeamIds)
            )
        }
        viewModelScope.launch {
            combine(
                store.autoUpdateCellular,
                store.updateBackground,
                store.updateFavoritesBackground,
                store.autoUpdateFavoritesCellular,
                store.keepScreenAwake,
                store.lowDataMode,
                store.pinFavorites,
                store.favorites
            ) { values ->
                val autoCell = values[0] as Boolean
                val bg = values[1] as Boolean
                val favBg = values[2] as Boolean
                val autoFavCell = values[3] as Boolean
                val keepAwake = values[4] as Boolean
                val lowData = values[5] as Boolean
                val pinFavorites = values[6] as Boolean
                val favs = values[7] as List<String>
                SettingsUiState(
                    autoUpdateCellular = autoCell,
                    updateBackground = bg,
                    updateFavoritesBackground = favBg,
                    autoUpdateFavoritesCellular = autoFavCell,
                    keepScreenAwake = keepAwake,
                    lowDataMode = lowData,
                    pinFavorites = pinFavorites,
                    favoriteTeamIds = favs,
                    favoriteTeamAbbrevs = abbreviationsFor(favs)
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setAutoUpdateCellular(v: Boolean) {
        _uiState.value = _uiState.value.copy(autoUpdateCellular = v)
        viewModelScope.launch { store.setAutoUpdateCellular(v) }
    }
    fun setUpdateBackground(v: Boolean) {
        _uiState.value = _uiState.value.copy(updateBackground = v)
        viewModelScope.launch {
            store.setUpdateBackground(v)
            syncBackgroundWork()
        }
    }
    fun setUpdateFavoritesBackground(v: Boolean) {
        _uiState.value = _uiState.value.copy(updateFavoritesBackground = v)
        viewModelScope.launch {
            store.setUpdateFavoritesBackground(v)
            syncBackgroundWork()
        }
    }
    fun setAutoUpdateFavoritesCellular(v: Boolean) {
        _uiState.value = _uiState.value.copy(autoUpdateFavoritesCellular = v)
        viewModelScope.launch { store.setAutoUpdateFavoritesCellular(v) }
    }
    fun setKeepScreenAwake(v: Boolean) {
        _uiState.value = _uiState.value.copy(keepScreenAwake = v)
        viewModelScope.launch { store.setKeepScreenAwake(v) }
    }
    fun setLowDataMode(v: Boolean) {
        _uiState.value = _uiState.value.copy(lowDataMode = v)
        viewModelScope.launch { store.setLowDataMode(v) }
    }
    fun setPinFavorites(v: Boolean) {
        _uiState.value = _uiState.value.copy(pinFavorites = v)
        viewModelScope.launch { store.setPinFavorites(v) }
    }

    private fun abbreviationsFor(teamIds: List<String>): List<String> {
        if (teamIds.isNotEmpty() && teamsById.isEmpty()) return listOf("selected")
        return teamIds.map { id ->
            teamsById[id]?.abbreviation?.uppercase() ?: "selected"
        }
    }

    private suspend fun syncBackgroundWork() {
        val state = _uiState.value
        if (state.updateBackground || (state.updateFavoritesBackground && state.favoriteTeamIds.isNotEmpty())) {
            WorkScheduler.enqueue(store.appContext)
        } else {
            WorkScheduler.cancel(store.appContext)
        }
    }
}

class SettingsViewModelFactory(private val store: SettingsStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(store) as T
    }
}
