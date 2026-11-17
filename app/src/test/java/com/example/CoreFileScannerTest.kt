package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.CoreFileScanner
import com.example.data.local.SecurityDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CoreFileScannerTest {

    @Test
    fun `scanDirectory traverses recursively, detects EICAR signature and double-extension heuristics, and safely deletes threats`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val rootDir = Files.createTempDirectory("shieldcore_scan_test").toFile()
        try {
            val dao = db.securityDao()
            dao.insertSignatures(SecurityDatabase.DEFAULT_SIGNATURES)

            val subDir = File(rootDir, "downloads/nested").apply { mkdirs() }
            val cleanFile = File(rootDir, "notes.txt").apply {
                writeText("Safe user document content")
            }

            // Standard EICAR test string (exact SHA-256 & MD5 in DEFAULT_SIGNATURES)
            val eicarPayload = "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!\$H+H*"
            val eicarFile = File(subDir, "test_payload.bin").apply {
                writeText(eicarPayload)
            }

            val spoofedFile = File(subDir, "invoice.pdf.apk").apply {
                writeText("Fake installer payload")
            }

            val scanner = CoreFileScanner(dao)
            val visitedFiles = mutableListOf<String>()
            val results = scanner.scanDirectory(rootDir) { file, _ ->
                visitedFiles.add(file.name)
            }

            assertEquals(3, results.size)
            assertTrue(visitedFiles.containsAll(listOf("notes.txt", "test_payload.bin", "invoice.pdf.apk")))

            val cleanResult = results.first { it.fileName == "notes.txt" }
            assertFalse(cleanResult.isThreat)
            assertTrue(cleanResult.sha256.length == 64)
            assertTrue(cleanResult.md5.length == 32)

            val eicarResult = results.first { it.fileName == "test_payload.bin" }
            assertTrue(eicarResult.isThreat)
            assertEquals("EICAR-Standard-AV-Test", eicarResult.threatName)
            assertEquals(
                "275a021bbfb6489e54d471899f7db9d1663fc695ec2fe2a2c4538aabf651fd0f",
                eicarResult.sha256
            )
            assertEquals("44d88612fea8a8f36de82e1278abb02f", eicarResult.md5)

            val spoofedResult = results.first { it.fileName == "invoice.pdf.apk" }
            assertTrue(spoofedResult.isThreat)
            assertEquals("Heuristic.SpoofedDoubleExtension", spoofedResult.threatName)

            // Verify threat removal works cleanly on local java.io.File
            assertTrue(scanner.removeLocalFile(eicarFile.absolutePath))
            assertFalse(eicarFile.exists())
            assertTrue(cleanFile.exists())
            assertTrue(spoofedFile.exists())
        } finally {
            rootDir.deleteRecursively()
            db.close()
        }
    }
}
