package com.grateful.deadly.services.archive.platform

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock

/**
 * iOS implementation of CacheManager (Phase 2 stub).
 *
 * This is a Phase 2 stub implementation for testing platform tool compilation.
 * Will be replaced with NSFileManager implementation in Phase 6.
 *
 * Handles generic file operations with V2's {recordingId}.{type}.json pattern.
 */
actual class CacheManager {

    companion object {
        private const val EXPIRY_SECONDS = 168L * 60L * 60L // 1 week like V2
    }

    private val memoryCache = mutableMapOf<String, Pair<String, Long>>() // data, timestamp

    /**
     * Stub implementation for Phase 2.
     * Uses in-memory cache for testing. Will be replaced with NSFileManager in Phase 6.
     */
    actual suspend fun get(key: String, type: CacheType): String? = withContext(Dispatchers.Default) {
        try {
            val cacheKey = "${key}.${type.name.lowercase()}.json"
            val cached = memoryCache[cacheKey]

            if (cached != null) {
                val (data, timestamp) = cached
                val currentTimeSeconds = Clock.System.now().epochSeconds
                val ageSeconds = currentTimeSeconds - timestamp

                if (ageSeconds <= EXPIRY_SECONDS) {
                    data
                } else {
                    memoryCache.remove(cacheKey)
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stub implementation for Phase 2.
     * Uses in-memory storage. Will be replaced with NSFileManager in Phase 6.
     */
    actual suspend fun put(key: String, type: CacheType, data: String): Unit = withContext(Dispatchers.Default) {
        try {
            val cacheKey = "${key}.${type.name.lowercase()}.json"
            val timestamp = Clock.System.now().epochSeconds
            memoryCache[cacheKey] = Pair(data, timestamp)
        } catch (e: Exception) {
            // Silently fail - cache is not critical
        }
    }

    /**
     * Stub implementation for Phase 2.
     * Checks in-memory cache expiry. Will be replaced with file timestamp check in Phase 6.
     */
    actual suspend fun isExpired(key: String, type: CacheType): Boolean = withContext(Dispatchers.Default) {
        try {
            val cacheKey = "${key}.${type.name.lowercase()}.json"
            val cached = memoryCache[cacheKey]

            if (cached != null) {
                val (_, timestamp) = cached
                val currentTimeSeconds = Clock.System.now().epochSeconds
                val ageSeconds = currentTimeSeconds - timestamp
                ageSeconds > EXPIRY_SECONDS
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Stub implementation for Phase 2.
     * Clears in-memory cache. Will be replaced with file deletion in Phase 6.
     */
    actual suspend fun clear(key: String?, type: CacheType?): Unit = withContext(Dispatchers.Default) {
        try {
            if (key != null && type != null) {
                // Clear specific key-type combination
                val cacheKey = "${key}.${type.name.lowercase()}.json"
                memoryCache.remove(cacheKey)
            } else if (key != null) {
                // Clear all types for specific key
                val pattern = "${key}."
                val keysToRemove = memoryCache.keys.filter { it.startsWith(pattern) }
                keysToRemove.forEach { memoryCache.remove(it) }
            } else {
                // Clear entire cache
                memoryCache.clear()
            }
        } catch (e: Exception) {
            // Silently fail - cache clearing is not critical
        }
    }
}