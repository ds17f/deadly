package com.grateful.deadly.platform

import platform.Foundation.NSBundle

/**
 * iOS implementation of AppVersionReader.
 * Reads version information from NSBundle.mainBundle.infoDictionary.
 */
actual class AppVersionReader {
    actual fun getVersionName(): String {
        val bundle = NSBundle.mainBundle
        val infoDictionary = bundle.infoDictionary ?: return "Unknown"
        return infoDictionary["CFBundleShortVersionString"] as? String ?: "Unknown"
    }

    actual fun getVersionCode(): String {
        val bundle = NSBundle.mainBundle
        val infoDictionary = bundle.infoDictionary ?: return "0"
        return infoDictionary["CFBundleVersion"] as? String ?: "0"
    }
}
