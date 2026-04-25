package com.awper.lightscore

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val json = Json { ignoreUnknownKeys = true; isLenient = true }

val client = HttpClient {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) { requestTimeoutMillis = 15000 }
    defaultRequest { header("Accept", "application/json") }
}

suspend fun fetchSchedule(date: String): ScheduleResponse =
    client.get("https://statsapi.mlb.com/api/v1/schedule") {
        parameter("sportId", 1)
        parameter("date", date)
        parameter("hydrate", "linescore,probablePitcher,team")
    }.body()

suspend fun fetchGameFeed(gamePk: Int): GameFeed =
    client.get("https://statsapi.mlb.com/api/v1.1/game/$gamePk/feed/live").body()

suspend fun fetchBoxscore(gamePk: Int): Boxscore =
    client.get("https://statsapi.mlb.com/api/v1/game/$gamePk/boxscore").body()

suspend fun fetchTeams(): List<TeamInfo> =
    client.get("https://statsapi.mlb.com/api/v1/teams") { parameter("sportId", 1) }
        .body<TeamsResponse>().teams

data class SeasonBattingLine(
    val avg: String = "—",
    val obp: String = "—",
    val hr: Int = 0,
    val rbi: Int = 0,
    val slg: String = "—",
    val ops: String = "—",
    val sb: Int = 0
)

data class SeasonPitchingLine(
    val era: String = "—",
    val wins: Int = 0,
    val losses: Int = 0
)

suspend fun fetchSeasonBattingStats(playerIds: List<Int>): Map<Int, SeasonBattingLine> {
    if (playerIds.isEmpty()) return emptyMap()
    val response = client.get("https://statsapi.mlb.com/api/v1/people") {
        parameter("personIds", playerIds.joinToString(","))
        parameter("hydrate", "stats(group=[hitting],type=[season])")
    }.bodyAsText()
    return parseSeasonBattingStats(response)
}

suspend fun fetchSeasonPitchingStats(playerIds: List<Int>): Map<Int, SeasonPitchingLine> {
    if (playerIds.isEmpty()) return emptyMap()
    val response = client.get("https://statsapi.mlb.com/api/v1/people") {
        parameter("personIds", playerIds.joinToString(","))
        parameter("hydrate", "stats(group=[pitching],type=[season])")
    }.bodyAsText()
    return parseSeasonPitchingStats(response)
}

data class StandingsTeamLine(
    val division: String,
    val divisionRank: Int,
    val teamName: String,
    val wins: Int,
    val losses: Int,
    val gamesBack: String,
    val streak: String
)

data class PlayerSearchResult(val id: Int, val name: String, val position: String)

data class PlayerStatsDetail(
    val name: String,
    val team: String,
    val position: String,
    val isPitcher: Boolean,
    val seasonStats: List<Pair<String, String>>,
    val lastTen: List<PlayerGameLine>
)

data class PlayerGameLine(
    val date: String,
    val matchup: String,
    val values: List<String>,
    val atBats: String,
    val hits: String,
    val rbi: String,
    val homeRuns: String
)

suspend fun fetchStandings(): List<StandingsTeamLine> {
    val response = client.get("https://statsapi.mlb.com/api/v1/standings") {
        parameter("leagueId", "103,104")
        parameter("standingsTypes", "regularSeason")
    }.bodyAsText()
    return parseStandings(response)
}

