package com.grateful.deadly.services.archive.platform

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock

/**
 * Cache types for domain model storage.
 *
 * Following V2's {recordingId}.{type}.json filename pattern.
 * Each type represents a different domain model cached from Archive.org.
 */
enum class CacheType {
    METADATA,  // Recording metadata (show info, ratings, etc.)
    TRACKS,    // Track list with format information
    REVIEWS    // User reviews and ratings
}

/**
 * Cross-platform cache manager using Okio FileSystem.
 *
 * This is a Universal Service that uses Okio FileSystem (platform tool) for file operations.
 * Follows the Universal Service + Platform Tool pattern properly by delegating to Okio.
 *
 * Universal services handle:
 * - Cache key generation (recordingId)
 * - Domain model serialization/deserialization
 * - Cache-first logic and expiry strategies
 * - Cache invalidation policies
 *
 * Platform tool (Okio FileSystem) handles:
 * - Cross-platform file read/write operations
 * - Directory management and path handling
 * - File existence and metadata checks
 */
class CacheManager(
    private val fileSystem: okio.FileSystem,
    private val getAppFilesDir: () -> String
) {

    companion object {
        private const val CACHE_DIR_NAME = "archive"
        private const val EXPIRY_HOURS = 168L // 1 week like V2
    }

    private fun getCacheDir(): okio.Path {
        val appFilesDir = getAppFilesDir()
        return (appFilesDir.toPath() / CACHE_DIR_NAME)
    }

    private fun getCacheFilePath(key: String, type: CacheType): okio.Path {
        return getCacheDir() / "${key}.${type.name.lowercase()}.json"
    }

    /**
     * Read cached data for a key and type.
     *
     * Uses V2's filename pattern: {key}.{type}.json
     * Returns null if file doesn't exist or is expired.
     *
     * @param key Cache key (typically recordingId)
     * @param type Cache type (metadata, tracks, reviews)
     * @return Cached JSON string or null if not found/expired
     */
    suspend fun get(key: String, type: CacheType): String? = withContext(Dispatchers.Default) {
        try {
            val filePath = getCacheFilePath(key, type)

            if (!fileSystem.exists(filePath)) {
                return@withContext null
            }

            // Check expiry before reading
            if (isExpired(key, type)) {
                fileSystem.delete(filePath) // Clean up expired file
                return@withContext null
            }

            fileSystem.read(filePath) {
                readUtf8()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write data to cache for a key and type.
     *
     * Uses V2's filename pattern: {key}.{type}.json
     * Creates cache directory if it doesn't exist.
     *
     * @param key Cache key (typically recordingId)
     * @param type Cache type (metadata, tracks, reviews)
     * @param data JSON string to cache
     */
    suspend fun put(key: String, type: CacheType, data: String): Unit = withContext(Dispatchers.Default) {
        try {
            val cacheDir = getCacheDir()
            val filePath = getCacheFilePath(key, type)

            // Create cache directory if needed
            if (!fileSystem.exists(cacheDir)) {
                fileSystem.createDirectories(cacheDir)
            }

            // Write data to file
            fileSystem.write(filePath) {
                writeUtf8(data)
            }
        } catch (e: Exception) {
            // Silently fail - cache is not critical
        }
    }

    /**
     * Check if cached data is expired.
     *
     * Following V2's pattern with 1 week (168 hours) expiry.
     *
     * @param key Cache key (typically recordingId)
     * @param type Cache type (metadata, tracks, reviews)
     * @return true if expired or doesn't exist, false if still valid
     */
    suspend fun isExpired(key: String, type: CacheType): Boolean = withContext(Dispatchers.Default) {
        try {
            val filePath = getCacheFilePath(key, type)

            if (!fileSystem.exists(filePath)) {
                return@withContext true
            }

            val metadata = fileSystem.metadata(filePath)
            val lastModified = metadata.lastModifiedAtMillis ?: return@withContext true
            val currentTimeMillis = Clock.System.now().toEpochMilliseconds()
            val ageHours = (currentTimeMillis - lastModified) / (1000 * 60 * 60)

            ageHours > EXPIRY_HOURS
        } catch (e: Exception) {
            true // Assume expired if we can't check
        }
    }

    /**
     * Clear cache entries.
     *
     * @param key If provided, clear only this key's cache. If null, clear all.
     * @param type If provided, clear only this type. If null, clear all types for key.
     */
    suspend fun clear(key: String?, type: CacheType?): Unit = withContext(Dispatchers.Default) {
        try {
            val cacheDir = getCacheDir()

            if (!fileSystem.exists(cacheDir)) {
                return@withContext // Nothing to clear
            }

            if (key != null && type != null) {
                // Clear specific key-type combination
                val filePath = getCacheFilePath(key, type)
                if (fileSystem.exists(filePath)) {
                    fileSystem.delete(filePath)
                }
            } else if (key != null) {
                // Clear all types for specific key
                val pattern = "${key}."
                fileSystem.list(cacheDir).forEach { path ->
                    if (path.name.startsWith(pattern) && path.name.endsWith(".json")) {
                        fileSystem.delete(path)
                    }
                }
            } else {
                // Clear entire cache directory
                fileSystem.list(cacheDir).forEach { path ->
                    fileSystem.delete(path)
                }
            }
        } catch (e: Exception) {
            // Silently fail - cache clearing is not critical
        }
    }
}