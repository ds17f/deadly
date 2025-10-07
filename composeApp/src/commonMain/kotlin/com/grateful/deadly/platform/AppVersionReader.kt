package com.grateful.deadly.platform

/**
 * Platform-specific tool for reading app version information.
 *
 * Following the Universal Service + Platform Tool pattern:
 * - Android implementation reads from BuildConfig/PackageManager
 * - iOS implementation reads from Bundle.main.infoDictionary
 */
expect class AppVersionReader {
    /**
     * Get the app version name (e.g., "1.0.0")
     */
    fun getVersionName(): String

    /**
     * Get the app version code/build number (e.g., "1")
     */
    fun getVersionCode(): String
}
