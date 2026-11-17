package com.example.data.engine

import com.example.data.local.SecurityDao
import com.example.data.model.ScannedFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class FileHashes(
    val sha256: String,
    val md5: String
)

/**
 * Core recursive file-system scanning engine built on [java.io.File].
 * Traverses directories safely (with canonical-path cycle protection and coroutine cancellation),
 * computes dual SHA-256 + MD5 digests in a single stream pass, and checks each file against
 * the local Room malware signature database plus filename/location heuristics.
 */
class CoreFileScanner(
    private val securityDao: SecurityDao,
    private val maxHashBytes: Long = 64L * 1024L * 1024L // 64 MB safety cap per file
) {
    private val doubleExtensionPattern = Regex(
        """^.+\.(pdf|doc|docx|jpg|jpeg|png|mp3|mp4|txt)\.(apk|exe|bat|sh|dex|jar|scr)$""",
        RegexOption.IGNORE_CASE
    )

    private val executableExtensions = setOf("apk", "dex", "so", "sh", "exe", "bat")

    suspend fun scanDirectories(
        directories: List<File>,
        onProgress: suspend (File, ScannedFileResult) -> Unit = { _, _ -> }
    ): List<ScannedFileResult> = withContext(Dispatchers.IO) {
        val visitedCanonicalDirs = HashSet<String>()
        val results = mutableListOf<ScannedFileResult>()

        for (root in directories) {
            coroutineContext.ensureActive()
            traverseRecursive(
                directory = root,
                visitedCanonicalDirs = visitedCanonicalDirs,
                results = results,
                onProgress = onProgress
            )
        }
        results
    }

    suspend fun scanDirectory(
        directory: File,
        onProgress: suspend (File, ScannedFileResult) -> Unit = { _, _ -> }
    ): List<ScannedFileResult> = scanDirectories(listOf(directory), onProgress)

    private suspend fun traverseRecursive(
        directory: File,
        visitedCanonicalDirs: MutableSet<String>,
        results: MutableList<ScannedFileResult>,
        onProgress: suspend (File, ScannedFileResult) -> Unit
    ) {
        coroutineContext.ensureActive()
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) return

        val canonical = runCatching { directory.canonicalPath }.getOrNull() ?: return
        if (!visitedCanonicalDirs.add(canonical)) {
            // Prevents symlink or mount-point cycles
            return
        }

        val entries = directory.listFiles() ?: return
        for (entry in entries) {
            coroutineContext.ensureActive()
            if (entry.isDirectory) {
                traverseRecursive(entry, visitedCanonicalDirs, results, onProgress)
            } else if (entry.isFile && entry.canRead()) {
                val result = inspectFile(entry)
                results.add(result)
                onProgress(entry, result)
            }
        }
    }

    suspend fun inspectFile(file: File): ScannedFileResult = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val fileSize = file.length()
        val hashes = if (fileSize <= maxHashBytes) {
            runCatching {
                file.inputStream().use { computeHashes(it) }
            }.getOrDefault(FileHashes(sha256 = "unavailable", md5 = ""))
        } else {
            FileHashes(sha256 = "skipped_large_file", md5 = "")
        }

        val signatureMatch = if (hashes.sha256 != "unavailable" && hashes.sha256 != "skipped_large_file") {
            securityDao.findSignatureByHashes(sha256 = hashes.sha256, md5 = hashes.md5)
        } else {
            null
        }

        val heuristic = evaluateHeuristics(fileName = file.name, fullPath = file.absolutePath)
        val isFlagged = signatureMatch != null || heuristic != null

        ScannedFileResult(
            fileName = file.name,
            pathOrUri = file.absolutePath,
            sizeBytes = fileSize,
            sha256 = hashes.sha256,
            md5 = hashes.md5,
            isThreat = isFlagged,
            threatName = signatureMatch?.threatName ?: heuristic,
            heuristicReason = heuristic,
            isLocalFile = true
        )
    }

    fun evaluateHeuristics(fileName: String, fullPath: String): String? {
        if (doubleExtensionPattern.matches(fileName)) {
            return "Heuristic.SpoofedDoubleExtension"
        }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val lowerPath = fullPath.lowercase()
        if (ext in executableExtensions && (lowerPath.contains("/cache/") || lowerPath.contains("/temp"))) {
            return "Heuristic.PayloadInCacheDir"
        }
        return null
    }

    fun removeLocalFile(path: String): Boolean {
        val target = File(path)
        return target.exists() && target.isFile && target.delete()
    }

    companion object {
        fun computeHashes(input: InputStream): FileHashes {
            val shaDigest = MessageDigest.getInstance("SHA-256")
            val md5Digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                shaDigest.update(buffer, 0, read)
                md5Digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
            return FileHashes(
                sha256 = shaDigest.digest().toHexString(),
                md5 = md5Digest.digest().toHexString()
            )
        }

        private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
    }
}
