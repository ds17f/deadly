package com.grateful.deadly.services.data

import com.grateful.deadly.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * ZIP extraction service for cross-platform file operations.
 * Extracts data.zip files containing show JSON data using expect/actual pattern.
 */
class ZipExtractionService(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val zipExtractor: ZipExtractor
) {

    companion object {
        private const val TAG = "ZipExtractionService"
    }

    data class ExtractionProgress(
        val currentItem: String = "",
        val extractedEntries: Int = 0,
        val totalEntries: Int = 0,
        val isCompleted: Boolean = false,
        val error: String? = null
    ) {
        val progressPercent: Float get() = if (totalEntries > 0) extractedEntries.toFloat() / totalEntries else 0f
    }

    sealed class ExtractionResult {
        data class Success(
            val extractedFiles: List<String>,
            val extractionDirectory: String
        ) : ExtractionResult()
        data class Error(val message: String) : ExtractionResult()
    }

    /**
     * Extract data.zip file and return list of extracted JSON files
     */
    suspend fun extractDataZip(
        zipFilePath: String,
        appFilesDir: String,
        progressCallback: ((ExtractionProgress) -> Unit)? = null
    ): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting extraction of: $zipFilePath")

            val zipPath = zipFilePath.toPath()
            if (!fileSystem.exists(zipPath)) {
                return@withContext ExtractionResult.Error("ZIP file does not exist: $zipFilePath")
            }

            // Create extraction directory
            val extractionDir = (appFilesDir.toPath() / "extracted_data")
            if (fileSystem.exists(extractionDir)) {
                // Clean up existing extraction directory
                fileSystem.deleteRecursively(extractionDir)
            }
            fileSystem.createDirectories(extractionDir)

            // Use platform-specific ZIP extractor
            val extractedFiles = zipExtractor.extractJsonFiles(
                zipPath = zipPath,
                extractionDir = extractionDir,
                progressCallback = { current, total, currentFile ->
                    progressCallback?.invoke(
                        ExtractionProgress(
                            currentItem = currentFile,
                            extractedEntries = current,
                            totalEntries = total,
                            isCompleted = current >= total
                        )
                    )
                }
            )

            Logger.d(TAG, "Extraction completed: ${extractedFiles.size} JSON files extracted")

            // Final progress
            progressCallback?.invoke(
                ExtractionProgress(
                    currentItem = "Extraction complete",
                    extractedEntries = extractedFiles.size,
                    totalEntries = extractedFiles.size,
                    isCompleted = true
                )
            )

            ExtractionResult.Success(
                extractedFiles = extractedFiles,
                extractionDirectory = extractionDir.toString()
            )

        } catch (e: Exception) {
            Logger.e(TAG, "Extraction failed", e)

            progressCallback?.invoke(
                ExtractionProgress(
                    error = e.message ?: "Extraction failed"
                )
            )

            ExtractionResult.Error("Extraction failed: ${e.message}")
        }
    }

    /**
     * Clean up extraction directory after import
     */
    suspend fun cleanupExtraction(extractionDirectory: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val extractionPath = extractionDirectory.toPath()
            if (fileSystem.exists(extractionPath)) {
                fileSystem.deleteRecursively(extractionPath)
                Logger.d(TAG, "Cleaned up extraction directory: $extractionDirectory")
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to cleanup extraction directory: $extractionDirectory", e)
            false
        }
    }

    /**
     * Get list of JSON files in extraction directory
     */
    suspend fun getExtractedJsonFiles(extractionDirectory: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val extractionPath = extractionDirectory.toPath()
            if (!fileSystem.exists(extractionPath)) {
                return@withContext emptyList()
            }

            val jsonFiles = mutableListOf<String>()
            fileSystem.list(extractionPath).forEach { path ->
                if (path.name.endsWith(".json", ignoreCase = true)) {
                    jsonFiles.add(path.toString())
                }
            }

            Logger.d(TAG, "Found ${jsonFiles.size} JSON files in extraction directory")
            jsonFiles
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to list extracted JSON files", e)
            emptyList()
        }
    }
}