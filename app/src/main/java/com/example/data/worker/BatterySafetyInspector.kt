package com.example.data.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

import androidx.annotation.VisibleForTesting

data class BatterySafetySnapshot(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val shouldPauseAutoScan: Boolean = false
)

object BatterySafetyInspector {
    const val LOW_BATTERY_THRESHOLD_PERCENT = 20

    @VisibleForTesting
    internal var testSnapshotOverride: ((safetyModeEnabled: Boolean) -> BatterySafetySnapshot)? = null

    fun inspect(
        context: Context,
        safetyModeEnabled: Boolean,
        thresholdPercent: Int = LOW_BATTERY_THRESHOLD_PERCENT
    ): BatterySafetySnapshot {
        testSnapshotOverride?.let { return it(safetyModeEnabled) }

        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            100
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

        return evaluate(
            levelPercent = levelPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            safetyModeEnabled = safetyModeEnabled,
            thresholdPercent = thresholdPercent
        )
    }

    fun evaluate(
        levelPercent: Int,
        isCharging: Boolean,
        isPowerSaveMode: Boolean,
        safetyModeEnabled: Boolean,
        thresholdPercent: Int = LOW_BATTERY_THRESHOLD_PERCENT
    ): BatterySafetySnapshot {
        val belowThresholdUnplugged = levelPercent < thresholdPercent && !isCharging
        val shouldPause = safetyModeEnabled && (belowThresholdUnplugged || isPowerSaveMode)
        return BatterySafetySnapshot(
            levelPercent = levelPercent.coerceIn(0, 100),
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            shouldPauseAutoScan = shouldPause
        )
    }
}
