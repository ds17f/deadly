package com.grateful.deadly.services.data.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bridge for platform-specific unzip operations using async callbacks.
 *
 * This allows Kotlin code to request unzip operations from the host app (iOS)
 * and await results via coroutines. The iOS app calls reportUnzipResult() to
 * complete the operation.
 */
object PlatformUnzipBridge {
    private val mutex = Mutex()
    private var pending: CompletableDeferred<Result<String>>? = null

    /**
     * Request an unzip operation from the host platform.
     * Suspends until the platform reports the result via reportUnzipResult().
     *
     * @param sourcePath Absolute path to ZIP file
     * @param destinationPath Absolute path to extraction directory
     * @param overwrite Whether to overwrite existing files
     * @return Result with extracted path or error
     */
    suspend fun requestUnzip(
        sourcePath: String,
        destinationPath: String,
        overwrite: Boolean
    ): Result<String> {
        return mutex.withLock {
            if (pending != null) {
                return Result.failure(IllegalStateException("Unzip already in progress"))
            }

            val deferred = CompletableDeferred<Result<String>>()
            pending = deferred

            // Ask host app (iOS) to perform the unzip
            AppPlatform.requestUnzipFromHost(sourcePath, destinationPath, overwrite)

            // Suspend until host calls reportUnzipResult(...)
            val result = deferred.await()
            pending = null
            result
        }
    }

    /**
     * Called by the host platform when unzip operation completes.
     * This resumes the suspended requestUnzip() call.
     *
     * @param path The extracted path on success
     * @param errorMsg Error message on failure
     */
    fun reportUnzipResult(path: String?, errorMsg: String?) {
        val result = if (path != null) {
            Result.success(path)
        } else {
            Result.failure(Exception(errorMsg ?: "Unknown unzip error"))
        }

        pending?.complete(result)
    }
}