suspend fun searchPlayers(name: String): List<PlayerSearchResult> {
    if (name.isBlank()) return emptyList()
    val response = client.get("https://statsapi.mlb.com/api/v1/people/search") {
        parameter("names", name.trim())
    }.bodyAsText()
    return try {
        val people = json.parseToJsonElement(response).jsonObject["people"]?.jsonArray ?: return emptyList()
        people.mapNotNull { p ->
            val obj = p.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val fullName = obj["fullName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val pos = obj["primaryPosition"]?.jsonObject?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
            PlayerSearchResult(id, fullName, pos)
        }.take(8)
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun fetchPlayerStatsDetail(playerId: Int): PlayerStatsDetail {
    val response = client.get("https://statsapi.mlb.com/api/v1/people/$playerId") {
        parameter("hydrate", "stats(group=[hitting,pitching],type=[season,gameLog]),currentTeam")
    }.bodyAsText()
    return parsePlayerStatsDetail(response)
}

private fun parseSeasonBattingStats(body: String): Map<Int, SeasonBattingLine> {
    return try {
        val root = json.parseToJsonElement(body).jsonObject
        val people = root["people"]?.jsonArray ?: return emptyMap()
        people.mapNotNull { p ->
            val person = p.jsonObject
            val id = person["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val stat = person["stats"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("splits")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("stat")?.jsonObject
                ?: return@mapNotNull id to SeasonBattingLine()
            id to SeasonBattingLine(
                avg = stat["avg"]?.jsonPrimitive?.contentOrNull ?: "—",
                obp = stat["obp"]?.jsonPrimitive?.contentOrNull ?: "—",
                hr = stat["homeRuns"]?.jsonPrimitive?.intOrNull ?: 0,
                rbi = stat["rbi"]?.jsonPrimitive?.intOrNull ?: 0,
                slg = stat["slg"]?.jsonPrimitive?.contentOrNull ?: "—",
                ops = stat["ops"]?.jsonPrimitive?.contentOrNull ?: "—",
                sb = stat["stolenBases"]?.jsonPrimitive?.intOrNull ?: 0
            )
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun parseSeasonPitchingStats(body: String): Map<Int, SeasonPitchingLine> {
    return try {
        val root = json.parseToJsonElement(body).jsonObject
        val people = root["people"]?.jsonArray ?: return emptyMap()
        people.mapNotNull { p ->
            val person = p.jsonObject
            val id = person["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val stat = person["stats"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("splits")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("stat")?.jsonObject
                ?: return@mapNotNull id to SeasonPitchingLine()
            id to SeasonPitchingLine(
                era = stat["era"]?.jsonPrimitive?.contentOrNull ?: "—",
                wins = stat["wins"]?.jsonPrimitive?.intOrNull ?: 0,
                losses = stat["losses"]?.jsonPrimitive?.intOrNull ?: 0
            )
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun parseStandings(body: String): List<StandingsTeamLine> {
    return try {
        val records = json.parseToJsonElement(body).jsonObject["records"]?.jsonArray ?: return emptyList()
        records.flatMap { recordElement ->
            val record = recordElement.jsonObject
            val divisionId = record["division"]?.jsonObject?.get("id")?.jsonPrimitive?.intOrNull
            val division = divisionId.toDivisionName()
                ?: record["division"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                    ?.replace("American League ", "al ")
                    ?.replace("National League ", "nl ")
                    ?.lowercase()
                ?: "standings"
            record["teamRecords"]?.jsonArray?.mapNotNull { teamElement ->
                val team = teamElement.jsonObject
                StandingsTeamLine(
                    division = division,
                    divisionRank = team["divisionRank"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE,
                    teamName = team["team"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull?.lowercase() ?: return@mapNotNull null,
                    wins = team["wins"]?.jsonPrimitive?.intOrNull ?: 0,
                    losses = team["losses"]?.jsonPrimitive?.intOrNull ?: 0,
                    gamesBack = team["gamesBack"]?.jsonPrimitive?.contentOrNull ?: "-",
                    streak = team["streak"]?.jsonObject?.get("streakCode")?.jsonPrimitive?.contentOrNull?.lowercase() ?: ""
                )
            }.orEmpty()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun Int?.toDivisionName(): String? = when (this) {
    201 -> "al east"
    202 -> "al central"
    200 -> "al west"
    204 -> "nl east"
    205 -> "nl central"
    203 -> "nl west"
    else -> null
}

private fun parsePlayerStatsDetail(body: String): PlayerStatsDetail {
    return try {
        val person = json.parseToJsonElement(body).jsonObject["people"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: return PlayerStatsDetail("player", "", "", false, emptyList(), emptyList())
        val name = person["fullName"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: "player"
        val team = person["currentTeam"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull?.abbr()?.lowercase() ?: ""
        val position = person["primaryPosition"]?.jsonObject?.get("abbreviation")?.jsonPrimitive?.contentOrNull ?: ""
        val isPitcher = position == "P"
        val stats = person["stats"]?.jsonArray.orEmpty()
        var seasonStats = emptyList<Pair<String, String>>()
        var games = emptyList<PlayerGameLine>()
        stats.forEach { statBlockElement ->
            val statBlock = statBlockElement.jsonObject
            val type = statBlock["type"]?.jsonObject?.get("displayName")?.jsonPrimitive?.contentOrNull ?: ""
            val group = statBlock["group"]?.jsonObject?.get("displayName")?.jsonPrimitive?.contentOrNull ?: ""
            if ((isPitcher && group != "pitching") || (!isPitcher && group != "hitting")) return@forEach
            val splits = statBlock["splits"]?.jsonArray.orEmpty()
            if (type == "season") {
                val stat = splits.firstOrNull()?.jsonObject?.get("stat")?.jsonObject ?: return@forEach
                seasonStats = if (isPitcher) {
                    listOf(
                        "g" to stat.value("gamesPitched", "gamesPlayed"),
                        "gs" to stat.value("gamesStarted"),
                        "ip" to stat.value("inningsPitched"),
                        "era" to stat.value("era"),
                        "fip" to estimatedFip(stat),
                        "whip" to stat.value("whip"),
                        "k" to stat.value("strikeOuts"),
                        "bb" to stat.value("baseOnBalls"),
                        "hr" to stat.value("homeRuns")
                    )
                } else {
                    listOf(
                        "g" to stat.value("gamesPlayed"),
                        "ab" to stat.value("atBats"),
                        "avg" to stat.value("avg"),
                        "h" to stat.value("hits"),
                        "rbi" to stat.value("rbi"),
                        "hr" to stat.value("homeRuns"),
                        "obp" to stat.value("obp"),
                        "slg" to stat.value("slg"),
                        "ops" to stat.value("ops")
                    )
                }
            } else if (type == "gameLog") {
                games = splits.mapNotNull { splitElement ->
                    val split = splitElement.jsonObject
                    val stat = split["stat"]?.jsonObject ?: return@mapNotNull null
                    val team = split["team"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                    val opponent = split["opponent"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
                    val isHome = split["isHome"]?.jsonPrimitive?.contentOrNull == "true"
                    val matchup = if (isHome) "${team.abbr()} vs ${opponent.abbr()}" else "${team.abbr()} @ ${opponent.abbr()}"
                    PlayerGameLine(
                        date = split["date"]?.jsonPrimitive?.contentOrNull?.drop(5) ?: "",
                        matchup = matchup,
                        values = if (isPitcher) listOf(
                            stat.value("inningsPitched"), stat.value("hits"), stat.value("runs"), stat.value("earnedRuns"), stat.value("baseOnBalls"), stat.value("strikeOuts"), stat.value("homeRuns")
                        ) else listOf(
                            stat.value("atBats"), stat.value("hits"), stat.value("runs"), stat.value("rbi"), stat.value("homeRuns")
                        ),
                        atBats = stat["atBats"]?.jsonPrimitive?.contentOrNull ?: "0",
                        hits = stat["hits"]?.jsonPrimitive?.contentOrNull ?: "0",
                        rbi = stat["rbi"]?.jsonPrimitive?.contentOrNull ?: "0",
                        homeRuns = stat["homeRuns"]?.jsonPrimitive?.contentOrNull ?: "0"
                    )
                }.takeLast(10).reversed()
            }
        }
        PlayerStatsDetail(name, team, position, isPitcher, seasonStats, games)
    } catch (_: Exception) {
        PlayerStatsDetail("player", "", "", false, emptyList(), emptyList())
    }
}

private fun kotlinx.serialization.json.JsonObject.value(vararg keys: String): String {
    keys.forEach { key ->
        val value = this[key]?.jsonPrimitive?.contentOrNull
        if (!value.isNullOrBlank()) return value
    }
    return "—"
}

private fun estimatedFip(stat: kotlinx.serialization.json.JsonObject): String {
    val ip = stat["inningsPitched"]?.jsonPrimitive?.contentOrNull?.toInnings() ?: return "—"
    if (ip <= 0.0) return "—"
    val hr = stat["homeRuns"]?.jsonPrimitive?.intOrNull ?: 0
    val bb = stat["baseOnBalls"]?.jsonPrimitive?.intOrNull ?: 0
    val hbp = stat["hitBatsmen"]?.jsonPrimitive?.intOrNull ?: 0
    val k = stat["strikeOuts"]?.jsonPrimitive?.intOrNull ?: 0
    return String.format("%.2f", ((13 * hr + 3 * (bb + hbp) - 2 * k) / ip) + 3.10)
}

private fun String.toInnings(): Double {
    val parts = split(".")
    val whole = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
    val outs = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    return whole + outs / 3.0
}

private fun String.abbr(): String = when (this) {
    "Arizona Diamondbacks" -> "ARI"
    "Athletics" -> "ATH"
    "Atlanta Braves" -> "ATL"
    "Baltimore Orioles" -> "BAL"
    "Boston Red Sox" -> "BOS"
    "Chicago Cubs" -> "CHC"
    "Chicago White Sox" -> "CWS"
    "Cincinnati Reds" -> "CIN"
    "Cleveland Guardians" -> "CLE"
    "Colorado Rockies" -> "COL"
    "Detroit Tigers" -> "DET"
    "Houston Astros" -> "HOU"
    "Kansas City Royals" -> "KC"
    "Los Angeles Angels" -> "LAA"
    "Los Angeles Dodgers" -> "LAD"
    "Miami Marlins" -> "MIA"
    "Milwaukee Brewers" -> "MIL"
    "Minnesota Twins" -> "MIN"
    "New York Mets" -> "NYM"
    "New York Yankees" -> "NYY"
    "Philadelphia Phillies" -> "PHI"
    "Pittsburgh Pirates" -> "PIT"
    "San Diego Padres" -> "SD"
    "San Francisco Giants" -> "SF"
    "Seattle Mariners" -> "SEA"
    "St. Louis Cardinals" -> "STL"
    "Tampa Bay Rays" -> "TB"
    "Texas Rangers" -> "TEX"
    "Toronto Blue Jays" -> "TOR"
    "Washington Nationals" -> "WSH"
    else -> take(3).uppercase()
}
