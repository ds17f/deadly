package com.grateful.deadly.services.archive.platform

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * iOS implementation of NetworkClient using Ktor with Darwin engine.
 *
 * Uses platform-optimized Ktor Darwin engine for maximum iOS performance.
 * Handles generic HTTP operations with no Archive.org business logic.
 */
actual class NetworkClient {

    private val httpClient = HttpClient(Darwin) {
        engine {
            configureRequest {
                setTimeoutInterval(60.0)
            }
        }
    }

    /**
     * iOS HTTP implementation using Ktor Darwin engine.
     * Converts platform-specific exceptions to generic Result type.
     */
    actual suspend fun getJson(url: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = httpClient.get(url) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.UserAgent, "Deadly-iOS/1.0")
                }
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}