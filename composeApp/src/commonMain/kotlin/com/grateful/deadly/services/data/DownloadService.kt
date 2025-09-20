package com.grateful.deadly.services.data

import com.grateful.deadly.core.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Download service with streaming downloads and progress tracking.
 * Downloads data.zip files with local caching.
 */
class DownloadService(
    private val httpClient: HttpClient,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    companion object {
        private const val TAG = "DownloadService"
        private const val BUFFER_SIZE = 8192
    }

    data class DownloadProgress(
        val fileName: String = "",
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val isCompleted: Boolean = false,
        val error: String? = null
    ) {
        val progressPercent: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }

    sealed class DownloadResult {
        data class Success(val localFilePath: String) : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    /**
     * Download a remote file to the local files directory with progress tracking
     */
    suspend fun downloadFile(
        remoteFile: FileDiscoveryService.RemoteFile,
        appFilesDir: String,
        progressCallback: ((DownloadProgress) -> Unit)? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Starting download: ${remoteFile.name} from ${remoteFile.downloadUrl}")

            val destinationPath = (appFilesDir.toPath() / remoteFile.name)

            // Create files directory if it doesn't exist
            val parentDir = destinationPath.parent
            if (parentDir != null && !fileSystem.exists(parentDir)) {
                fileSystem.createDirectories(parentDir)
            }

            // Delete existing file if it exists
            if (fileSystem.exists(destinationPath)) {
                Logger.d(TAG, "Deleting existing file: ${remoteFile.name}")
                fileSystem.delete(destinationPath)
            }

            // Start download
            val response: HttpResponse = httpClient.get(remoteFile.downloadUrl)
            val contentLength = response.contentLength() ?: remoteFile.sizeBytes

            Logger.d(TAG, "Download response: ${response.status}, content length: $contentLength bytes")

            // Create file and download with progress tracking
            fileSystem.write(destinationPath, mustCreate = true) {
                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = ByteArray(BUFFER_SIZE)
                var downloadedBytes = 0L

                // Initial progress
                progressCallback?.invoke(
                    DownloadProgress(
                        fileName = remoteFile.name,
                        downloadedBytes = 0L,
                        totalBytes = contentLength,
                        isCompleted = false
                    )
                )

                // Download loop with progress updates
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) break

                    // Write chunk to file
                    write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    // Update progress
                    progressCallback?.invoke(
                        DownloadProgress(
                            fileName = remoteFile.name,
                            downloadedBytes = downloadedBytes,
                            totalBytes = contentLength,
                            isCompleted = false
                        )
                    )
                }

                // Final progress
                progressCallback?.invoke(
                    DownloadProgress(
                        fileName = remoteFile.name,
                        downloadedBytes = downloadedBytes,
                        totalBytes = contentLength,
                        isCompleted = true
                    )
                )

                Logger.d(TAG, "Download completed: ${remoteFile.name} ($downloadedBytes bytes)")
            }

            DownloadResult.Success(destinationPath.toString())

        } catch (e: Exception) {
            Logger.e(TAG, "Download failed for ${remoteFile.name}", e)

            progressCallback?.invoke(
                DownloadProgress(
                    fileName = remoteFile.name,
                    error = e.message ?: "Download failed"
                )
            )

            DownloadResult.Error("Download failed: ${e.message}")
        }
    }

    /**
     * Delete a local file (for cache management)
     */
    suspend fun deleteLocalFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = filePath.toPath()
            if (fileSystem.exists(path)) {
                fileSystem.delete(path)
                Logger.d(TAG, "Deleted local file: $filePath")
                true
            } else {
                Logger.d(TAG, "File does not exist: $filePath")
                true // Consider non-existent file as success
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete file: $filePath", e)
            false
        }
    }

    /**
     * Get file size of a local file
     */
    suspend fun getLocalFileSize(filePath: String): Long = withContext(Dispatchers.IO) {
        try {
            val path = filePath.toPath()
            if (fileSystem.exists(path)) {
                fileSystem.metadata(path).size ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get file size: $filePath", e)
            0L
        }
    }
}