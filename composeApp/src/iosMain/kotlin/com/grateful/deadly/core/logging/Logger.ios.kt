package com.grateful.deadly.core.logging

/**
 * iOS implementation using println for console output
 */
internal actual fun logDebug(tag: String, message: String) {
    println("D/$tag: $message")
}

internal actual fun logInfo(tag: String, message: String) {
    println("I/$tag: $message")
}

internal actual fun logWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        println("W/$tag: $message")
        println("W/$tag: ${throwable.stackTraceToString()}")
    } else {
        println("W/$tag: $message")
    }
}

internal actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        println("E/$tag: $message")
        println("E/$tag: ${throwable.stackTraceToString()}")
    } else {
        println("E/$tag: $message")
    }
}