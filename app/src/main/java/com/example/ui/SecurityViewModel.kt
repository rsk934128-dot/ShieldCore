package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.data.engine.DeviceSecurityEngine
import com.example.data.local.MalwareSignatureEntity
import com.example.data.local.ScanHistoryEntity
import com.example.data.local.SecurityDatabase
import com.example.data.model.AppFilterMode
import com.example.data.model.AppRiskLevel
import com.example.data.model.AuditedAppInfo
import com.example.data.model.JunkCategory
import com.example.data.model.ScanPhase
import com.example.data.model.ScannedFileResult
import com.example.data.model.StorageOverview
import com.example.data.preferences.SecurityPreferences
import com.example.data.repository.SecurityRepository
import com.example.data.worker.BackgroundShieldScheduler
import com.example.data.worker.BatterySafetyInspector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val securityScore: Int = 92,
    val scanPhase: ScanPhase = ScanPhase.IDLE,
    val scanProgress: Float = 0f,
    val currentTarget: String = "",
    val filesScannedCount: Int = 0,
    val scannedFiles: List<ScannedFileResult> = emptyList(),
    val threatsDetected: List<ScannedFileResult> = emptyList(),
    val auditedApps: List<AuditedAppInfo> = emptyList(),
    val appSearchQuery: String = "",
    val appFilterMode: AppFilterMode = AppFilterMode.ALL,
    val junkCategories: List<JunkCategory> = emptyList(),
    val lastReclaimedBytes: Long = 0L,
    val storageOverview: StorageOverview = StorageOverview(),
    val realTimeShieldEnabled: Boolean = true,
    val lowBatterySafetyMode: Boolean = true,
    val autoScanIntervalHours: Int = 12,
    val lastBackgroundScanTime: Long = 0L,
    val lastBackgroundScanStatus: String = "IDLE",
    val lastBackgroundScanSummary: String = "",
    val periodicWorkState: String = "ENQUEUED",
    val immediateWorkState: String = "IDLE",
    val batteryLevelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val shouldPauseForLowBattery: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isBengali: Boolean = false,
    val signatureCount: Int = 0,
    val signatureDbVersion: String = "2026.11.17-v1",
    val signatures: List<MalwareSignatureEntity> = emptyList(),
    val scanHistory: List<ScanHistoryEntity> = emptyList(),
    val selectedFolderLabel: String? = null,
    val selectedFolderUri: String? = null,
    val statusNotice: String? = null,
    val isCleaning: Boolean = false
) {
    val isScanning: Boolean
        get() = scanPhase == ScanPhase.SCANNING_STORAGE ||
            scanPhase == ScanPhase.AUDITING_APPS ||
            scanPhase == ScanPhase.CHECKING_CACHE

    val totalJunkBytes: Long
        get() = junkCategories.sumOf { it.sizeBytes }

    val selectedJunkBytes: Long
        get() = junkCategories.filter { it.isSelected }.sumOf { it.sizeBytes }

    val riskyAppsCount: Int
        get() = auditedApps.count { it.riskLevel != AppRiskLevel.SAFE }

    val filteredApps: List<AuditedAppInfo>
        get() = auditedApps.filter { app ->
            val matchesQuery = appSearchQuery.isBlank() ||
                app.appName.contains(appSearchQuery, ignoreCase = true) ||
                app.packageName.contains(appSearchQuery, ignoreCase = true)
            val matchesFilter = when (appFilterMode) {
                AppFilterMode.ALL -> true
                AppFilterMode.RISKY_ONLY -> app.riskLevel != AppRiskLevel.SAFE
                AppFilterMode.PRIVACY_SENSORS -> app.sensitivePermissions.any {
                    it.contains("CAMERA") || it.contains("AUDIO") || it.contains("LOCATION")
                }
            }
            matchesQuery && matchesFilter
        }
}

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SecurityRepository
    private val scheduler = BackgroundShieldScheduler(application)
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var lastSyncedScheduleKey: Triple<Boolean, Boolean, Int>? = null

    init {
        val db = SecurityDatabase.getInstance(application)
        val prefs = SecurityPreferences(application)
        val engine = DeviceSecurityEngine(application, db.securityDao())
        repository = SecurityRepository(db.securityDao(), prefs, engine)

        refreshBatteryStatus()

        viewModelScope.launch {
            repository.ensureDefaultSignatures()
            refreshDiagnostics()
        }

        viewModelScope.launch {
            combine(
                repository.userPrefs,
                repository.signatureCount,
                repository.recentScans,
                repository.allSignatures
            ) { prefsVal, sigCount, history, sigs ->
                Quadruple(prefsVal, sigCount, history, sigs)
            }.collect { (prefsVal, sigCount, history, sigs) ->
                val scheduleKey = Triple(
                    prefsVal.realTimeShieldEnabled,
                    prefsVal.lowBatterySafetyMode,
                    prefsVal.autoScanIntervalHours
                )
                if (scheduleKey != lastSyncedScheduleKey) {
                    lastSyncedScheduleKey = scheduleKey
                    scheduler.syncPeriodicShield(
                        enabled = prefsVal.realTimeShieldEnabled,
                        lowBatterySafetyMode = prefsVal.lowBatterySafetyMode,
                        intervalHours = prefsVal.autoScanIntervalHours
                    )
                }

                val battery = BatterySafetyInspector.inspect(
                    context = getApplication(),
                    safetyModeEnabled = prefsVal.lowBatterySafetyMode
                )

                _uiState.update { state ->
                    val recalculatedScore = computeScore(
                        shieldEnabled = prefsVal.realTimeShieldEnabled,
                        threats = state.threatsDetected.size,
                        apps = state.auditedApps,
                        junkBytes = state.totalJunkBytes
                    )
                    state.copy(
                        realTimeShieldEnabled = prefsVal.realTimeShieldEnabled,
                        lowBatterySafetyMode = prefsVal.lowBatterySafetyMode,
                        autoScanIntervalHours = prefsVal.autoScanIntervalHours,
                        lastBackgroundScanTime = prefsVal.lastBackgroundScanTime,
                        lastBackgroundScanStatus = prefsVal.lastBackgroundScanStatus,
                        lastBackgroundScanSummary = prefsVal.lastBackgroundScanSummary,
                        batteryLevelPercent = battery.levelPercent,
                        isCharging = battery.isCharging,
                        isPowerSaveMode = battery.isPowerSaveMode,
                        shouldPauseForLowBattery = battery.shouldPauseAutoScan,
                        isDarkTheme = prefsVal.darkThemeEnabled,
                        isBengali = prefsVal.bengaliLanguage,
                        signatureDbVersion = prefsVal.signatureDbVersion,
                        selectedFolderLabel = prefsVal.lastSelectedFolderLabel,
                        selectedFolderUri = prefsVal.lastSelectedFolderUri,
                        signatureCount = sigCount,
                        scanHistory = history,
                        signatures = sigs,
                        securityScore = recalculatedScore
                    )
                }
            }
        }

        viewModelScope.launch {
            scheduler.observePeriodicStatus().collect { info ->
                _uiState.update { state ->
                    state.copy(
                        periodicWorkState = info?.state?.name ?: if (state.realTimeShieldEnabled) "ENQUEUED" else "CANCELLED"
                    )
                }
            }
        }

        viewModelScope.launch {
            scheduler.observeImmediateStatus().collect { info ->
                val workState = info?.state?.name ?: "IDLE"
                _uiState.update { it.copy(immediateWorkState = workState) }
                if (info?.state == WorkInfo.State.SUCCEEDED) {
                    refreshBatteryStatus()
                }
            }
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val storage = repository.getStorageOverview()
            val junk = repository.analyzeJunk()
            val apps = repository.auditInstalledApps()
            _uiState.update { state ->
                val newScore = computeScore(
                    shieldEnabled = state.realTimeShieldEnabled,
                    threats = state.threatsDetected.size,
                    apps = apps,
                    junkBytes = junk.sumOf { it.sizeBytes }
                )
                state.copy(
                    storageOverview = storage,
                    junkCategories = junk,
                    auditedApps = apps,
                    securityScore = newScore
                )
            }
        }
    }

    fun startFullScan() {
        if (_uiState.value.isScanning) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.SCANNING_STORAGE,
                    scanProgress = 0.08f,
                    currentTarget = "Initializing SHA-256 engine...",
                    filesScannedCount = 0,
                    scannedFiles = emptyList(),
                    threatsDetected = emptyList(),
                    statusNotice = null
                )
            }

            val collectedResults = mutableListOf<ScannedFileResult>()

            // Step 1: Scan app sandbox files
            val sandboxResults = repository.scanSandbox { name, item ->
                collectedResults.add(item)
                _uiState.update { s ->
                    val threats = collectedResults.filter { it.isThreat }
                    s.copy(
                        currentTarget = name,
                        filesScannedCount = collectedResults.size,
                        scannedFiles = collectedResults.takeLast(50).reversed(),
                        threatsDetected = threats,
                        scanProgress = (0.12f + (collectedResults.size.coerceAtMost(30) / 30f) * 0.28f)
                    )
                }
                delay(40)
            }

            // Step 1b: If user previously picked a SAF folder, scan it too
            val treeUriStr = _uiState.value.selectedFolderUri
            if (!treeUriStr.isNullOrBlank()) {
                runCatching {
                    val uri = Uri.parse(treeUriStr)
                    repository.scanFolderTree(uri) { name, item ->
                        collectedResults.add(item)
                        _uiState.update { s ->
                            val threats = collectedResults.filter { it.isThreat }
                            s.copy(
                                currentTarget = name,
                                filesScannedCount = collectedResults.size,
                                scannedFiles = collectedResults.takeLast(50).reversed(),
                                threatsDetected = threats,
                                scanProgress = (0.40f + (collectedResults.size.coerceAtMost(60) / 60f) * 0.15f)
                                    .coerceAtMost(0.55f)
                            )
                        }
                    }
                }
            }

            // Step 2: Audit installed launchable apps
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.AUDITING_APPS,
                    scanProgress = 0.62f,
                    currentTarget = "Inspecting PackageManager manifests..."
                )
            }
            val apps = repository.auditInstalledApps()
            delay(280)

            // Step 3: Analyze storage & cache
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.CHECKING_CACHE,
                    scanProgress = 0.85f,
                    currentTarget = "Calculating StatFs & cache footprint...",
                    auditedApps = apps
                )
            }
            val storage = repository.getStorageOverview()
            val junk = repository.analyzeJunk()
            val junkBytes = junk.sumOf { it.sizeBytes }
            val threats = collectedResults.filter { it.isThreat }
            val finalScore = computeScore(
                shieldEnabled = _uiState.value.realTimeShieldEnabled,
                threats = threats.size,
                apps = apps,
                junkBytes = junkBytes
            )

            repository.writeDiagnosticSnapshot(collectedResults.size.coerceAtLeast(sandboxResults.size), finalScore)

            val refreshedJunk = repository.analyzeJunk()
            val summaryText = if (_uiState.value.isBengali) {
                "${collectedResults.size} ফাইল ও ${apps.size} অ্যাপ পরীক্ষা সম্পন্ন • ${threats.size} ঝুঁকি"
            } else {
                "Checked ${collectedResults.size} files & ${apps.size} apps • ${threats.size} threats"
            }

            repository.saveScanReport(
                ScanHistoryEntity(
                    scanType = if (treeUriStr != null) "Deep + Folder Scan" else "Full System Audit",
                    filesScanned = collectedResults.size,
                    appsAudited = apps.size,
                    threatsFound = threats.size,
                    junkBytesFound = refreshedJunk.sumOf { it.sizeBytes },
                    securityScore = finalScore,
                    summary = summaryText
                )
            )

            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.COMPLETE,
                    scanProgress = 1f,
                    currentTarget = summaryText,
                    storageOverview = storage,
                    junkCategories = refreshedJunk,
                    auditedApps = apps,
                    securityScore = finalScore
                )
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                scanPhase = ScanPhase.IDLE,
                scanProgress = 0f,
                currentTarget = ""
            )
        }
    }

    fun scanUserFolder(uri: Uri, label: String) {
        viewModelScope.launch {
            repository.saveSelectedFolder(uri.toString(), label)
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.SCANNING_STORAGE,
                    scanProgress = 0.15f,
                    currentTarget = label,
                    selectedFolderLabel = label,
                    selectedFolderUri = uri.toString(),
                    statusNotice = null
                )
            }

            val scanned = mutableListOf<ScannedFileResult>()
            repository.scanFolderTree(uri) { name, item ->
                scanned.add(item)
                _uiState.update { s ->
                    s.copy(
                        currentTarget = name,
                        filesScannedCount = scanned.size,
                        scannedFiles = scanned.takeLast(60).reversed(),
                        threatsDetected = scanned.filter { it.isThreat },
                        scanProgress = (0.2f + (scanned.size.coerceAtMost(80) / 80f) * 0.75f).coerceAtMost(0.95f)
                    )
                }
            }

            val threats = scanned.filter { it.isThreat }
            val finalScore = computeScore(
                shieldEnabled = _uiState.value.realTimeShieldEnabled,
                threats = threats.size,
                apps = _uiState.value.auditedApps,
                junkBytes = _uiState.value.totalJunkBytes
            )
            val summaryText = if (_uiState.value.isBengali) {
                "ফোল্ডার '$label': ${scanned.size} ফাইল স্ক্যান, ${threats.size} হুমকি"
            } else {
                "Folder '$label': ${scanned.size} files scanned, ${threats.size} threats"
            }

            repository.saveScanReport(
                ScanHistoryEntity(
                    scanType = "Folder Scan",
                    filesScanned = scanned.size,
                    appsAudited = _uiState.value.auditedApps.size,
                    threatsFound = threats.size,
                    junkBytesFound = _uiState.value.totalJunkBytes,
                    securityScore = finalScore,
                    summary = summaryText
                )
            )

            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.COMPLETE,
                    scanProgress = 1f,
                    currentTarget = summaryText,
                    securityScore = finalScore,
                    statusNotice = summaryText
                )
            }
        }
    }

    fun scanSinglePickedFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scanPhase = ScanPhase.SCANNING_STORAGE,
                    scanProgress = 0.5f,
                    currentTarget = "Hashing selected file..."
                )
            }
            val result = repository.scanSingleFile(uri)
            if (result != null) {
                val updatedList = (listOf(result) + _uiState.value.scannedFiles).take(60)
                val threats = updatedList.filter { it.isThreat }
                val score = computeScore(
                    shieldEnabled = _uiState.value.realTimeShieldEnabled,
                    threats = threats.size,
                    apps = _uiState.value.auditedApps,
                    junkBytes = _uiState.value.totalJunkBytes
                )
                val msg = if (result.isThreat) {
                    "Threat matched: ${result.threatName} (${result.fileName})"
                } else {
                    "Verified clean: ${result.fileName} (SHA-256 ${result.sha256.take(12)}...)"
                }
                repository.saveScanReport(
                    ScanHistoryEntity(
                        scanType = "File Inspection",
                        filesScanned = 1,
                        appsAudited = _uiState.value.auditedApps.size,
                        threatsFound = if (result.isThreat) 1 else 0,
                        junkBytesFound = _uiState.value.totalJunkBytes,
                        securityScore = score,
                        summary = msg
                    )
                )
                _uiState.update {
                    it.copy(
                        scanPhase = ScanPhase.COMPLETE,
                        scanProgress = 1f,
                        filesScannedCount = it.filesScannedCount + 1,
                        scannedFiles = updatedList,
                        threatsDetected = threats,
                        currentTarget = msg,
                        statusNotice = msg,
                        securityScore = score
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        scanPhase = ScanPhase.IDLE,
                        scanProgress = 0f,
                        statusNotice = "Could not open file stream for hashing"
                    )
                }
            }
        }
    }

    fun toggleJunkCategory(id: String) {
        _uiState.update { state ->
            state.copy(
                junkCategories = state.junkCategories.map {
                    if (it.id == id) it.copy(isSelected = !it.isSelected) else it
                }
            )
        }
    }

    fun cleanSelectedJunk() {
        val selectedIds = _uiState.value.junkCategories.filter { it.isSelected }.map { it.id }.toSet()
        if (selectedIds.isEmpty() || _uiState.value.isCleaning) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            delay(300)
            val freed = repository.cleanJunk(selectedIds)
            val refreshedJunk = repository.analyzeJunk()
            val storage = repository.getStorageOverview()
            val newScore = computeScore(
                shieldEnabled = _uiState.value.realTimeShieldEnabled,
                threats = _uiState.value.threatsDetected.size,
                apps = _uiState.value.auditedApps,
                junkBytes = refreshedJunk.sumOf { it.sizeBytes }
            )
            _uiState.update {
                it.copy(
                    isCleaning = false,
                    junkCategories = refreshedJunk,
                    storageOverview = storage,
                    lastReclaimedBytes = freed,
                    securityScore = newScore,
                    statusNotice = if (it.isBengali) {
                        "সফলভাবে ${formatBytes(freed)} জাঙ্ক পরিষ্কার করা হয়েছে"
                    } else {
                        "Cleaned ${formatBytes(freed)} of temporary cache"
                    }
                )
            }
        }
    }

    fun toggleRealTimeShield(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRealTimeShield(enabled)
            val state = _uiState.value
            scheduler.syncPeriodicShield(
                enabled = enabled,
                lowBatterySafetyMode = state.lowBatterySafetyMode,
                intervalHours = state.autoScanIntervalHours
            )
            _uiState.update {
                it.copy(
                    statusNotice = if (it.isBengali) {
                        if (enabled) "WorkManager ব্যাকগ্রাউন্ড অটো-স্ক্যান চালু হয়েছে" else "ব্যাকগ্রাউন্ড অটো-স্ক্যান বিরত রাখা হয়েছে"
                    } else {
                        if (enabled) "WorkManager background auto-scan enabled" else "Background auto-scan paused"
                    }
                )
            }
        }
    }

    fun toggleLowBatterySafetyMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLowBatterySafetyMode(enabled)
            val state = _uiState.value
            scheduler.syncPeriodicShield(
                enabled = state.realTimeShieldEnabled,
                lowBatterySafetyMode = enabled,
                intervalHours = state.autoScanIntervalHours
            )
            val battery = BatterySafetyInspector.inspect(
                context = getApplication(),
                safetyModeEnabled = enabled
            )
            _uiState.update {
                it.copy(
                    batteryLevelPercent = battery.levelPercent,
                    isCharging = battery.isCharging,
                    isPowerSaveMode = battery.isPowerSaveMode,
                    shouldPauseForLowBattery = battery.shouldPauseAutoScan,
                    statusNotice = if (it.isBengali) {
                        if (enabled) "লো-ব্যাটারি সেফটি মোড চালু (<২০% বা সেভারে অটো-স্ক্যান বিরত)" else "লো-ব্যাটারি সেফটি মোড বন্ধ"
                    } else {
                        if (enabled) "Low-Battery Safety Mode enabled (<20% or Power Saver guard)" else "Low-Battery Safety Mode disabled"
                    }
                )
            }
        }
    }

    fun setAutoScanIntervalHours(hours: Int) {
        viewModelScope.launch {
            val safeHours = hours.coerceIn(6, 24)
            repository.setAutoScanIntervalHours(safeHours)
            val state = _uiState.value
            scheduler.syncPeriodicShield(
                enabled = state.realTimeShieldEnabled,
                lowBatterySafetyMode = state.lowBatterySafetyMode,
                intervalHours = safeHours
            )
            _uiState.update {
                it.copy(
                    statusNotice = if (it.isBengali) {
                        "অটো-স্ক্যান সময়সূচি প্রতি $safeHours ঘণ্টা নির্ধারণ করা হয়েছে"
                    } else {
                        "Background auto-scan scheduled every ${safeHours}h"
                    }
                )
            }
        }
    }

    fun runBackgroundCheckNow() {
        val state = _uiState.value
        if (!state.realTimeShieldEnabled) {
            _uiState.update {
                it.copy(
                    statusNotice = if (it.isBengali) {
                        "প্রথমে রিয়েল-টাইম ব্যাকগ্রাউন্ড শিল্ড চালু করুন"
                    } else {
                        "Enable Real-time Shield first to run background auto-scan"
                    }
                )
            }
            return
        }
        val battery = BatterySafetyInspector.inspect(
            context = getApplication(),
            safetyModeEnabled = state.lowBatterySafetyMode
        )
        _uiState.update {
            it.copy(
                batteryLevelPercent = battery.levelPercent,
                isCharging = battery.isCharging,
                isPowerSaveMode = battery.isPowerSaveMode,
                shouldPauseForLowBattery = battery.shouldPauseAutoScan
            )
        }
        if (battery.shouldPauseAutoScan) {
            _uiState.update {
                it.copy(
                    statusNotice = if (it.isBengali) {
                        "লো-ব্যাটারি সেফটি সক্রিয় (${battery.levelPercent}%) — ব্যাটারি চার্জ হলে অটো-স্ক্যান চলবে"
                    } else {
                        "Paused by Low-Battery Safety (${battery.levelPercent}%) — resumes when battery recovers"
                    }
                )
            }
            return
        }
        scheduler.triggerImmediateCheck(lowBatterySafetyMode = state.lowBatterySafetyMode)
        _uiState.update {
            it.copy(
                statusNotice = if (it.isBengali) {
                    "WorkManager ব্যাকগ্রাউন্ড অটো-স্ক্যান কিউতে যুক্ত হয়েছে"
                } else {
                    "Background auto-scan queued via WorkManager"
                }
            )
        }
    }

    fun refreshBatteryStatus() {
        val battery = BatterySafetyInspector.inspect(
            context = getApplication(),
            safetyModeEnabled = _uiState.value.lowBatterySafetyMode
        )
        _uiState.update {
            it.copy(
                batteryLevelPercent = battery.levelPercent,
                isCharging = battery.isCharging,
                isPowerSaveMode = battery.isPowerSaveMode,
                shouldPauseForLowBattery = battery.shouldPauseAutoScan
            )
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            repository.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            repository.setBengaliLanguage(!_uiState.value.isBengali)
        }
    }

    fun refreshSignatures() {
        viewModelScope.launch {
            val ver = "2026.11.17-v${(1..9).random()}"
            repository.refreshSignaturesDatabase(ver)
            _uiState.update {
                it.copy(
                    statusNotice = if (it.isBengali) {
                        "Room সিগনেচার ডেটাবেস আপডেট হয়েছে ($ver)"
                    } else {
                        "Local SHA-256 signature database synced ($ver)"
                    }
                )
            }
        }
    }

    fun setAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun setAppFilterMode(mode: AppFilterMode) {
        _uiState.update { it.copy(appFilterMode = mode) }
    }

    fun removeThreatFile(item: ScannedFileResult) {
        viewModelScope.launch {
            val removed = repository.deleteFlaggedFile(item)
            if (removed) {
                val remaining = _uiState.value.scannedFiles.filterNot { it.pathOrUri == item.pathOrUri }
                val remainingThreats = remaining.filter { it.isThreat }
                val junk = repository.analyzeJunk()
                val newScore = computeScore(
                    shieldEnabled = _uiState.value.realTimeShieldEnabled,
                    threats = remainingThreats.size,
                    apps = _uiState.value.auditedApps,
                    junkBytes = junk.sumOf { it.sizeBytes }
                )
                _uiState.update {
                    it.copy(
                        scannedFiles = remaining,
                        threatsDetected = remainingThreats,
                        junkCategories = junk,
                        securityScore = newScore,
                        statusNotice = if (it.isBengali) {
                            "সন্দেহজনক ফাইল '${item.fileName}' মুছে ফেলা হয়েছে"
                        } else {
                            "Removed flagged file '${item.fileName}'"
                        }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        statusNotice = if (it.isBengali) {
                            "ফাইল '${item.fileName}' মোছা যায়নি"
                        } else {
                            "Could not remove '${item.fileName}'"
                        }
                    )
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(statusNotice = null) }
    }

    fun showNotice(message: String) {
        _uiState.update { it.copy(statusNotice = message) }
    }

    private fun computeScore(
        shieldEnabled: Boolean,
        threats: Int,
        apps: List<AuditedAppInfo>,
        junkBytes: Long
    ): Int {
        var score = 100
        if (!shieldEnabled) score -= 18
        score -= (threats * 25)
        val highRiskApps = apps.count { it.riskLevel == AppRiskLevel.HIGH }
        val elevatedApps = apps.count { it.riskLevel == AppRiskLevel.ELEVATED }
        score -= (highRiskApps * 4).coerceAtMost(16)
        score -= (elevatedApps * 1).coerceAtMost(8)
        if (junkBytes > 10L * 1024L * 1024L) score -= 4
        return score.coerceIn(15, 100)
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIdx = 0
    while (value >= 1024.0 && unitIdx < units.lastIndex) {
        value /= 1024.0
        unitIdx++
    }
    return if (unitIdx == 0) "${value.toInt()} ${units[unitIdx]}"
    else String.format("%.1f %s", value, units[unitIdx])
}
