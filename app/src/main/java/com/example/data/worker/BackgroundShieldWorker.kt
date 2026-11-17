package com.example.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.engine.DeviceSecurityEngine
import com.example.data.local.ScanHistoryEntity
import com.example.data.local.SecurityDatabase
import com.example.data.model.AppRiskLevel
import com.example.data.model.ScannedFileResult
import com.example.data.preferences.SecurityPreferences
import kotlinx.coroutines.flow.first

class BackgroundShieldWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = SecurityPreferences(applicationContext)
        val userPrefs = prefs.prefsFlow.first()

        if (!userPrefs.realTimeShieldEnabled) {
            return Result.success(
                workDataOf(
                    KEY_RESULT_STATUS to STATUS_DISABLED
                )
            )
        }

        val battery = BatterySafetyInspector.inspect(
            context = applicationContext,
            safetyModeEnabled = userPrefs.lowBatterySafetyMode
        )

        if (battery.shouldPauseAutoScan) {
            val now = System.currentTimeMillis()
            val reasonText = if (userPrefs.bengaliLanguage) {
                if (battery.isPowerSaveMode) {
                    "ব্যাটারি সেভার সক্রিয় (${battery.levelPercent}%) — অটো-স্ক্যান বিরত রাখা হয়েছে"
                } else {
                    "লো-ব্যাটারি (${battery.levelPercent}%) — সুরক্ষার জন্য অটো-স্ক্যান স্থগিত"
                }
            } else {
                if (battery.isPowerSaveMode) {
                    "Power Saver active (${battery.levelPercent}%) — auto-scan paused"
                } else {
                    "Low battery (${battery.levelPercent}%) — auto-scan paused for safety"
                }
            }
            prefs.recordBackgroundScanResult(
                timestamp = now,
                status = STATUS_PAUSED_LOW_BATTERY,
                summary = reasonText
            )
            return Result.success(
                workDataOf(
                    KEY_RESULT_STATUS to STATUS_PAUSED_LOW_BATTERY,
                    KEY_BATTERY_PERCENT to battery.levelPercent,
                    KEY_SUMMARY to reasonText
                )
            )
        }

        val db = SecurityDatabase.getInstance(applicationContext)
        val dao = db.securityDao()
        dao.insertSignatures(SecurityDatabase.DEFAULT_SIGNATURES)

        val engine = DeviceSecurityEngine(applicationContext, dao)
        val scanned = mutableListOf<ScannedFileResult>()

        scanned += engine.scanSandboxDirectories { _, _ -> }

        val folderUri = userPrefs.lastSelectedFolderUri
        if (!folderUri.isNullOrBlank()) {
            runCatching {
                scanned += engine.scanDocumentTree(Uri.parse(folderUri), maxFiles = 120) { _, _ -> }
            }
        }

        val apps = engine.auditLaunchableApps()
        val junk = engine.analyzeJunkCategories()
        val junkBytes = junk.sumOf { it.sizeBytes }
        val threats = scanned.filter { it.isThreat }

        val score = calculateScore(
            shieldEnabled = userPrefs.realTimeShieldEnabled,
            threatsCount = threats.size,
            highRiskApps = apps.count { it.riskLevel == AppRiskLevel.HIGH },
            elevatedApps = apps.count { it.riskLevel == AppRiskLevel.ELEVATED },
            junkBytes = junkBytes
        )

        val summary = if (userPrefs.bengaliLanguage) {
            "অটো-স্ক্যান: ${scanned.size} ফাইল, ${apps.size} অ্যাপ • ${threats.size} হুমকি"
        } else {
            "Auto-scan: ${scanned.size} files, ${apps.size} apps • ${threats.size} threats"
        }

        val now = System.currentTimeMillis()
        dao.insertScanHistory(
            ScanHistoryEntity(
                timestamp = now,
                scanType = "Background Auto-Scan",
                filesScanned = scanned.size,
                appsAudited = apps.size,
                threatsFound = threats.size,
                junkBytesFound = junkBytes,
                securityScore = score,
                summary = summary
            )
        )

        val status = if (threats.isNotEmpty()) STATUS_THREAT_FOUND else STATUS_COMPLETED
        prefs.recordBackgroundScanResult(
            timestamp = now,
            status = status,
            summary = summary
        )

        if (threats.isNotEmpty()) {
            postThreatAlertNotification(
                threatCount = threats.size,
                firstThreatName = threats.first().threatName ?: threats.first().fileName,
                isBengali = userPrefs.bengaliLanguage
            )
        }

        return Result.success(
            workDataOf(
                KEY_RESULT_STATUS to status,
                KEY_FILES_SCANNED to scanned.size,
                KEY_THREATS_FOUND to threats.size,
                KEY_SECURITY_SCORE to score,
                KEY_BATTERY_PERCENT to battery.levelPercent,
                KEY_SUMMARY to summary
            )
        )
    }

    private fun postThreatAlertNotification(
        threatCount: Int,
        firstThreatName: String,
        isBengali: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notif_channel_shield_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.notif_channel_shield_desc)
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBengali) {
            "ShieldCore সতর্কতা: $threatCount টি ঝুঁকি শনাক্ত"
        } else {
            "ShieldCore Alert: $threatCount Threat(s) Detected"
        }
        val body = if (isBengali) {
            "ব্যাকগ্রাউন্ড স্ক্যানে '$firstThreatName' পাওয়া গেছে। পর্যালোচনা করতে ট্যাপ করুন।"
        } else {
            "Background auto-scan flagged '$firstThreatName'. Tap to inspect."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_RESULT_STATUS = "result_status"
        const val KEY_FILES_SCANNED = "files_scanned"
        const val KEY_THREATS_FOUND = "threats_found"
        const val KEY_SECURITY_SCORE = "security_score"
        const val KEY_BATTERY_PERCENT = "battery_percent"
        const val KEY_SUMMARY = "summary"

        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_THREAT_FOUND = "THREAT_FOUND"
        const val STATUS_PAUSED_LOW_BATTERY = "PAUSED_LOW_BATTERY"
        const val STATUS_DISABLED = "DISABLED"

        private const val CHANNEL_ID = "shieldcore_bg_alerts"
        private const val NOTIFICATION_ID = 4109

        internal fun calculateScore(
            shieldEnabled: Boolean,
            threatsCount: Int,
            highRiskApps: Int,
            elevatedApps: Int,
            junkBytes: Long
        ): Int {
            var score = 100
            if (!shieldEnabled) score -= 18
            score -= (threatsCount * 25)
            score -= (highRiskApps * 4).coerceAtMost(16)
            score -= (elevatedApps * 1).coerceAtMost(8)
            if (junkBytes > 10L * 1024L * 1024L) score -= 4
            return score.coerceIn(15, 100)
        }
    }
}
