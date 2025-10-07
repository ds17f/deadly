package com.grateful.deadly.services.app

import com.grateful.deadly.platform.AppVersionReader

/**
 * Universal service for app version information.
 *
 * Following the Universal Service + Platform Tool pattern:
 * - Universal logic: version formatting, display strings
 * - Platform-specific: reading from package/bundle (via AppVersionReader)
 */
class AppVersionService(
    private val versionReader: AppVersionReader
) {
    /**
     * Get the formatted version string for display (e.g., "Version 1.0.0 (1)")
     */
    fun getVersionString(): String {
        val versionName = versionReader.getVersionName()
        val versionCode = versionReader.getVersionCode()
        return "Version $versionName ($versionCode)"
    }

    /**
     * Get the short version string (e.g., "1.0.0")
     */
    fun getVersionName(): String {
        return versionReader.getVersionName()
    }

    /**
     * Get the build number (e.g., "1")
     */
    fun getBuildNumber(): String {
        return versionReader.getVersionCode()
    }

    /**
     * Get the full version info for debugging
     */
    fun getVersionInfo(): VersionInfo {
        return VersionInfo(
            versionName = versionReader.getVersionName(),
            versionCode = versionReader.getVersionCode(),
            displayString = getVersionString()
        )
    }
}

/**
 * Data class containing all version information
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: String,
    val displayString: String
)
