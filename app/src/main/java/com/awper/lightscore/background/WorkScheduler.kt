package com.awper.lightscore.background

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.awper.lightscore.settings.SettingsStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_WORK_NAME = "lightscore_schedule_poll"

    suspend fun enqueue(context: Context) {
        val store = SettingsStore(context.applicationContext)
        val delayMinutes = if (store.lowDataMode.first()) 15L else 10L
        val request = OneTimeWorkRequestBuilder<ScheduleWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
