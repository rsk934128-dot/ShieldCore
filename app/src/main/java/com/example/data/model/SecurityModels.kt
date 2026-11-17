package com.example.data.model

enum class ScanPhase {
    IDLE,
    SCANNING_STORAGE,
    AUDITING_APPS,
    CHECKING_CACHE,
    COMPLETE
}

enum class AppRiskLevel {
    SAFE,
    ELEVATED,
    HIGH
}

enum class AppFilterMode {
    ALL,
    RISKY_ONLY,
    PRIVACY_SENSORS
}

data class ScannedFileResult(
    val fileName: String,
    val pathOrUri: String,
    val sizeBytes: Long,
    val sha256: String,
    val md5: String = "",
    val isThreat: Boolean,
    val threatName: String? = null,
    val heuristicReason: String? = null,
    val isLocalFile: Boolean = false
)

data class AuditedAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val requestedPermissions: List<String>,
    val sensitivePermissions: List<String>,
    val riskLevel: AppRiskLevel,
    val riskScorePenalty: Int
)

data class JunkCategory(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descriptionEn: String,
    val descriptionBn: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val isSelected: Boolean = true
)

data class StorageOverview(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
