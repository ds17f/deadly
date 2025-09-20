package com.grateful.deadly.services.data

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.services.data.models.ExtractionProgress
import com.grateful.deadly.services.data.models.ExtractionResult
import com.grateful.deadly.services.data.models.ImportProgress
import com.grateful.deadly.services.data.models.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates the complete data synchronization workflow using the new architecture.
 *
 * This service demonstrates the Universal Service + Platform Tool pattern at a higher level:
 * - Coordinates universal workflow: download → extract → import → cleanup
 * - Uses DownloadService (already well-designed)
 * - Uses FileExtractionService (new universal service)
 * - Uses DataImportService (new universal service)
 * - Provides unified progress tracking and error handling
 */
class DataSyncOrchestrator(
    private val downloadService: DownloadService,
    private val fileDiscoveryService: FileDiscoveryService,
    private val fileExtractionService: FileExtractionService,
    private val dataImportService: DataImportService,
    private val getAppFilesDir: () -> String
) {

    companion object {
        private const val TAG = "DataSyncOrchestrator"
    }

    private val _progress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val progress: Flow<SyncProgress> = _progress.asStateFlow()

    /**
     * Synchronize data: download → extract → import → cleanup.
     * This is the main entry point replacing the old DataImportService workflow.
     */
    suspend fun syncData(): SyncResult {
        return try {
            Logger.d(TAG, "Starting data synchronization")

            // Check if import is needed
            val showCount = dataImportService.getShowCount()
            if (showCount > 0) {
                Logger.d(TAG, "Data already exists ($showCount shows), skipping sync")
                return SyncResult.AlreadyExists(showCount.toInt())
            }

            val appFilesDir = getAppFilesDir()

            // Phase 1: Download data.zip
            _progress.value = SyncProgress.Downloading
            val dataZipPath = downloadDataZip(appFilesDir)
                ?: return SyncResult.Error("Failed to download data.zip")

            // Phase 2: Extract all files
            _progress.value = SyncProgress.Extracting
            val extractedFiles = extractDataZip(dataZipPath, appFilesDir)
                ?: return SyncResult.Error("Failed to extract data.zip")

            // Phase 3: Import show data
            _progress.value = SyncProgress.Importing(0, extractedFiles.size)
            val importedCount = importShowData(extractedFiles)
                ?: return SyncResult.Error("Failed to import show data")

            // Phase 4: Cleanup
            cleanupExtractionDirectory("$appFilesDir/extracted_data")

            _progress.value = SyncProgress.Idle
            Logger.d(TAG, "Data synchronization completed: $importedCount shows imported")
            SyncResult.Success(importedCount)

        } catch (e: Exception) {
            Logger.e(TAG, "Data synchronization failed", e)
            _progress.value = SyncProgress.Idle
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }

    /**
     * Force refresh data by clearing existing data and re-importing.
     */
    suspend fun forceRefreshData(): SyncResult {
        return try {
            Logger.d(TAG, "Force refreshing data")

            // Clear existing data
            _progress.value = SyncProgress.Clearing
            dataImportService.clearAllShows()

            // Delete cached data.zip to force re-download
            deleteCachedDataFile()

            // Perform full sync
            syncData()

        } catch (e: Exception) {
            Logger.e(TAG, "Force refresh failed", e)
            _progress.value = SyncProgress.Idle
            SyncResult.Error("Force refresh failed: ${e.message}")
        }
    }

    /**
     * Clear all data without re-importing.
     */
    suspend fun clearAllData(): SyncResult {
        return try {
            Logger.d(TAG, "Clearing all data")
            _progress.value = SyncProgress.Clearing

            dataImportService.clearAllShows()

            _progress.value = SyncProgress.Idle
            Logger.d(TAG, "All data cleared")
            SyncResult.Cleared

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear data", e)
            _progress.value = SyncProgress.Idle
            SyncResult.Error("Failed to clear data: ${e.message}")
        }
    }

    /**
     * Phase 1: Download data.zip file.
     */
    private suspend fun downloadDataZip(appFilesDir: String): String? {
        return try {
            // Check for cached file first
            val localFile = fileDiscoveryService.getLocalDataFile(appFilesDir)
            if (localFile != null) {
                Logger.d(TAG, "Using cached data.zip file: ${localFile.sizeBytes} bytes")
                return localFile.path
            }

            // Download from GitHub releases
            val remoteFile = fileDiscoveryService.getRemoteDataFile()
                ?: return null

            val downloadResult = downloadService.downloadFile(remoteFile, appFilesDir) { progress ->
                // Progress forwarding handled internally by DownloadService
            }

            when (downloadResult) {
                is DownloadService.DownloadResult.Success -> downloadResult.localFilePath
                is DownloadService.DownloadResult.Error -> {
                    Logger.e(TAG, "Download failed: ${downloadResult.message}")
                    null
                }
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Download phase failed", e)
            null
        }
    }

    /**
     * Phase 2: Extract all files from data.zip.
     */
    private suspend fun extractDataZip(dataZipPath: String, appFilesDir: String): List<com.grateful.deadly.services.data.models.ExtractedFile>? {
        return try {
            val extractionDir = "$appFilesDir/extracted_data"

            val extractionResult = fileExtractionService.extractFiles(
                zipPath = dataZipPath,
                outputDir = extractionDir
            ) { progress ->
                when (progress) {
                    is ExtractionProgress.Progress -> {
                        // Forward extraction progress to sync progress
                        _progress.value = SyncProgress.Extracting
                    }
                    else -> { /* Handle other progress states if needed */ }
                }
            }

            when (extractionResult) {
                is ExtractionResult.Success -> extractionResult.extractedFiles
                is ExtractionResult.Error -> {
                    Logger.e(TAG, "Extraction failed: ${extractionResult.message}")
                    null
                }
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Extraction phase failed", e)
            null
        }
    }

    /**
     * Phase 3: Import show data from extracted files.
     */
    private suspend fun importShowData(extractedFiles: List<com.grateful.deadly.services.data.models.ExtractedFile>): Int? {
        return try {
            val importResult = dataImportService.importShowData(extractedFiles) { progress ->
                when (progress) {
                    is ImportProgress.Processing -> {
                        _progress.value = SyncProgress.Importing(progress.current, progress.total)
                    }
                    else -> { /* Handle other progress states if needed */ }
                }
            }

            when (importResult) {
                is ImportResult.Success -> importResult.importedCount
                is ImportResult.Error -> {
                    Logger.e(TAG, "Import failed: ${importResult.message}")
                    null
                }
            }

        } catch (e: Exception) {
            Logger.e(TAG, "Import phase failed", e)
            null
        }
    }

    /**
     * Phase 4: Cleanup extraction directory.
     */
    private suspend fun cleanupExtractionDirectory(extractionDir: String) {
        try {
            val success = fileExtractionService.cleanupExtraction(extractionDir)
            if (success) {
                Logger.d(TAG, "Extraction directory cleaned up")
            } else {
                Logger.w(TAG, "Failed to clean up extraction directory")
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Cleanup failed", e)
        }
    }

    /**
     * Delete cached data.zip file.
     */
    private suspend fun deleteCachedDataFile() {
        try {
            val appFilesDir = getAppFilesDir()
            val localFile = fileDiscoveryService.getLocalDataFile(appFilesDir)
            if (localFile != null) {
                downloadService.deleteLocalFile(localFile.path)
                Logger.d(TAG, "Cached data.zip file deleted")
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to delete cached data.zip file", e)
        }
    }
}

/**
 * Progress tracking for the complete synchronization workflow.
 */
sealed class SyncProgress {
    object Idle : SyncProgress()
    object Downloading : SyncProgress()
    object Extracting : SyncProgress()
    object Clearing : SyncProgress()
    data class Importing(val current: Int, val total: Int) : SyncProgress() {
        val percent: Float get() = if (total > 0) current.toFloat() / total else 0f
    }
}

/**
 * Result of the complete synchronization workflow.
 */
sealed class SyncResult {
    data class Success(val showCount: Int) : SyncResult()
    data class AlreadyExists(val showCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    object Cleared : SyncResult()
}