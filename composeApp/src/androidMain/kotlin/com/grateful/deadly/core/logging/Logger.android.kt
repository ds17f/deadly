package com.grateful.deadly.core.logging

import android.util.Log

/**
 * Android implementation using android.util.Log
 */
internal actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}

internal actual fun logInfo(tag: String, message: String) {
    Log.i(tag, message)
}

internal actual fun logWarn(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.w(tag, message, throwable)
    } else {
        Log.w(tag, message)
    }
}

internal actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.e(tag, message, throwable)
    } else {
        Log.e(tag, message)
    }
}