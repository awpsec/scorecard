package com.awper.lightscore.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awper.lightscore.Game
import com.awper.lightscore.fetchSchedule
import com.awper.lightscore.mlbSlateDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val liveGames: List<Game> = emptyList(),
    val nextGames: List<Game> = emptyList(),
    val finalGames: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var requestVersion = 0

    init { loadSchedule() }

    fun loadSchedule(showLoading: Boolean = true, showRefreshing: Boolean = false) {
        if (loadJob?.isActive == true && !showLoading && !showRefreshing) return
        val version = ++requestVersion
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = showLoading,
                isRefreshing = showRefreshing,
                error = null
            )
            try {
                val games = fetchSchedule(mlbSlateDate()).dates.flatMap { it.games }
                if (showRefreshing) delay(700)
                if (version != requestVersion) return@launch
                _uiState.value = HomeUiState(
                    liveGames = games.filter { it.status.abstractGameState == "Live" },
                    nextGames = games.filter { it.status.abstractGameState == "Preview" },
                    finalGames = games.filter { it.status.abstractGameState == "Final" },
                    isLoading = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                if (showRefreshing) delay(700)
                if (version != requestVersion) return@launch
                _uiState.value = HomeUiState(
                    isLoading = false,
                    isRefreshing = false,
                    error = "unable to connect"
                )
            }
        }
    }
}
