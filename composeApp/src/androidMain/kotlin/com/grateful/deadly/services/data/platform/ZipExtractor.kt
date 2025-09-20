package com.grateful.deadly.services.data.platform

import com.grateful.deadly.services.data.models.ExtractedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.io.FileInputStream

/**
 * Android implementation of ZIP extraction using java.util.zip.
 *
 * This is the platform tool that handles only Android-specific ZIP operations.
 * All universal logic (progress, error handling, workflow) is in FileExtractionService.
 */
actual class ZipExtractor actual constructor() {

    actual suspend fun extractAll(
        zipPath: String,
        outputDir: String,
        progressCallback: ((current: Int, total: Int) -> Unit)?
    ): List<ExtractedFile> = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<ExtractedFile>()

        // First pass: count total entries for accurate progress tracking
        val totalEntries = countZipEntries(zipPath)

        // Second pass: extract all files
        ZipInputStream(FileInputStream(zipPath)).use { zis ->
            var currentEntry = 0
            var entry = zis.nextEntry

            while (entry != null) {
                val entryName = entry.name

                // Update progress
                progressCallback?.invoke(currentEntry, totalEntries)

                if (!entry.isDirectory) {
                    // Create output file preserving directory structure
                    val outputFile = File(outputDir, entryName)

                    // Ensure parent directories exist
                    outputFile.parentFile?.mkdirs()

                    // Extract file content
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }

                    extractedFiles.add(
                        ExtractedFile(
                            path = outputFile.absolutePath,
                            relativePath = entryName,
                            isDirectory = false,
                            sizeBytes = outputFile.length()
                        )
                    )
                } else {
                    // Handle directory entries
                    val outputDir = File(outputDir, entryName)
                    outputDir.mkdirs()

                    extractedFiles.add(
                        ExtractedFile(
                            path = outputDir.absolutePath,
                            relativePath = entryName,
                            isDirectory = true,
                            sizeBytes = 0L
                        )
                    )
                }

                currentEntry++
                entry = zis.nextEntry
            }
        }

        extractedFiles
    }

    /**
     * Count total entries in ZIP file for progress tracking.
     */
    private fun countZipEntries(zipPath: String): Int {
        var count = 0
        ZipInputStream(FileInputStream(zipPath)).use { zis ->
            while (zis.nextEntry != null) {
                count++
            }
        }
        return count
    }
}