package com.grateful.deadly.core.logging

/**
 * Simple cross-platform logging interface for KMM
 * 
 * Usage:
 *   Logger.d("MyTag", "Debug message")
 *   Logger.i("MyTag", "Info message") 
 *   Logger.w("MyTag", "Warning message")
 *   Logger.e("MyTag", "Error message", throwable)
 */
object Logger {
    
    fun d(tag: String, message: String) {
        logDebug(tag, message)
    }
    
    fun i(tag: String, message: String) {
        logInfo(tag, message)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logWarn(tag, message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logError(tag, message, throwable)
    }
}

/**
 * Platform-specific logging implementations
 */
internal expect fun logDebug(tag: String, message: String)
internal expect fun logInfo(tag: String, message: String)
internal expect fun logWarn(tag: String, message: String, throwable: Throwable?)
internal expect fun logError(tag: String, message: String, throwable: Throwable?)