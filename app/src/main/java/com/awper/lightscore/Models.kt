package com.awper.lightscore

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable data class ScheduleResponse(val dates: List<GameDate> = emptyList())
@Serializable data class GameDate(val games: List<Game> = emptyList())

@Serializable
data class Game(
    val gamePk: Int,
    val gameDate: String? = null,
    val status: GameStatus,
    val teams: GameTeams,
    val linescore: Linescore? = null,
    val venue: Venue? = null
)

@Serializable data class GameStatus(val abstractGameState: String? = null, val detailedState: String? = null)

@Serializable
data class GameTeams(
    val away: TeamInfoWrap,
    val home: TeamInfoWrap
)

@Serializable data class TeamInfoWrap(val team: TeamInfo? = null)
@Serializable data class TeamInfo(val id: Int, val name: String, val abbreviation: String? = null)
@Serializable data class Venue(val id: Int, val name: String)

@Serializable
data class Linescore(
    val currentInning: Int? = null,
    val inningHalf: String? = null,
    val outs: Int? = null,
    val balls: Int? = null,
    val strikes: Int? = null,
    val offense: Offense? = null,
    val defense: Defense? = null,
    val teams: LinescoreTeams? = null,
    val innings: List<Inning> = emptyList()
)

@Serializable
data class Offense(
    val batter: Person? = null,
    val pitcher: Person? = null,
    val first: Person? = null,
    val second: Person? = null,
    val third: Person? = null
)

@Serializable
data class Defense(
    val pitcher: Person? = null
)

@Serializable data class Person(val id: Int, val fullName: String)
@Serializable data class LinescoreTeams(val away: TeamStats, val home: TeamStats)
@Serializable data class TeamStats(val runs: Int? = null, val hits: Int? = null, val errors: Int? = null)
@Serializable data class Inning(val num: Int, val home: TeamStats? = null, val away: TeamStats? = null)

typealias InningScore = Inning
typealias LinescoreTeam = TeamStats

@Serializable
data class GameFeed(
    val gameData: FeedGameData,
    val liveData: LiveData
)

@Serializable
data class FeedGameData(
    val datetime: DateTime? = null,
    val status: GameStatus,
    val teams: FeedTeams,
    val probablePitchers: ProbablePitchers? = null,
    val venue: Venue? = null
)

@Serializable data class DateTime(val dateTime: String? = null)
@Serializable data class FeedTeams(val away: FeedTeam, val home: FeedTeam)
@Serializable data class FeedTeam(val id: Int, val name: String, val abbreviation: String, val record: TeamRecord? = null)
@Serializable data class TeamRecord(val wins: Int? = null, val losses: Int? = null)
@Serializable data class ProbablePitchers(val away: Pitcher? = null, val home: Pitcher? = null)
@Serializable data class Pitcher(val id: Int, val fullName: String)

@Serializable
data class LiveData(
    val linescore: Linescore? = null,
    val plays: Plays? = null,
    val boxscore: Boxscore? = null
)

@Serializable
data class Plays(
    val currentPlay: Play? = null,
    val allPlays: List<Play> = emptyList()
)

@Serializable
data class Play(
    val playEvents: List<PlayEvent> = emptyList(),
    val result: PlayResult? = null,
    val matchup: Matchup? = null
)

@Serializable data class PlayResult(val description: String? = null, val event: String? = null)
@Serializable data class Matchup(val batter: Person? = null, val pitcher: Person? = null)

@Serializable
data class PlayEvent(
    val isPitch: Boolean = false,
    val pitchData: PitchData? = null,
    val details: EventDetails
)

@Serializable
data class PitchData(
    val startSpeed: Double? = null,
    val coordinates: Coordinates? = null,
    val strikeZoneTop: Double? = null,
    val strikeZoneBottom: Double? = null
)

@Serializable data class Coordinates(val pX: Double? = null, val pZ: Double? = null)
@Serializable data class EventDetails(val call: Call? = null, val type: PitchType? = null)
@Serializable data class Call(val description: String? = null)
@Serializable data class PitchType(val description: String? = null)

@Serializable data class TeamsResponse(val teams: List<TeamInfo> = emptyList())

@Serializable
data class Boxscore(
    val teams: BoxscoreTeams? = null
)

@Serializable
data class BoxscoreTeams(
    val home: BoxscoreTeam? = null,
    val away: BoxscoreTeam? = null
)

