package com.grateful.deadly.services.archive.platform

/**
 * Platform-specific network client for HTTP operations.
 *
 * This is a minimal platform tool in the Universal Service + Platform Tool pattern.
 * It handles ONLY generic HTTP operations with no business logic or Archive.org knowledge.
 *
 * Universal services will handle:
 * - Archive.org URL building
 * - Domain model conversion
 * - Cache-first logic
 * - Error handling strategies
 *
 * Platform tools handle:
 * - Generic HTTP requests
 * - Platform-optimized networking (OkHttp vs Ktor)
 * - Platform-specific error types -> generic Result conversion
 */
expect class NetworkClient {

    /**
     * Generic JSON HTTP GET request.
     *
     * This method has NO knowledge of Archive.org APIs, endpoints, or data formats.
     * It simply fetches JSON from any URL and returns it as a string.
     *
     * @param url Complete URL to fetch from
     * @return Result with JSON string or error
     */
    suspend fun getJson(url: String): Result<String>
}