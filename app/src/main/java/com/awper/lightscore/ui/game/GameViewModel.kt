package com.awper.lightscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awper.lightscore.GameFeed
import com.awper.lightscore.SeasonBattingLine
import com.awper.lightscore.SeasonPitchingLine
import com.awper.lightscore.fetchBoxscore
import com.awper.lightscore.fetchGameFeed
import com.awper.lightscore.fetchSeasonBattingStats
import com.awper.lightscore.fetchSeasonPitchingStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val gameFeed: GameFeed? = null,
    val seasonBattingByPlayerId: Map<Int, SeasonBattingLine> = emptyMap(),
    val seasonPitchingByPlayerId: Map<Int, SeasonPitchingLine> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var requestVersion = 0
    private var loadJob: Job? = null

    fun loadGame(
        gamePk: Int,
        showLoading: Boolean = true,
        showRefreshing: Boolean = false,
        refreshSeasonStats: Boolean = showLoading
    ) {
        if (loadJob?.isActive == true && !showLoading && !showRefreshing) return
        val version = ++requestVersion
        loadJob = viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(
                isLoading = showLoading,
                isRefreshing = showRefreshing,
                error = null
            )
            try {
                val feed = fetchGameFeed(gamePk)
                val boxscore = try { fetchBoxscore(gamePk) } catch (_: Exception) { null }
                val mergedFeed = feed.copy(liveData = feed.liveData.copy(boxscore = boxscore))

                val battingIds = listOfNotNull(
                    boxscore?.teams?.away?.batters ?: boxscore?.teams?.away?.battingOrder,
                    boxscore?.teams?.home?.batters ?: boxscore?.teams?.home?.battingOrder
                ).flatten().distinct()

                val pitcherIds = listOfNotNull(
                    feed.gameData.probablePitchers?.away?.id,
                    feed.gameData.probablePitchers?.home?.id
                ).distinct()

                val seasonBatting = if (refreshSeasonStats || previous.seasonBattingByPlayerId.isEmpty()) {
                    try { fetchSeasonBattingStats(battingIds) } catch (_: Exception) { previous.seasonBattingByPlayerId }
                } else {
                    previous.seasonBattingByPlayerId
                }
                val seasonPitching = if (refreshSeasonStats || previous.seasonPitchingByPlayerId.isEmpty()) {
                    try { fetchSeasonPitchingStats(pitcherIds) } catch (_: Exception) { previous.seasonPitchingByPlayerId }
                } else {
                    previous.seasonPitchingByPlayerId
                }

                if (showRefreshing) delay(700)
                if (version != requestVersion) return@launch

                _uiState.value = GameUiState(
                    gameFeed = mergedFeed,
                    seasonBattingByPlayerId = seasonBatting,
                    seasonPitchingByPlayerId = seasonPitching,
                    isLoading = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                if (showRefreshing) delay(700)
                if (version != requestVersion) return@launch
                _uiState.value = previous.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "unable to connect"
                )
            }
        }
    }
}
