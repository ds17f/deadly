package com.grateful.deadly.services.archive.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of CacheManager using Android filesystem.
 *
 * Uses platform-optimized Android cache directory for automatic cleanup.
 * Handles generic file operations with V2's {recordingId}.{type}.json pattern.
 */
actual class CacheManager(
    private val context: Context
) {

    companion object {
        private const val CACHE_DIR_NAME = "archive"
        private const val EXPIRY_HOURS = 168L // 1 week like V2
    }

    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    /**
     * Android file read implementation.
     * Uses Android cache directory with automatic cleanup support.
     */
    actual suspend fun get(key: String, type: CacheType): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "${key}.${type.name.lowercase()}.json")

            if (!file.exists()) {
                return@withContext null
            }

            // Check expiry before reading
            if (isExpired(key, type)) {
                file.delete() // Clean up expired file
                return@withContext null
            }

            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Android file write implementation.
     * Creates cache directory if needed and writes to filesystem.
     */
    actual suspend fun put(key: String, type: CacheType, data: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "${key}.${type.name.lowercase()}.json")
            file.writeText(data)
        } catch (e: Exception) {
            // Silently fail - cache is not critical
        }
    }

    /**
     * Android expiry check using file modification time.
     * Follows V2's 1 week expiry pattern.
     */
    actual suspend fun isExpired(key: String, type: CacheType): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "${key}.${type.name.lowercase()}.json")

            if (!file.exists()) {
                return@withContext true
            }

            val ageHours = (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60)
            ageHours > EXPIRY_HOURS
        } catch (e: Exception) {
            true // Assume expired if we can't check
        }
    }

    /**
     * Android cache clearing implementation.
     * Handles selective clearing by key and/or type.
     */
    actual suspend fun clear(key: String?, type: CacheType?): Unit = withContext(Dispatchers.IO) {
        try {
            if (key != null && type != null) {
                // Clear specific key-type combination
                val file = File(cacheDir, "${key}.${type.name.lowercase()}.json")
                file.delete()
            } else if (key != null) {
                // Clear all types for specific key
                val pattern = "${key}."
                cacheDir.listFiles { file ->
                    file.name.startsWith(pattern) && file.name.endsWith(".json")
                }?.forEach { it.delete() }
            } else {
                // Clear entire cache directory
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Silently fail - cache clearing is not critical
        }
    }
}