@Serializable
data class BoxscoreTeam(
    val team: TeamInfoWrap? = null,
    val teamStats: BoxscoreTeamStats? = null,
    val players: Map<String, BoxscorePlayer>? = null,
    val batters: List<Int>? = null,
    val pitchers: List<Int>? = null,
    val bench: List<Int>? = null,
    val bullpen: List<Int>? = null,
    val battingOrder: List<Int>? = null
)

@Serializable
data class BoxscoreTeamStats(
    val batting: TeamBattingStats? = null,
    val pitching: TeamPitchingStats? = null
)

@Serializable
data class TeamBattingStats(
    val runs: Int? = null,
    val hits: Int? = null,
    val errors: Int? = null,
    val leftOnBase: Int? = null
)

@Serializable
data class TeamPitchingStats(
    val runs: Int? = null,
    val earnedRuns: Int? = null
)

@Serializable
data class BoxscorePlayer(
    val person: Person? = null,
    val jerseyNumber: String? = null,
    val position: Position? = null,
    val status: PlayerStatus? = null,
    val stats: PlayerStats? = null,
    val gameStatus: GameStatusInfo? = null,
    val battingOrder: String? = null,
    val substitution: Boolean? = null,
    val note: String? = null,
    val parentTeamId: Int? = null
)

@Serializable
data class Position(
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val abbreviation: String? = null
)

@Serializable
data class PlayerStatus(
    val code: String? = null,
    val description: String? = null
)

@Serializable
data class PlayerStats(
    val batting: BattingStats? = null,
    val pitching: PitchingStats? = null
)

@Serializable
data class BattingStats(
    val gamesPlayed: Int? = null,
    val flyOuts: Int? = null,
    val groundOuts: Int? = null,
    val runs: Int? = null,
    val doubles: Int? = null,
    val triples: Int? = null,
    val homeRuns: Int? = null,
    val strikeOuts: Int? = null,
    val baseOnBalls: Int? = null,
    val hits: Int? = null,
    val hitByPitch: Int? = null,
    val atBats: Int? = null,
    val caughtStealing: Int? = null,
    val stolenBases: Int? = null,
    val groundIntoDoublePlay: Int? = null,
    val groundIntoTriplePlay: Int? = null,
    val plateAppearances: Int? = null,
    val totalBases: Int? = null,
    val rbi: Int? = null,
    @SerialName("avg") val avg: String? = null,
    @SerialName("obp") val obp: String? = null,
    @SerialName("slg") val slg: String? = null,
    @SerialName("ops") val ops: String? = null,
    val leftOnBase: Int? = null,
    val sacBunts: Int? = null,
    val sacFlies: Int? = null,
    val catchersInterference: Int? = null,
    val pickoffs: Int? = null
)

@Serializable
data class PitchingStats(
    val gamesPlayed: Int? = null,
    val gamesStarted: Int? = null,
    val groundOuts: Int? = null,
    val airOuts: Int? = null,
    val runs: Int? = null,
    val doubles: Int? = null,
    val triples: Int? = null,
    val homeRuns: Int? = null,
    val strikeOuts: Int? = null,
    val baseOnBalls: Int? = null,
    val hits: Int? = null,
    val hitByPitch: Int? = null,
    val atBats: Int? = null,
    val caughtStealing: Int? = null,
    val stolenBases: Int? = null,
    val numberOfPitches: Int? = null,
    val era: String? = null,
    val inningsPitched: String? = null,
    val wins: Int? = null,
    val losses: Int? = null,
    val saves: Int? = null,
    val saveOpportunities: Int? = null,
    val holds: Int? = null,
    val blownSaves: Int? = null,
    val earnedRuns: Int? = null,
    val battersFaced: Int? = null,
    val outs: Int? = null,
    val completeGames: Int? = null,
    val shutouts: Int? = null,
    val strikes: Int? = null,
    val hitBatsmen: Int? = null,
    val balks: Int? = null,
    val wildPitches: Int? = null,
    val pickoffs: Int? = null,
    val rbi: Int? = null,
    val gamesFinished: Int? = null,
    val inheritedRunners: Int? = null,
    val inheritedRunnersScored: Int? = null,
    val sacBunts: Int? = null,
    val sacFlies: Int? = null
)

@Serializable
data class GameStatusInfo(
    val isCurrentBatter: Boolean? = null,
    val isCurrentPitcher: Boolean? = null,
    val isOnBench: Boolean? = null,
    val isSubstitute: Boolean? = null
)
