package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.local.SecurityDatabase
import com.example.data.preferences.SecurityPreferences
import com.example.data.worker.BackgroundShieldScheduler
import com.example.data.worker.BackgroundShieldWorker
import com.example.data.worker.BatterySafetyInspector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackgroundShieldWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        BatterySafetyInspector.testSnapshotOverride = null
    }

    @Test
    fun `battery safety inspector pauses when battery low and unplugged`() {
        val paused = BatterySafetyInspector.evaluate(
            levelPercent = 15,
            isCharging = false,
            isPowerSaveMode = false,
            safetyModeEnabled = true
        )
        assertTrue(paused.shouldPauseAutoScan)

        val charging = BatterySafetyInspector.evaluate(
            levelPercent = 15,
            isCharging = true,
            isPowerSaveMode = false,
            safetyModeEnabled = true
        )
        assertFalse(charging.shouldPauseAutoScan)

        val powerSaver = BatterySafetyInspector.evaluate(
            levelPercent = 75,
            isCharging = false,
            isPowerSaveMode = true,
            safetyModeEnabled = true
        )
        assertTrue(powerSaver.shouldPauseAutoScan)

        val disabledSafety = BatterySafetyInspector.evaluate(
            levelPercent = 10,
            isCharging = false,
            isPowerSaveMode = true,
            safetyModeEnabled = false
        )
        assertFalse(disabledSafety.shouldPauseAutoScan)
    }

    @Test
    fun `scheduler enqueues and cancels periodic shield work with battery constraint`() {
        val scheduler = BackgroundShieldScheduler(context)
        val workManager = WorkManager.getInstance(context)

        scheduler.syncPeriodicShield(
            enabled = true,
            lowBatterySafetyMode = true,
            intervalHours = 6
        )

        val enqueued = workManager
            .getWorkInfosForUniqueWork(BackgroundShieldScheduler.UNIQUE_PERIODIC_SHIELD_WORK)
            .get()
        assertEquals(1, enqueued.size)
        assertEquals(WorkInfo.State.ENQUEUED, enqueued.first().state)
        assertTrue(enqueued.first().constraints.requiresBatteryNotLow())

        scheduler.syncPeriodicShield(
            enabled = false,
            lowBatterySafetyMode = true,
            intervalHours = 6
        )

        val cancelled = workManager
            .getWorkInfosForUniqueWork(BackgroundShieldScheduler.UNIQUE_PERIODIC_SHIELD_WORK)
            .get()
        assertTrue(cancelled.isEmpty() || cancelled.first().state == WorkInfo.State.CANCELLED)
    }

    @Test
    fun `background shield worker pauses gracefully on low battery`() = runTest {
        val prefs = SecurityPreferences(context)
        prefs.setRealTimeShield(true)
        prefs.setLowBatterySafetyMode(true)

        BatterySafetyInspector.testSnapshotOverride = { safetyEnabled ->
            BatterySafetyInspector.evaluate(
                levelPercent = 12,
                isCharging = false,
                isPowerSaveMode = false,
                safetyModeEnabled = safetyEnabled
            )
        }

        val worker = TestListenableWorkerBuilder<BackgroundShieldWorker>(context).build()
        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val status = result.outputData.getString(BackgroundShieldWorker.KEY_RESULT_STATUS)
        assertEquals(BackgroundShieldWorker.STATUS_PAUSED_LOW_BATTERY, status)
    }

    @Test
    fun `background shield worker scans sandbox and records threat when battery healthy`() = runTest {
        val prefs = SecurityPreferences(context)
        prefs.setRealTimeShield(true)
        prefs.setLowBatterySafetyMode(true)

        BatterySafetyInspector.testSnapshotOverride = { safetyEnabled ->
            BatterySafetyInspector.evaluate(
                levelPercent = 88,
                isCharging = true,
                isPowerSaveMode = false,
                safetyModeEnabled = safetyEnabled
            )
        }

        val suspiciousFile = File(context.filesDir, "payload_test.pdf.apk")
        suspiciousFile.writeText("suspicious_test_payload")

        try {
            val worker = TestListenableWorkerBuilder<BackgroundShieldWorker>(context).build()
            val result = worker.doWork()

            assertTrue(result is ListenableWorker.Result.Success)
            val status = result.outputData.getString(BackgroundShieldWorker.KEY_RESULT_STATUS)
            assertEquals(BackgroundShieldWorker.STATUS_THREAT_FOUND, status)
            assertTrue(result.outputData.getInt(BackgroundShieldWorker.KEY_THREATS_FOUND, 0) >= 1)

            val dao = SecurityDatabase.getInstance(context).securityDao()
            val history = dao.observeRecentScans().first()
            assertTrue(history.any { it.scanType == "Background Auto-Scan" })
        } finally {
            suspiciousFile.delete()
        }
    }
}
