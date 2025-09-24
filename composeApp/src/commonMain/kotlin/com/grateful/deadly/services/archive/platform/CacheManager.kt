package com.grateful.deadly.services.archive.platform

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
 * Platform-specific cache manager for file I/O operations.
 *
 * This is a minimal platform tool in the Universal Service + Platform Tool pattern.
 * It handles ONLY generic file operations with no business logic or Archive.org knowledge.
 *
 * Universal services will handle:
 * - Cache key generation (recordingId)
 * - Domain model serialization/deserialization
 * - Cache-first logic and expiry strategies
 * - Cache invalidation policies
 *
 * Platform tools handle:
 * - Generic file read/write operations
 * - Platform-specific filesystem access
 * - Directory management
 * - File existence and timestamp checks
 */
expect class CacheManager {

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
    suspend fun get(key: String, type: CacheType): String?

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
    suspend fun put(key: String, type: CacheType, data: String)

    /**
     * Check if cached data is expired.
     *
     * Following V2's pattern with 1 week (168 hours) expiry.
     *
     * @param key Cache key (typically recordingId)
     * @param type Cache type (metadata, tracks, reviews)
     * @return true if expired or doesn't exist, false if still valid
     */
    suspend fun isExpired(key: String, type: CacheType): Boolean

    /**
     * Clear cache entries.
     *
     * @param key If provided, clear only this key's cache. If null, clear all.
     * @param type If provided, clear only this type. If null, clear all types for key.
     */
    suspend fun clear(key: String?, type: CacheType?)
}