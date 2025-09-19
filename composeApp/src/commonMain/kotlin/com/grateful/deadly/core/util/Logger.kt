package com.grateful.deadly.core.util

/**
 * Simple cross-platform logger for KMM.
 * Follows our existing logging patterns.
 */
object Logger {

    fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    fun i(tag: String, message: String) {
        println("I/$tag: $message")
    }

    fun w(tag: String, message: String) {
        println("W/$tag: $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        println("E/$tag: $message")
        throwable?.let {
            println("E/$tag: ${it.stackTraceToString()}")
        }
    }
}