package com.awper.lightscore.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset as GeometryOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Paint
import com.awper.lightscore.BattingStats
import com.awper.lightscore.BoxscoreTeam
import com.awper.lightscore.BoxscorePlayer
import com.awper.lightscore.GameFeed
import com.awper.lightscore.Inning
import com.awper.lightscore.PitchingStats
import com.awper.lightscore.PlayEvent
import com.awper.lightscore.Person
import com.awper.lightscore.SeasonBattingLine
import com.awper.lightscore.SeasonPitchingLine
import com.awper.lightscore.ui.home.components.CountText
import com.awper.lightscore.TeamStats
import com.awper.lightscore.ui.home.components.BasesDiamond
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@Composable
fun GameScreen(
    gamePk: Int,
    viewModel: GameViewModel = viewModel(),
    lowDataMode: Boolean = false,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var showRefreshCue by remember { mutableStateOf(false) }
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    LaunchedEffect(gamePk) { viewModel.loadGame(gamePk) }

    LaunchedEffect(showRefreshCue) {
        if (showRefreshCue) {
            delay(1_500)
            showRefreshCue = false
        }
    }

    val pullRefreshConnection = remember(gamePk, scrollState.value) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: GeometryOffset, source: NestedScrollSource): GeometryOffset {
                if (source == NestedScrollSource.Drag && scrollState.value == 0 && available.y > 0f) {
                    pullDistance += available.y
                    if (pullDistance > 80f && !showRefreshCue && !uiState.isRefreshing) {
                        showRefreshCue = true
                        viewModel.loadGame(
                            gamePk,
                            showLoading = false,
                            showRefreshing = true,
                            refreshSeasonStats = false
                        )
                    }
                } else if (available.y < 0f) {
                    pullDistance = 0f
                }
                return GeometryOffset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                pullDistance = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    LaunchedEffect(gamePk, uiState.gameFeed?.gameData?.status?.abstractGameState, lowDataMode) {
        val interval = if (lowDataMode) 10_000L else 2_500L
        while (isActive && uiState.gameFeed?.gameData?.status?.abstractGameState == "Live") {
            delay(interval)
            viewModel.loadGame(gamePk, showLoading = false, refreshSeasonStats = false)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshConnection)
                .pointerInput(gamePk) {
                    var totalX = 0f
                    var totalY = 0f
                    detectDragGestures(
                        onDragStart = {
                            totalX = 0f
                            totalY = 0f
                        },
                        onDrag = { _, dragAmount ->
                            totalX += dragAmount.x
                            totalY += dragAmount.y
                        },
                        onDragEnd = {
                            if (totalY > 80f && abs(totalY) > abs(totalX)) {
                                showRefreshCue = true
                                viewModel.loadGame(
                                    gamePk,
                                    showLoading = false,
                                    showRefreshing = true,
                                    refreshSeasonStats = false
                                )
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (uiState.isRefreshing || showRefreshCue) 26.dp else 0.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (uiState.isRefreshing || showRefreshCue) RefreshEllipsis()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = topInset + 4.dp, bottom = 8.dp)
            ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "back", tint = Color.White)
            }
            val gf = uiState.gameFeed
            if (gf != null) {
                val away = gf.gameData.teams.away.abbreviation
                val home = gf.gameData.teams.home.abbreviation
                Text("$away @ $home", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("loading", style = TextStyle(color = Color.White, fontSize = 14.sp))
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("unable to connect", style = TextStyle(color = Color.White, fontSize = 14.sp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "retry",
                        style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 14.sp),
                        modifier = Modifier.clickable { viewModel.loadGame(gamePk) }
                    )
                }
            }
            uiState.gameFeed != null -> {
                val gf = uiState.gameFeed!!
                val status = gf.gameData.status.abstractGameState ?: "Preview"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    when (status) {
                        "Live" -> LiveView(gf)
                        "Preview" -> PregameView(gf, uiState.seasonBattingByPlayerId, uiState.seasonPitchingByPlayerId)
                        "Final" -> FinalView(gf)
                        else -> PregameView(gf, uiState.seasonBattingByPlayerId, uiState.seasonPitchingByPlayerId)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
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
private fun LiveView(gameFeed: GameFeed) {
    val linescore = gameFeed.liveData.linescore
    val plays = gameFeed.liveData.plays
    val innings = linescore?.innings ?: emptyList()
    val pitchEvents = plays?.currentPlay?.playEvents?.filter { it.isPitch } ?: emptyList()
    val eventLine = latestEventLine(plays?.currentPlay?.result?.description, pitchEvents)
    val boxscore = gameFeed.liveData.boxscore

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        InningBoxScore(
            awayAbbr = gameFeed.gameData.teams.away.abbreviation,
            homeAbbr = gameFeed.gameData.teams.home.abbreviation,
            innings = innings,
            awayTotals = linescore?.teams?.away,
            homeTotals = linescore?.teams?.home
        )

        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            val offense = linescore?.offense

            Column(
                modifier = Modifier.width(92.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutsDots(linescore?.outs ?: 0)
                    CountText(linescore?.balls ?: 0, linescore?.strikes ?: 0)
                }

                Spacer(Modifier.height(10.dp))

                BasesDiamondWithInning(
                    inning = linescore?.currentInning ?: 0,
                    isTopInning = linescore?.inningHalf == "Top",
                    runnerFirst = offense?.first.hasRunner(),
                    runnerSecond = offense?.second.hasRunner(),
                    runnerThird = offense?.third.hasRunner(),
                    modifier = Modifier.size(66.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                StrikeZoneFull(Modifier.size(104.dp, 146.dp), pitchEvents)
            }

            Spacer(Modifier.width(8.dp))

            PitchLogCompact(pitchEvents, modifier = Modifier.width(80.dp))
        }

        Spacer(Modifier.height(10.dp))

        Column(Modifier.fillMaxWidth()) {
            val pitcher = linescore?.defense?.pitcher
            if (pitcher != null) {
                val pitchCount = boxscore?.teams?.home?.players?.values
                    ?.find { it.person?.id == pitcher.id }
                    ?.stats?.pitching?.numberOfPitches
                    ?: boxscore?.teams?.away?.players?.values
                        ?.find { it.person?.id == pitcher.id }
                        ?.stats?.pitching?.numberOfPitches
                Text("p: ${pitcher.fullName.lowercase()}${if (pitchCount != null) " ($pitchCount)" else ""}", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
            }
            val batter = linescore?.offense?.batter
            val batterId = linescore?.offense?.batter?.id
            val batterLine = batterId?.let {
                val player = boxscore?.teams?.away?.players?.get("ID$it") ?: boxscore?.teams?.home?.players?.get("ID$it")
                val h = player?.stats?.batting?.hits ?: 0
                val ab = player?.stats?.batting?.atBats ?: 0
                "$h-$ab"
            } ?: "0-0"
            if (batter != null) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("b: ${batter.fullName.lowercase()}", style = TextStyle(color = Color.White, fontSize = 12.sp))
                    Text(batterLine, style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.BottomStart) {
            if (eventLine.isNotBlank()) {
                Text(
                    eventLine.lowercase(),
                    style = TextStyle(color = Color.White, fontSize = 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        ThinDivider()
        Spacer(Modifier.height(12.dp))

        val homeBox = boxscore?.teams?.home
        val awayBox = boxscore?.teams?.away

        Text("batting stats", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(8.dp))
        renderLineup(gameFeed.gameData.teams.away.abbreviation, awayBox)
        Spacer(Modifier.height(12.dp))
        renderLineup(gameFeed.gameData.teams.home.abbreviation, homeBox)

        Spacer(Modifier.height(12.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Text("pitching stats", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(6.dp))
        renderPitchingStats(gameFeed.gameData.teams.away.abbreviation, awayBox)
        Spacer(Modifier.height(10.dp))
        renderPitchingStats(gameFeed.gameData.teams.home.abbreviation, homeBox)
    }
}

private fun latestEventLine(playDescription: String?, pitchEvents: List<PlayEvent>): String {
    val pitchCall = pitchEvents.lastOrNull()?.details?.call?.description
    return playDescription?.takeIf { it.isNotBlank() } ?: pitchCall.orEmpty()
}

private fun Person?.hasRunner(): Boolean = this != null && id > 0 && fullName.isNotBlank()

@Composable
private fun renderLineup(teamAbbr: String, box: BoxscoreTeam?) {
    Text(teamAbbr.lowercase(), style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    StatsHeader(
        listOf("name", "ab", "r", "h", "hr", "rbi", "bb", "so"),
        listOf(0.39f, 0.08f, 0.08f, 0.08f, 0.08f, 0.10f, 0.08f, 0.08f)
    )
    Spacer(Modifier.height(4.dp))
    val batters = battingPlayersInOrder(box)
    if (batters.isNotEmpty()) {
        batters.forEach { player ->
            val name = displayPlayerName(player)
            val bs = player.stats?.batting
            if (bs != null) {
                StatsRow(
                    listOf(
                        name,
                        bs.atBats?.toString() ?: "0",
                        bs.runs?.toString() ?: "0",
                        bs.hits?.toString() ?: "0",
                        bs.homeRuns?.toString() ?: "0",
                        bs.rbi?.toString() ?: "0",
                        bs.baseOnBalls?.toString() ?: "0",
                        bs.strikeOuts?.toString() ?: "0"
                    ),
                    listOf(0.39f, 0.08f, 0.08f, 0.08f, 0.08f, 0.10f, 0.08f, 0.08f)
                )
            }
        }
    } else {
        Text("lineups not yet posted", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
    }
}

@Composable
private fun renderPitchingStats(teamAbbr: String, box: BoxscoreTeam?) {
    Text(teamAbbr.lowercase(), style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    StatsHeader(
        listOf("name", "ip", "h", "r", "er", "bb", "so"),
        listOf(0.39f, 0.10f, 0.08f, 0.08f, 0.08f, 0.08f, 0.08f)
    )
    Spacer(Modifier.height(4.dp))
    if (box?.pitchers != null && box.pitchers.isNotEmpty()) {
        box.pitchers.forEach { playerId ->
            val player = box.players?.get("ID$playerId")
            if (player != null) {
                val name = displayPlayerName(player)
                val ps = player.stats?.pitching
                if (ps != null) {
                    StatsRow(
                        listOf(
                            name,
                            ps.inningsPitched ?: "0.0",
                            ps.hits?.toString() ?: "0",
                            ps.runs?.toString() ?: "0",
                            ps.earnedRuns?.toString() ?: "0",
                            ps.baseOnBalls?.toString() ?: "0",
                            ps.strikeOuts?.toString() ?: "0"
                        ),
                        listOf(0.39f, 0.10f, 0.08f, 0.08f, 0.08f, 0.08f, 0.08f)
                    )
                }
            }
        }
    } else {
        Text("no pitching data", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
    }
}

@Composable
private fun BasesDiamondWithInning(
    inning: Int,
    isTopInning: Boolean,
    runnerFirst: Boolean,
    runnerSecond: Boolean,
    runnerThird: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = minOf(w, h) * 0.1f
            val bs = minOf(w, h) * 0.16f
            val topY = h * 0.30f
            val sideY = h * 0.62f
            val homeY = h * 1.10f
            val homeX = w / 2

            val halfSize = bs / 2f
            if (runnerFirst) drawRect(Color.White, Offset(w - pad - halfSize, sideY - halfSize), Size(bs, bs)) else drawRect(Color.White, Offset(w - pad - halfSize, sideY - halfSize), Size(bs, bs), style = Stroke(1.5f))
            if (runnerSecond) drawRect(Color.White, Offset(w / 2 - halfSize, topY - halfSize), Size(bs, bs)) else drawRect(Color.White, Offset(w / 2 - halfSize, topY - halfSize), Size(bs, bs), style = Stroke(1.5f))
            if (runnerThird) drawRect(Color.White, Offset(pad - halfSize, sideY - halfSize), Size(bs, bs)) else drawRect(Color.White, Offset(pad - halfSize, sideY - halfSize), Size(bs, bs), style = Stroke(1.5f))

            val numberPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = bs * 1.65f
                isFakeBoldText = true
            }
            val inningText = inning.toString()
            val arrowWidth = bs * 0.95f
            val inningWidth = numberPaint.measureText(inningText)
            val gap = bs * 0.18f
            val totalWidth = arrowWidth + gap + inningWidth
            val baseline = homeY + numberPaint.textSize * 0.16f
            val arrowX = homeX - totalWidth / 2f + arrowWidth / 2f
            val inningX = homeX - totalWidth / 2f + arrowWidth + gap + inningWidth / 2f
            val arrowHeight = bs * 0.75f
            val arrowTop = baseline - arrowHeight * 0.75f
            val arrowBottom = baseline - arrowHeight * 0.05f
            val arrowPath = Path().apply {
                if (isTopInning) {
                    moveTo(arrowX, arrowTop)
                    lineTo(arrowX - arrowWidth / 2f, arrowBottom)
                    lineTo(arrowX + arrowWidth / 2f, arrowBottom)
                } else {
                    moveTo(arrowX - arrowWidth / 2f, arrowTop)
                    lineTo(arrowX + arrowWidth / 2f, arrowTop)
                    lineTo(arrowX, arrowBottom)
                }
                close()
            }
            drawPath(arrowPath, Color.White)
            drawContext.canvas.nativeCanvas.drawText(inningText, inningX, baseline, numberPaint)
        }
    }
}

@Composable
private fun StatsRow(values: List<String>, widths: List<Float>) {
    Row(Modifier.fillMaxWidth()) {
        values.forEachIndexed { index, value ->
            Box(Modifier.weight(widths[index]), contentAlignment = if (index == 0) Alignment.CenterStart else Alignment.Center) {
                Text(value, style = TextStyle(color = Color.White, fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun StatsHeader(labels: List<String>, widths: List<Float>) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Box(Modifier.weight(widths[index]), contentAlignment = if (index == 0) Alignment.CenterStart else Alignment.Center) {
                Text(label, style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
            }
        }
    }
}

@Composable
private fun InningBoxScore(
    awayAbbr: String,
    homeAbbr: String,
    innings: List<Inning>,
    awayTotals: TeamStats?,
    homeTotals: TeamStats?
) {
    val maxDisplay = 9
    val startInning = when {
        innings.isEmpty() -> 1
        innings.size <= maxDisplay -> 1
        else -> innings.size - maxDisplay + 1
    }
    val endInning = startInning + maxDisplay - 1
    val display = (startInning..endInning).mapNotNull { inning -> innings.firstOrNull { it.num == inning } ?: Inning(num = inning) }

    Column {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(36.dp))
            display.forEach { inn ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(inn.num.toString(), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
                }
            }
            Box(Modifier.width(16.dp))
            listOf("r", "h", "e").forEach { label ->
                Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp))
                }
            }
        }
        ThinDivider()

        listOf(awayAbbr to awayTotals, homeAbbr to homeTotals).forEach { pair ->
            val abbr = pair.first
            val totals = pair.second
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    abbr,
                    Modifier.width(36.dp),
                    style = TextStyle(color = Color.White, fontSize = 11.sp)
                )
                display.forEach { inn ->
                    val runs = if (abbr == awayAbbr) inn.away?.runs else inn.home?.runs
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(runs?.toString() ?: "", style = TextStyle(color = Color.White, fontSize = 10.sp))
                    }
                }
                Box(Modifier.width(16.dp))
                listOf(totals?.runs, totals?.hits, totals?.errors).forEach { v ->
                    Box(Modifier.width(18.dp), contentAlignment = Alignment.Center) {
                        Text(v?.toString() ?: "0", style = TextStyle(color = Color.White, fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StrikeZoneFull(modifier: Modifier, pitchEvents: List<PlayEvent>) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 2f
            val w = size.width - 2 * pad
            val h = size.height - 2 * pad

            val outerW = w
            val outerH = h
            val outerLeft = pad
            val outerTop = pad

            val szW = outerW * 0.54f
            val szH = outerH * 0.46f
            val szLeft = outerLeft + (outerW - szW) / 2
            val szTop = outerTop + (outerH - szH) / 2
            val szRight = szLeft + szW
            val szBot = szTop + szH

            drawRect(Color.White, Offset(outerLeft, outerTop), Size(outerW, outerH), style = Stroke(1f))
            drawRect(Color.White, Offset(szLeft, szTop), Size(szW, szH), style = Stroke(1f))

            for (i in 1..2) {
                drawLine(Color(0xFFAAAAAA), Offset(szLeft + i * szW / 3, szTop), Offset(szLeft + i * szW / 3, szBot), 0.5f)
                drawLine(Color(0xFFAAAAAA), Offset(szLeft, szTop + i * szH / 3), Offset(szRight, szTop + i * szH / 3), 0.5f)
            }

            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                textSize = if (size.minDimension < 140f) 20f else 22f
            }

            pitchEvents.forEachIndexed { idx, event ->
                val pd = event.pitchData ?: return@forEachIndexed
                val c = pd.coordinates ?: return@forEachIndexed
                val px = c.pX?.toFloat() ?: return@forEachIndexed
                val pz = c.pZ?.toFloat() ?: return@forEachIndexed

                val cx = outerLeft + (px + 1.5f) / 3f * outerW
                val cy = outerTop + (5f - pz) / 5f * outerH

                drawCircle(Color.White, 14f, Offset(cx, cy))

                val pitchNumber = (idx + 1).toString()
                textPaint.textSize = if (pitchNumber.length > 1) 16f else 20f
                val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(pitchNumber, cx, textY, textPaint)
            }
        }
    }
}

@Composable
private fun PitchLogCompact(pitchEvents: List<PlayEvent>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.height(160.dp)) {
        LazyColumn(modifier = Modifier.height(160.dp)) {
            items(pitchEvents) { event ->
                val call = event.details.call?.description ?: ""
                val short = when {
                    call == "Called Strike" -> "called strike"
                    call == "Swinging Strike" -> "swinging"
                    call == "Foul Tip" -> "foul tip"
                    call == "Foul Bunt" -> "foul"
                    call == "Ball In Dirt" -> "ball"
                    call.startsWith("In play, run(s)") -> "in play, runs"
                    call.startsWith("In play, out(s)") -> "in play, out"
                    call.startsWith("In play") -> "in play"
                    call == "Automatic Ball" -> "auto ball"
                    else -> call.lowercase()
                }
                val speed = event.pitchData?.startSpeed?.toInt()?.toString() ?: ""
                Row(Modifier.fillMaxWidth()) {
                    Text("${eventEventsIndex(event, pitchEvents)}", Modifier.width(14.dp), style = TextStyle(color = Color.White, fontSize = 10.sp))
                    Column(Modifier.weight(1f)) {
                        Text(short, style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 9.sp))
                        if (speed.isNotBlank()) {
                            Text("${speed} mph", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 8.sp))
                        }
                    }
                }
            }
        }
    }
}

private fun eventEventsIndex(event: PlayEvent, events: List<PlayEvent>): Int = events.indexOf(event) + 1

@Composable
private fun RecordsLine(gameFeed: GameFeed) {
    val away = gameFeed.gameData.teams.away
    val home = gameFeed.gameData.teams.home
    val awayRecord = away.recordText()
    val homeRecord = home.recordText()
    if (awayRecord.isNotBlank() || homeRecord.isNotBlank()) {
        Text(
            "${away.abbreviation} $awayRecord   ${home.abbreviation} $homeRecord",
            style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp)
        )
    }
}

private fun com.awper.lightscore.FeedTeam.recordText(): String {
    val wins = record?.wins
    val losses = record?.losses
    return if (wins != null && losses != null) "$wins-$losses" else ""
}

@Composable
private fun OutsDots(outs: Int) {
    Row {
        repeat(3) { i ->
            Box(Modifier.size(8.dp).padding(1.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    if (i < outs) drawCircle(Color.White, size.minDimension / 2)
                    else drawCircle(Color.White, size.minDimension / 2, style = Stroke(1f))
                }
            }
        }
    }
}

@Composable
private fun PregameView(
    gameFeed: GameFeed,
    seasonBattingByPlayerId: Map<Int, SeasonBattingLine>,
    seasonPitchingByPlayerId: Map<Int, SeasonPitchingLine>
) {
    val away = gameFeed.gameData.teams.away.abbreviation
    val home = gameFeed.gameData.teams.home.abbreviation
    val boxscore = gameFeed.liveData.boxscore

    Column(modifier = Modifier.padding(16.dp)) {
        Text("$away @ $home", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        RecordsLine(gameFeed)
        Spacer(Modifier.height(4.dp))
        val dt = gameFeed.gameData.datetime?.dateTime
        if (dt != null) Text(formatTime(dt), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 13.sp))
        val venue = gameFeed.gameData.venue
        if (venue != null) Text(venue.name.lowercase(), style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))

        Spacer(Modifier.height(12.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Text("probable pitchers", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(6.dp))
        StatsHeader(
            listOf("team", "pitcher", "era", "w/l"),
            listOf(0.16f, 0.50f, 0.14f, 0.14f)
        )
        Spacer(Modifier.height(4.dp))
        val pp = gameFeed.gameData.probablePitchers
        if (pp != null) {
            if (pp.away != null) {
                val awayPitching = seasonPitchingByPlayerId[pp.away.id]
                val era = awayPitching?.era ?: "—"
                val wl = "${awayPitching?.wins ?: 0}-${awayPitching?.losses ?: 0}"
                StatsRow(
                    listOf(away, pp.away.fullName.lowercase(), era, wl),
                    listOf(0.16f, 0.50f, 0.14f, 0.14f)
                )
            }
            if (pp.home != null) {
                val homePitching = seasonPitchingByPlayerId[pp.home.id]
                val era = homePitching?.era ?: "—"
                val wl = "${homePitching?.wins ?: 0}-${homePitching?.losses ?: 0}"
                StatsRow(
                    listOf(home, pp.home.fullName.lowercase(), era, wl),
                    listOf(0.16f, 0.50f, 0.14f, 0.14f)
                )
            }
        } else {
            Text("tbd", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        }

        Spacer(Modifier.height(16.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Text("lineups", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(6.dp))

        renderSeasonLineup(away, boxscore?.teams?.away, seasonBattingByPlayerId)
        Spacer(Modifier.height(8.dp))
        renderSeasonLineup(home, boxscore?.teams?.home, seasonBattingByPlayerId)
    }
}

@Composable
private fun renderSeasonLineup(
    teamAbbr: String,
    box: BoxscoreTeam?,
    seasonBattingByPlayerId: Map<Int, SeasonBattingLine>
) {
    Text(teamAbbr.lowercase(), style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    StatsHeader(
        listOf("name", "avg", "obp", "hr", "rbi", "slg", "ops", "sb"),
        listOf(0.34f, 0.10f, 0.10f, 0.07f, 0.09f, 0.10f, 0.10f, 0.06f)
    )
    Spacer(Modifier.height(4.dp))
    val batters = battingPlayersInOrder(box)
    if (batters.isNotEmpty()) {
        batters.forEach { player ->
            val playerId = player.person?.id ?: 0
            val bs = seasonBattingByPlayerId[playerId]
            StatsRow(
                listOf(
                    displayPlayerName(player),
                    bs?.avg ?: "—",
                    bs?.obp ?: "—",
                    bs?.hr?.toString() ?: "0",
                    bs?.rbi?.toString() ?: "0",
                    bs?.slg ?: "—",
                    bs?.ops ?: "—",
                    bs?.sb?.toString() ?: "0"
                ),
                listOf(0.34f, 0.10f, 0.10f, 0.07f, 0.09f, 0.10f, 0.10f, 0.06f)
            )
        }
    } else {
        Text("lineups not yet posted", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 11.sp))
    }
}

@Composable
private fun FinalView(gameFeed: GameFeed) {
    val away = gameFeed.gameData.teams.away.abbreviation
    val home = gameFeed.gameData.teams.home.abbreviation
    val innings = gameFeed.liveData.linescore?.innings ?: emptyList()
    val boxscore = gameFeed.liveData.boxscore

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        val awayR = gameFeed.liveData.linescore?.teams?.away?.runs ?: 0
        val homeR = gameFeed.liveData.linescore?.teams?.home?.runs ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(away, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text(" $awayR   ", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text(home, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text(" $homeR   [f]", style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(4.dp))
        RecordsLine(gameFeed)
        Spacer(Modifier.height(8.dp))
        InningBoxScore(away, home, innings, gameFeed.liveData.linescore?.teams?.away, gameFeed.liveData.linescore?.teams?.home)

        Spacer(Modifier.height(12.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Text("batting stats", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(4.dp))
        StatsHeader(
            listOf("name", "ab", "r", "h", "hr", "rbi", "bb", "so"),
            listOf(0.39f, 0.08f, 0.08f, 0.08f, 0.08f, 0.10f, 0.08f, 0.08f)
        )
        Spacer(Modifier.height(4.dp))

        Text(away.lowercase(), style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        val awayBox = boxscore?.teams?.away
        battingPlayersInOrder(awayBox).forEach { player ->
            val name = displayPlayerName(player)
            val bs = player.stats?.batting
            if (bs != null) {
                StatsRow(
                    listOf(
                        name,
                        bs.atBats?.toString() ?: "0",
                        bs.runs?.toString() ?: "0",
                        bs.hits?.toString() ?: "0",
                        bs.homeRuns?.toString() ?: "0",
                        bs.rbi?.toString() ?: "0",
                        bs.baseOnBalls?.toString() ?: "0",
                        bs.strikeOuts?.toString() ?: "0"
                    ),
                    listOf(0.39f, 0.08f, 0.08f, 0.08f, 0.08f, 0.10f, 0.08f, 0.08f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(home.lowercase(), style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        val homeBox = boxscore?.teams?.home
        battingPlayersInOrder(homeBox).forEach { player ->
            val name = displayPlayerName(player)
            val bs = player.stats?.batting
            if (bs != null) {
                StatsRow(
                    listOf(
                        name,
                        bs.atBats?.toString() ?: "0",
                        bs.runs?.toString() ?: "0",
                        bs.hits?.toString() ?: "0",
                        bs.homeRuns?.toString() ?: "0",
                        bs.rbi?.toString() ?: "0",
                        bs.baseOnBalls?.toString() ?: "0",
                        bs.strikeOuts?.toString() ?: "0"
                    ),
                    listOf(0.39f, 0.08f, 0.08f, 0.08f, 0.08f, 0.10f, 0.08f, 0.08f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))

        Text("pitching stats", style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp))
        Spacer(Modifier.height(4.dp))
        StatsHeader(
            listOf("name", "ip", "h", "r", "er", "bb", "so"),
            listOf(0.44f, 0.10f, 0.09f, 0.09f, 0.09f, 0.09f, 0.10f)
        )
        Spacer(Modifier.height(4.dp))

        Text(away.lowercase(), style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        awayBox?.pitchers?.forEach { playerId ->
            val player = awayBox.players?.get("ID$playerId")
            if (player != null) {
                val name = displayPlayerName(player)
                val ps = player.stats?.pitching
                if (ps != null) {
                    StatsRow(
                        listOf(
                            name,
                            ps.inningsPitched ?: "0.0",
                            ps.hits?.toString() ?: "0",
                            ps.runs?.toString() ?: "0",
                            ps.earnedRuns?.toString() ?: "0",
                            ps.baseOnBalls?.toString() ?: "0",
                            ps.strikeOuts?.toString() ?: "0"
                        ),
                        listOf(0.44f, 0.10f, 0.09f, 0.09f, 0.09f, 0.09f, 0.10f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(home.lowercase(), style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        homeBox?.pitchers?.forEach { playerId ->
            val player = homeBox.players?.get("ID$playerId")
            if (player != null) {
                val name = displayPlayerName(player)
                val ps = player.stats?.pitching
                if (ps != null) {
                    StatsRow(
                        listOf(
                            name,
                            ps.inningsPitched ?: "0.0",
                            ps.hits?.toString() ?: "0",
                            ps.runs?.toString() ?: "0",
                            ps.earnedRuns?.toString() ?: "0",
                            ps.baseOnBalls?.toString() ?: "0",
                            ps.strikeOuts?.toString() ?: "0"
                        ),
                        listOf(0.44f, 0.10f, 0.09f, 0.09f, 0.09f, 0.09f, 0.10f)
                    )
                }
            }
        }
    }
}

private fun displayPlayerName(player: BoxscorePlayer): String {
    val base = player.person?.fullName?.lowercase() ?: "?"
    val order = player.battingOrder?.toIntOrNull()
    val isSub = player.substitution == true || (order != null && order % 100 != 0)
    val role = player.note?.lowercase()?.trim()?.takeIf { noteText -> noteText.isNotEmpty() } ?: "ph"
    return if (isSub) "    $role $base" else base
}

private fun battingPlayersInOrder(box: BoxscoreTeam?): List<BoxscorePlayer> {
    val players = box?.players ?: return emptyList()
    val ids = box.batters?.takeIf { it.isNotEmpty() } ?: box.battingOrder.orEmpty()
    return ids.mapNotNull { players["ID$it"] }
        .filter { it.battingOrder?.toIntOrNull() != null }
        .sortedBy { it.battingOrder?.toIntOrNull() ?: Int.MAX_VALUE }
}

@Composable
private fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White))
}

private fun formatTime(iso: String): String {
    return try {
        val odt = OffsetDateTime.parse(iso, DateTimeFormatter.ISO_DATE_TIME)
        odt.atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEEE, MMM d  h:mm a", Locale.US))
            .lowercase()
    } catch (_: Exception) { iso.lowercase() }
}
