package com.awper.lightscore.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awper.lightscore.PlayerSearchResult
import com.awper.lightscore.PlayerStatsDetail
import com.awper.lightscore.StandingsTeamLine
import com.awper.lightscore.fetchPlayerStatsDetail
import com.awper.lightscore.fetchStandings
import com.awper.lightscore.searchPlayers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class StatsUiState(
    val standings: List<StandingsTeamLine> = emptyList(),
    val query: String = "",
    val results: List<PlayerSearchResult> = emptyList(),
    val player: PlayerStatsDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class StatsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var searchVersion = 0
    private var playerVersion = 0

    init { loadStandings() }

    fun loadStandings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                _uiState.value = _uiState.value.copy(standings = fetchStandings(), isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "unable to connect")
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
        searchJob?.cancel()
        if (value.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            search(showLoading = false)
        }
    }

    fun search(showLoading: Boolean = true) {
        val query = _uiState.value.query
        if (query.length < 2) return
        searchJob?.cancel()
        val version = ++searchVersion
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = showLoading, error = null, player = null)
            try {
                val results = searchPlayers(query)
                if (version == searchVersion) {
                    _uiState.value = _uiState.value.copy(results = results, isLoading = false)
                }
            } catch (e: Exception) {
                if (version == searchVersion) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "unable to connect")
                }
            }
        }
    }

    fun selectPlayer(playerId: Int) {
        val version = ++playerVersion
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val player = fetchPlayerStatsDetail(playerId)
                if (version == playerVersion) {
                    _uiState.value = _uiState.value.copy(player = player, results = emptyList(), isLoading = false)
                }
            } catch (e: Exception) {
                if (version == playerVersion) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "unable to connect")
                }
            }
        }
    }
}
