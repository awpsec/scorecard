package com.awper.lightscore.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.awper.lightscore.fetchSchedule
import com.awper.lightscore.mlbSlateDate
import com.awper.lightscore.settings.NetworkGate
import com.awper.lightscore.settings.SettingsStore
import kotlinx.coroutines.flow.first

class ScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = SettingsStore(applicationContext)
        val favorites = store.favorites.first().toSet()
        val updateAll = store.updateBackground.first()
        val updateFavoritesOnly = !updateAll && store.updateFavoritesBackground.first() && favorites.isNotEmpty()

        if (!updateAll && !updateFavoritesOnly) return Result.success()

        val canPoll = NetworkGate.shouldPoll(
            context = applicationContext,
            store = store,
            isFavoriteGame = updateFavoritesOnly
        )
        if (!canPoll) {
            WorkScheduler.enqueue(applicationContext)
            return Result.success()
        }

        return try {
            val schedule = fetchSchedule(mlbSlateDate())
            if (updateFavoritesOnly) {
                schedule.dates.flatMap { it.games }.filter { game ->
                    val awayId = game.teams.away.team?.id?.toString()
                    val homeId = game.teams.home.team?.id?.toString()
                    awayId in favorites || homeId in favorites
                }
            }
            WorkScheduler.enqueue(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
