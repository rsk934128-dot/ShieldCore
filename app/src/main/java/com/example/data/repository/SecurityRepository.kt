package com.example.data.repository

import android.net.Uri
import com.example.data.engine.DeviceSecurityEngine
import com.example.data.local.MalwareSignatureEntity
import com.example.data.local.ScanHistoryEntity
import com.example.data.local.SecurityDao
import com.example.data.local.SecurityDatabase
import com.example.data.model.AuditedAppInfo
import com.example.data.model.JunkCategory
import com.example.data.model.ScannedFileResult
import com.example.data.model.StorageOverview
import com.example.data.preferences.SecurityPreferences
import com.example.data.preferences.UserSecurityPrefs
import kotlinx.coroutines.flow.Flow

class SecurityRepository(
    private val dao: SecurityDao,
    private val preferences: SecurityPreferences,
    private val engine: DeviceSecurityEngine
) {
    val userPrefs: Flow<UserSecurityPrefs> = preferences.prefsFlow
    val signatureCount: Flow<Int> = dao.observeSignatureCount()
    val allSignatures: Flow<List<MalwareSignatureEntity>> = dao.observeAllSignatures()
    val recentScans: Flow<List<ScanHistoryEntity>> = dao.observeRecentScans()

    suspend fun ensureDefaultSignatures() {
        dao.insertSignatures(SecurityDatabase.DEFAULT_SIGNATURES)
    }

    suspend fun refreshSignaturesDatabase(newVersion: String) {
        dao.insertSignatures(SecurityDatabase.DEFAULT_SIGNATURES)
        preferences.setSignatureDbVersion(newVersion)
    }

    suspend fun getStorageOverview(): StorageOverview = engine.getStorageOverview()

    suspend fun scanSandbox(
        onProgress: suspend (String, ScannedFileResult) -> Unit
    ): List<ScannedFileResult> = engine.scanSandboxDirectories(onProgress)

    suspend fun scanFolderTree(
        uri: Uri,
        onProgress: suspend (String, ScannedFileResult) -> Unit
    ): List<ScannedFileResult> = engine.scanDocumentTree(uri, onProgress = onProgress)

    suspend fun scanSingleFile(uri: Uri): ScannedFileResult? = engine.scanSingleDocument(uri)

    suspend fun deleteFlaggedFile(item: ScannedFileResult): Boolean = engine.deleteFlaggedFile(item)

    suspend fun auditInstalledApps(): List<AuditedAppInfo> = engine.auditLaunchableApps()

    suspend fun analyzeJunk(): List<JunkCategory> = engine.analyzeJunkCategories()

    suspend fun cleanJunk(selectedIds: Set<String>): Long = engine.cleanSelectedJunk(selectedIds)

    suspend fun writeDiagnosticSnapshot(filesCount: Int, score: Int) {
        engine.writeDiagnosticSnapshot(filesCount, score)
    }

    suspend fun saveScanReport(report: ScanHistoryEntity) {
        dao.insertScanHistory(report)
    }

    suspend fun clearHistory() {
        dao.clearScanHistory()
    }

    suspend fun setRealTimeShield(enabled: Boolean) = preferences.setRealTimeShield(enabled)
    suspend fun setDarkTheme(enabled: Boolean) = preferences.setDarkTheme(enabled)
    suspend fun setBengaliLanguage(enabled: Boolean) = preferences.setBengaliLanguage(enabled)
    suspend fun saveSelectedFolder(uri: String, label: String) = preferences.setSelectedFolder(uri, label)
}
