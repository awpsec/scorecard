package com.awper.lightscore.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awper.lightscore.Game
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    lowDataMode: Boolean = false,
    favoriteTeamIds: Set<String> = emptySet(),
    pinFavorites: Boolean = true,
    onNavigateToGame: (Int) -> Unit,
    onSwipeToStats: () -> Unit,
    onSwipeToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allGames = remember(uiState.liveGames, uiState.nextGames, uiState.finalGames, favoriteTeamIds, pinFavorites) {
        val games = uiState.liveGames + uiState.nextGames + uiState.finalGames
        if (pinFavorites) games.sortedByDescending { it.isFavoriteGame(favoriteTeamIds) } else games
    }
    val gamesPerPage = 6
    val pages = remember(allGames) { allGames.chunked(gamesPerPage) }
    val pageCount = pages.size.coerceAtLeast(1)
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var showRefreshCue by remember { mutableStateOf(false) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) currentPage = pageCount - 1
    }

    LaunchedEffect(showRefreshCue) {
        if (showRefreshCue) {
            delay(1_500)
            showRefreshCue = false
        }
    }

    LaunchedEffect(lowDataMode) {
        val interval = if (lowDataMode) 60_000L else 30_000L
        while (isActive) {
            delay(interval)
            viewModel.loadSchedule(showLoading = false)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = topInset + 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("scorecard", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                if (pageCount > 1) {
                    Text("${currentPage + 1}/$pageCount", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 14.sp))
                }
            }

            if (uiState.isLoading) {
                Text("loading", style = TextStyle(color = Color.White, fontSize = 16.sp), modifier = Modifier.padding(16.dp))
            } else if (uiState.error != null) {
                Column(Modifier.padding(16.dp)) {
                    Text("unable to connect", style = TextStyle(color = Color.White, fontSize = 14.sp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "retry",
                        style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 14.sp),
                        modifier = Modifier.clickable { viewModel.loadSchedule() }
                    )
                }
            } else if (allGames.isEmpty()) {
                Text("no games today", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 16.sp), modifier = Modifier.padding(16.dp))
            } else {
                val pageGames = pages.getOrNull(currentPage) ?: emptyList()
                val swipeThreshold = 80f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentPage, pageCount) {
                            var totalX = 0f
                            var totalY = 0f
                            detectDragGestures(
                                onDragStart = {
                                    totalX = 0f
                                    totalY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    totalX += dragAmount.x
                                    totalY += dragAmount.y
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (abs(totalY) > abs(totalX) && abs(totalY) > swipeThreshold) {
                                        if (totalY > 0f) {
                                            if (currentPage == 0) {
                                                showRefreshCue = true
                                                viewModel.loadSchedule(showLoading = false, showRefreshing = true)
                                            }
                                            else currentPage -= 1
                                        }
                                        if (totalY < 0f && currentPage < pageCount - 1) currentPage += 1
                                    } else if (abs(totalX) > abs(totalY) && totalX < -swipeThreshold) {
                                        onSwipeToSettings()
                                    } else if (abs(totalX) > abs(totalY) && totalX > swipeThreshold) {
                                        onSwipeToStats()
                                    }
                                }
                            )
                        }
                ) {
                    PageContent(pageGames, favoriteTeamIds, onNavigateToGame)
                }
            }
        }

        if (uiState.isRefreshing || showRefreshCue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset + 44.dp)
            ) {
                RefreshEllipsis()
            }
        }
    }
}

@Composable
private fun RefreshEllipsis() {
    var dots by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            dots = if (dots == 3) 1 else dots + 1
        }
    }
    Text(".".repeat(dots), style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold))
}

@Composable
private fun PageContent(
    games: List<Game>,
    favoriteTeamIds: Set<String>,
    onGameClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(2.dp))

        games.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { game ->
                    GameCard(game, favoriteTeamIds, { onGameClick(game.gamePk) }, Modifier.weight(1f))
                }
                if (row.size < 2) {
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: Game, favoriteTeamIds: Set<String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val awayTeam = game.teams.away
    val homeTeam = game.teams.home
    val ls = game.linescore
    val isLive = game.status.abstractGameState == "Live" && ls?.currentInning != null
    val isFinal = game.status.abstractGameState == "Final"
    val awayAbbrev = awayTeam.team?.abbreviation?.uppercase() ?: ""
    val homeAbbrev = homeTeam.team?.abbreviation?.uppercase() ?: ""
    val awayFavorite = awayTeam.team?.id?.toString() in favoriteTeamIds
    val homeFavorite = homeTeam.team?.id?.toString() in favoriteTeamIds
    val awayRuns = ls?.teams?.away?.runs ?: 0
    val homeRuns = ls?.teams?.home?.runs ?: 0

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .heightIn(min = 78.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (isLive) {
                val count = "${ls?.balls ?: 0}-${ls?.strikes ?: 0}"
                val outs = "${ls?.outs ?: 0} out"
                val monoStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                val monoDimStyle = TextStyle(color = Color(0xFFAAAAAA), fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TeamScore(awayAbbrev, awayRuns, awayFavorite)
                    HomeInningText(ls?.inningHalf == "Top", ls?.currentInning ?: 0)
                    Text(count, style = monoStyle)
                }
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TeamScore(homeAbbrev, homeRuns, homeFavorite)
                    Text(outs, style = monoDimStyle)
                }
            } else if (isFinal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamAbbrev(awayAbbrev, awayFavorite, awayRuns > homeRuns)
                    Spacer(Modifier.width(4.dp))
                    Text("@", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    TeamAbbrev(homeAbbrev, homeFavorite, homeRuns > awayRuns)
                }
                Spacer(Modifier.height(6.dp))
                Text("$awayRuns - $homeRuns  [f]", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamAbbrev(awayAbbrev, awayFavorite)
                    Spacer(Modifier.width(4.dp))
                    Text("@", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    TeamAbbrev(homeAbbrev, homeFavorite)
                }
                Spacer(Modifier.height(6.dp))
                Text(game.gameDate?.let { formatGameTime(it) } ?: "", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun HomeInningText(isTop: Boolean, inning: Int) {
    Text(
        buildAnnotatedString {
            pushStyle(SpanStyle(fontSize = 8.sp))
            append(if (isTop) "▲" else "▼")
            pop()
            append(inning.toString())
        },
        style = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    )
}

@Composable
private fun TeamScore(abbrev: String, runs: Int, isFavorite: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text("$abbrev $runs", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace))
        FavoriteStar(isFavorite)
    }
}

@Composable
private fun TeamAbbrev(abbrev: String, isFavorite: Boolean, underline: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            abbrev,
            style = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                textDecoration = if (underline) TextDecoration.Underline else null
            )
        )
        FavoriteStar(isFavorite)
    }
}

@Composable
private fun FavoriteStar(isFavorite: Boolean) {
    if (isFavorite) {
        Text("*", modifier = Modifier.offset(y = (-5).dp), style = TextStyle(color = Color.White, fontSize = 9.sp))
    }
}

private fun Game.isFavoriteGame(favoriteTeamIds: Set<String>): Boolean {
    val awayId = teams.away.team?.id?.toString()
    val homeId = teams.home.team?.id?.toString()
    return awayId in favoriteTeamIds || homeId in favoriteTeamIds
}

private fun formatGameTime(iso: String): String {
    return try {
        val odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        odt.atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            .lowercase()
    } catch (_: Exception) { "" }
}
