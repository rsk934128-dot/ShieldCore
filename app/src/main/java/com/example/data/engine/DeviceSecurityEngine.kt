package com.example.data.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.data.local.SecurityDao
import com.example.data.model.AppRiskLevel
import com.example.data.model.AuditedAppInfo
import com.example.data.model.JunkCategory
import com.example.data.model.ScannedFileResult
import com.example.data.model.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeviceSecurityEngine(
    private val context: Context,
    private val securityDao: SecurityDao,
    private val coreFileScanner: CoreFileScanner = CoreFileScanner(securityDao)
) {
    private val highRiskPermissions = setOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.READ_CONTACTS,
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    suspend fun getStorageOverview(): StorageOverview = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            StorageOverview(
                totalBytes = total,
                freeBytes = free,
                usedBytes = (total - free).coerceAtLeast(0L)
            )
        } catch (_: Exception) {
            StorageOverview()
        }
    }

    suspend fun scanSandboxDirectories(
        onProgress: suspend (String, ScannedFileResult) -> Unit
    ): List<ScannedFileResult> = withContext(Dispatchers.IO) {
        val roots = listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir,
            context.getExternalFilesDir(null)
        )
        coreFileScanner.scanDirectories(roots) { file, result ->
            onProgress(file.name, result)
        }
    }

    suspend fun scanDirectory(
        directory: File,
        onProgress: suspend (String, ScannedFileResult) -> Unit
    ): List<ScannedFileResult> = withContext(Dispatchers.IO) {
        coreFileScanner.scanDirectory(directory) { file, result ->
            onProgress(file.name, result)
        }
    }

    suspend fun scanDocumentTree(
        treeUri: Uri,
        maxFiles: Int = 200,
        onProgress: suspend (String, ScannedFileResult) -> Unit
    ): List<ScannedFileResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFileResult>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque<String>()
        queue.add(rootDocId)

        while (queue.isNotEmpty() && results.size < maxFiles) {
            val currentDocId = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext() && results.size < maxFiles) {
                        val docId = cursor.getString(idIdx)
                        val name = cursor.getString(nameIdx) ?: "unknown"
                        val mime = cursor.getString(mimeIdx) ?: ""
                        val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(docId)
                        } else {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val hashes = runCatching {
                                context.contentResolver.openInputStream(docUri)?.use {
                                    CoreFileScanner.computeHashes(it)
                                }
                            }.getOrNull() ?: FileHashes(sha256 = "unreadable", md5 = "")

                            val match = if (hashes.sha256 != "unreadable") {
                                securityDao.findSignatureByHashes(hashes.sha256, hashes.md5)
                            } else {
                                null
                            }
                            val heuristic = coreFileScanner.evaluateHeuristics(name, docUri.toString())
                            val isFlagged = match != null || heuristic != null

                            val item = ScannedFileResult(
                                fileName = name,
                                pathOrUri = docUri.toString(),
                                sizeBytes = size,
                                sha256 = hashes.sha256,
                                md5 = hashes.md5,
                                isThreat = isFlagged,
                                threatName = match?.threatName ?: heuristic,
                                heuristicReason = heuristic,
                                isLocalFile = false
                            )
                            results.add(item)
                            onProgress(name, item)
                        }
                    }
                }
            }
        }
        results
    }

    suspend fun scanSingleDocument(uri: Uri): ScannedFileResult? = withContext(Dispatchers.IO) {
        var displayName = uri.lastPathSegment ?: "selected_file"
        var size = 0L

        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        displayName = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        }

        val hashes = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                CoreFileScanner.computeHashes(it)
            }
        }.getOrNull() ?: return@withContext null

        val match = securityDao.findSignatureByHashes(hashes.sha256, hashes.md5)
        val heuristic = coreFileScanner.evaluateHeuristics(displayName, uri.toString())
        val isFlagged = match != null || heuristic != null

        ScannedFileResult(
            fileName = displayName,
            pathOrUri = uri.toString(),
            sizeBytes = size,
            sha256 = hashes.sha256,
            md5 = hashes.md5,
            isThreat = isFlagged,
            threatName = match?.threatName ?: heuristic,
            heuristicReason = heuristic,
            isLocalFile = false
        )
    }

    suspend fun deleteFlaggedFile(item: ScannedFileResult): Boolean = withContext(Dispatchers.IO) {
        if (item.isLocalFile) {
            coreFileScanner.removeLocalFile(item.pathOrUri)
        } else {
            runCatching {
                DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(item.pathOrUri))
            }.getOrDefault(false)
        }
    }

    suspend fun auditLaunchableApps(): List<AuditedAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val uniquePackages = resolveInfos
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()

        uniquePackages.mapNotNull { pkgName ->
            runCatching {
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        pkgName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS)
                }

                val appInfo = pkgInfo.applicationInfo
                val appLabel = appInfo?.loadLabel(pm)?.toString() ?: pkgName
                val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
                val sensitive = requested.filter { it in highRiskPermissions }.distinct()

                val riskLevel = when {
                    !isSystem && sensitive.size >= 4 -> AppRiskLevel.HIGH
                    !isSystem && sensitive.size >= 2 -> AppRiskLevel.ELEVATED
                    else -> AppRiskLevel.SAFE
                }

                val penalty = when (riskLevel) {
                    AppRiskLevel.HIGH -> 6
                    AppRiskLevel.ELEVATED -> 2
                    AppRiskLevel.SAFE -> 0
                }

                AuditedAppInfo(
                    packageName = pkgName,
                    appName = appLabel,
                    versionName = pkgInfo.versionName ?: "1.0",
                    isSystemApp = isSystem,
                    requestedPermissions = requested,
                    sensitivePermissions = sensitive,
                    riskLevel = riskLevel,
                    riskScorePenalty = penalty
                )
            }.getOrNull()
        }.sortedWith(
            compareByDescending<AuditedAppInfo> { it.riskLevel.ordinal }
                .thenByDescending { it.sensitivePermissions.size }
                .thenBy { it.appName.lowercase() }
        )
    }

    suspend fun analyzeJunkCategories(): List<JunkCategory> = withContext(Dispatchers.IO) {
        val appCacheStats = measureDirectory(context.cacheDir)
        val codeCacheStats = measureDirectory(context.codeCacheDir)
        val extCacheStats = context.externalCacheDir?.let { measureDirectory(it) } ?: (0L to 0)
        val scanTempDir = File(context.filesDir, "scan_reports")
        val tempLogStats = if (scanTempDir.exists()) measureDirectory(scanTempDir) else (0L to 0)

        listOf(
            JunkCategory(
                id = "app_cache",
                titleEn = "App Cache & Buffers",
                titleBn = "অ্যাপ ক্যাশ ও বাফার",
                descriptionEn = "Internal temporary cache files safe to remove",
                descriptionBn = "ইন্টারনাল টেম্পোরারি ক্যাশ যা নিরাপদে মুছে ফেলা যায়",
                sizeBytes = appCacheStats.first + extCacheStats.first,
                fileCount = appCacheStats.second + extCacheStats.second,
                isSelected = true
            ),
            JunkCategory(
                id = "compiled_cache",
                titleEn = "Runtime Code Cache",
                titleBn = "রানটাইম কোড ক্যাশ",
                descriptionEn = "Temporary shader and JIT compilation artifacts",
                descriptionBn = "টেম্পোরারি শেডার ও রানটাইম কম্পাইলেশন ফাইল",
                sizeBytes = codeCacheStats.first,
                fileCount = codeCacheStats.second,
                isSelected = true
            ),
            JunkCategory(
                id = "scan_temp",
                titleEn = "Diagnostic Scan Logs",
                titleBn = "ডায়াগনস্টিক স্ক্যান লগ",
                descriptionEn = "Archived telemetry snapshots from past scans",
                descriptionBn = "পূর্ববর্তী স্ক্যানের অপ্রয়োজনীয় টেলিমেট্রি লগ",
                sizeBytes = tempLogStats.first,
                fileCount = tempLogStats.second,
                isSelected = true
            )
        )
    }

    suspend fun writeDiagnosticSnapshot(filesCount: Int, score: Int) = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "scan_reports").apply { mkdirs() }
            val logFile = File(dir, "diag_${System.currentTimeMillis()}.log")
            logFile.writeText(
                "ShieldCore Diagnostic Snapshot\nfiles=$filesCount\nscore=$score\ntimestamp=${System.currentTimeMillis()}\n"
            )
        }
    }

    suspend fun cleanSelectedJunk(selectedIds: Set<String>): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        if ("app_cache" in selectedIds) {
            reclaimed += deleteDirectoryContents(context.cacheDir)
            context.externalCacheDir?.let { reclaimed += deleteDirectoryContents(it) }
        }
        if ("compiled_cache" in selectedIds) {
            reclaimed += deleteDirectoryContents(context.codeCacheDir)
        }
        if ("scan_temp" in selectedIds) {
            val scanTempDir = File(context.filesDir, "scan_reports")
            if (scanTempDir.exists()) {
                reclaimed += deleteDirectoryContents(scanTempDir)
            }
        }
        reclaimed
    }

    private fun measureDirectory(dir: File): Pair<Long, Int> {
        var bytes = 0L
        var count = 0
        val children = dir.listFiles() ?: return 0L to 0
        for (child in children) {
            if (child.isDirectory) {
                val sub = measureDirectory(child)
                bytes += sub.first
                count += sub.second
            } else if (child.isFile) {
                bytes += child.length()
                count++
            }
        }
        return bytes to count
    }

    private fun deleteDirectoryContents(dir: File): Long {
        var freed = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            if (child.isDirectory) {
                freed += deleteDirectoryContents(child)
                child.delete()
            } else if (child.isFile) {
                val len = child.length()
                if (child.delete()) {
                    freed += len
                }
            }
        }
        return freed
    }
}
