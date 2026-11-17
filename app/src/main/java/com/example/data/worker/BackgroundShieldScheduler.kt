package com.example.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class BackgroundShieldScheduler(context: Context) {
    private val appContext = context.applicationContext

    private val workManager: WorkManager?
        get() = runCatching { WorkManager.getInstance(appContext) }.getOrNull()

    fun syncPeriodicShield(
        enabled: Boolean,
        lowBatterySafetyMode: Boolean,
        intervalHours: Int
    ) {
        val manager = workManager ?: return
        if (!enabled) {
            manager.cancelUniqueWork(UNIQUE_PERIODIC_SHIELD_WORK)
            return
        }

        val safeHours = intervalHours.coerceIn(6, 24).toLong()
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(lowBatterySafetyMode)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackgroundShieldWorker>(
            safeHours,
            TimeUnit.HOURS,
            15L,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.MINUTES)
            .addTag(TAG_BACKGROUND_SHIELD)
            .build()

        manager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_SHIELD_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun triggerImmediateCheck(lowBatterySafetyMode: Boolean) {
        val manager = workManager ?: return
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(lowBatterySafetyMode)
            .build()

        val request = OneTimeWorkRequestBuilder<BackgroundShieldWorker>()
            .setConstraints(constraints)
            .addTag(TAG_IMMEDIATE_CHECK)
            .build()

        manager.enqueueUniqueWork(
            UNIQUE_IMMEDIATE_SHIELD_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun observePeriodicStatus(): Flow<WorkInfo?> {
        val manager = workManager ?: return flowOf(null)
        return manager.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_SHIELD_WORK)
            .map { infos -> infos.firstOrNull() }
    }

    fun observeImmediateStatus(): Flow<WorkInfo?> {
        val manager = workManager ?: return flowOf(null)
        return manager.getWorkInfosForUniqueWorkFlow(UNIQUE_IMMEDIATE_SHIELD_WORK)
            .map { infos -> infos.firstOrNull() }
    }

    companion object {
        const val UNIQUE_PERIODIC_SHIELD_WORK = "shieldcore_periodic_background_shield"
        const val UNIQUE_IMMEDIATE_SHIELD_WORK = "shieldcore_immediate_background_shield"
        const val TAG_BACKGROUND_SHIELD = "shieldcore_auto_scan"
        const val TAG_IMMEDIATE_CHECK = "shieldcore_immediate_check"
    }
}
