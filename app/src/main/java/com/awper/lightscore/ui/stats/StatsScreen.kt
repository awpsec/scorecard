package com.awper.lightscore.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awper.lightscore.PlayerGameLine
import com.awper.lightscore.PlayerStatsDetail
import com.awper.lightscore.StandingsTeamLine

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = topInset + 4.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Color.White)
            }
            Text("stats", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SearchRow(
                query = uiState.query,
                onQueryChange = viewModel::setQuery,
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    viewModel.search()
                }
            )
            Spacer(Modifier.height(10.dp))

            if (uiState.isLoading) Text("loading", style = TextStyle(color = Color.White, fontSize = 13.sp))
            if (uiState.error != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("unable to connect", style = TextStyle(color = Color.White, fontSize = 12.sp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "retry",
                        style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp),
                        modifier = Modifier.clickable { viewModel.loadStandings() }
                    )
                }
            }

            uiState.results.forEach { player ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        viewModel.selectPlayer(player.id)
                    }.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(player.name.lowercase(), style = TextStyle(color = Color.White, fontSize = 13.sp))
                    Text(player.position.lowercase(), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
                }
            }

            uiState.player?.let {
                PlayerStats(it)
                Spacer(Modifier.height(16.dp))
            }

            Text("standings", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 13.sp))
            Spacer(Modifier.height(6.dp))
            Standings(uiState.standings)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SearchRow(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (query.isBlank()) Text("search player", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 15.sp))
                    inner()
                }
            }
        )
        Box(Modifier.size(44.dp).clickable { onSearch() }, contentAlignment = Alignment.Center) {
            SearchIcon()
        }
    }
    ThinDivider()
}

@Composable
private fun SearchIcon() {
    Canvas(Modifier.size(18.dp)) {
        drawCircle(Color.White, radius = size.minDimension * 0.32f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(2f))
        drawLine(Color.White, Offset(size.width * 0.62f, size.height * 0.62f), Offset(size.width * 0.88f, size.height * 0.88f), strokeWidth = 2f)
    }
}

@Composable
private fun PlayerStats(player: PlayerStatsDetail) {
    val teamSuffix = if (player.team.isNotBlank()) " - ${player.team}" else ""
    Text("${player.name}$teamSuffix", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
    if (player.position.isNotBlank()) Text(player.position.lowercase(), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
    Spacer(Modifier.height(8.dp))
    Text("season", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
    StatGrid(player.seasonStats)
    Spacer(Modifier.height(10.dp))
    Text("last 10", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
    GameLogHeader(player.isPitcher)
    player.lastTen.forEach { GameLogRow(it, player.isPitcher) }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    stats.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { (label, value) ->
                Text("$label $value", modifier = Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun GameLogHeader(isPitcher: Boolean) {
    val labels = if (isPitcher) listOf("ip", "h", "r", "er", "bb", "k", "hr") else listOf("ab", "h", "r", "rbi", "hr")
    Row(Modifier.fillMaxWidth()) {
        Text("", Modifier.width(126.dp))
        labels.forEach { label ->
            Text(label, Modifier.weight(1f), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
        }
    }
}

@Composable
private fun GameLogRow(line: PlayerGameLine, isPitcher: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${line.date.shortDate()} ${line.matchup}", Modifier.width(126.dp), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 9.sp))
        val expected = if (isPitcher) 7 else 5
        line.values.take(expected).forEach { value ->
            Text(value, Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 10.sp))
        }
    }
}

private fun String.shortDate(): String {
    val parts = split("-")
    val month = parts.getOrNull(0)?.toIntOrNull()?.toString() ?: return this
    val day = parts.getOrNull(1)?.toIntOrNull()?.toString() ?: return this
    return "$month/$day"
}

@Composable
private fun Standings(lines: List<StandingsTeamLine>) {
    val divisionOrder = listOf("al east", "al central", "al west", "nl east", "nl central", "nl west")
    val grouped = lines.groupBy { it.division }
    divisionOrder.forEach { division ->
        val teams = grouped[division].orEmpty()
        if (teams.isEmpty()) return@forEach
        Text(division, style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        teams.sortedWith(compareBy<StandingsTeamLine> { it.divisionRank }.thenByDescending { it.wins }).forEach { team ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(team.teamName.take(18), Modifier.weight(1f), style = TextStyle(color = Color.White, fontSize = 11.sp))
                Text("${team.wins}-${team.losses}", Modifier.width(48.dp), style = TextStyle(color = Color.White, fontSize = 11.sp))
                Text(team.gamesBack, Modifier.width(34.dp), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
                Text(team.streak, Modifier.width(28.dp), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White))
}
