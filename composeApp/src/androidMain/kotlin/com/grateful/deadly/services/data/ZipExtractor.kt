package com.grateful.deadly.services.data

import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of ZIP extraction using java.util.zip.
 */
actual class ZipExtractor actual constructor(private val fileSystem: FileSystem) {

    actual suspend fun extractJsonFiles(
        zipPath: Path,
        extractionDir: Path,
        progressCallback: ((current: Int, total: Int, currentFile: String) -> Unit)?
    ): List<String> = withContext(Dispatchers.IO) {
        val extractedFiles = mutableListOf<String>()

        // First pass: count total entries
        var totalEntries = 0
        fileSystem.read(zipPath) {
            val zipInputStream = ZipInputStream(inputStream())
            zipInputStream.use { zis ->
                while (zis.nextEntry != null) {
                    totalEntries++
                }
            }
        }

        // Second pass: extract JSON files
        var currentEntry = 0
        fileSystem.read(zipPath) {
            val zipInputStream = ZipInputStream(inputStream())
            zipInputStream.use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name

                    progressCallback?.invoke(currentEntry, totalEntries, entryName)

                    if (!entry.isDirectory && entryName.endsWith(".json", ignoreCase = true)) {
                        // Extract JSON file
                        val fileName = entryName.substringAfterLast('/')
                        val outputPath = extractionDir / fileName

                        fileSystem.write(outputPath) {
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                write(buffer, 0, bytesRead)
                            }
                        }

                        extractedFiles.add(outputPath.toString())
                    }

                    currentEntry++
                    entry = zis.nextEntry
                }
            }
        }

        extractedFiles
    }
}