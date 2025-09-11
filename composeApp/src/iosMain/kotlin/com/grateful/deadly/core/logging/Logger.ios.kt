package com.grateful.deadly.core.logging

import platform.Foundation.NSLog

/**
 * iOS implementation using NSLog for system console output
 * Logs are visible via Console.app and xcrun simctl spawn booted log stream
 */
internal actual fun logDebug(tag: String, message: String) {
    NSLog("DEBUG: $tag: $message")
}

internal actual fun logInfo(tag: String, message: String) {
    NSLog("INFO: $tag: $message")
}

internal actual fun logWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("WARN: $tag: $message")
        NSLog("WARN: $tag: ${throwable.stackTraceToString()}")
    } else {
        NSLog("WARN: $tag: $message")
    }
}

internal actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("ERROR: $tag: $message")
        NSLog("ERROR: $tag: ${throwable.stackTraceToString()}")
    } else {
        NSLog("ERROR: $tag: $message")
    }
}