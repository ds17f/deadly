package com.grateful.deadly.services.data

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.services.data.models.ExtractionProgress
import com.grateful.deadly.services.data.models.ExtractionResult
import com.grateful.deadly.services.data.platform.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Universal file extraction service using the Universal Service + Platform Tool pattern.
 *
 * This service handles all universal concerns:
 * - Workflow coordination
 * - Progress tracking
 * - Error handling
 * - Input validation
 * - Result formatting
 *
 * Platform-specific ZIP extraction is delegated to the ZipExtractor platform tool.
 */
class FileExtractionService(
    private val zipExtractor: ZipExtractor,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    companion object {
        private const val TAG = "FileExtractionService"
    }

    /**
     * Extract all files from a ZIP archive with progress tracking.
     *
     * This method demonstrates the Universal Service pattern:
     * - Universal validation, workflow, and error handling
     * - Platform-specific extraction delegated to ZipExtractor
     * - Standardized results that work across platforms
     */
    suspend fun extractFiles(
        zipPath: String,
        outputDir: String,
        progressCallback: ((ExtractionProgress) -> Unit)? = null
    ): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting extraction: $zipPath -> $outputDir")

            // Universal validation
            val zipFile = zipPath.toPath()
            if (!fileSystem.exists(zipFile)) {
                return@withContext ExtractionResult.Error("ZIP file does not exist: $zipPath")
            }

            val outputDirectory = outputDir.toPath()

            // Universal setup - clean and create extraction directory
            if (fileSystem.exists(outputDirectory)) {
                Logger.d(TAG, "Cleaning existing extraction directory")
                fileSystem.deleteRecursively(outputDirectory)
            }
            fileSystem.createDirectories(outputDirectory)

            // Universal progress tracking
            progressCallback?.invoke(ExtractionProgress.Started)

            // Delegate platform-specific extraction to tool
            Logger.d(TAG, "Delegating extraction to platform-specific ZipExtractor")
            val extractedFiles = zipExtractor.extractAll(zipPath, outputDir) { current, total ->
                // Forward progress from platform tool to universal progress tracking
                progressCallback?.invoke(ExtractionProgress.Progress(current, total))
            }

            // Universal result processing
            Logger.d(TAG, "Extraction completed: ${extractedFiles.size} files extracted")
            progressCallback?.invoke(ExtractionProgress.Completed)

            ExtractionResult.Success(extractedFiles)

        } catch (e: Exception) {
            // Universal error handling
            Logger.e(TAG, "Extraction failed", e)
            ExtractionResult.Error("Extraction failed: ${e.message}")
        }
    }

    /**
     * Clean up extraction directory after processing.
     */
    suspend fun cleanupExtraction(extractionDir: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = extractionDir.toPath()
            if (fileSystem.exists(path)) {
                fileSystem.deleteRecursively(path)
                Logger.d(TAG, "Cleaned up extraction directory: $extractionDir")
            }
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to cleanup extraction directory: $extractionDir", e)
            false
        }
    }

    /**
     * Validate that a ZIP file exists and is readable.
     */
    suspend fun validateZipFile(zipPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val zipFile = zipPath.toPath()
            fileSystem.exists(zipFile) && fileSystem.metadata(zipFile).isRegularFile
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to validate ZIP file: $zipPath", e)
            false
        }
    }
}