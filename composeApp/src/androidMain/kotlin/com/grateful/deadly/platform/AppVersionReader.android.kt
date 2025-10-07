package com.grateful.deadly.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Android implementation of AppVersionReader.
 * Reads version information from PackageManager.
 */
actual class AppVersionReader(private val context: Context) {
    actual fun getVersionName(): String {
        return try {
            val packageInfo = getPackageInfo()
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    actual fun getVersionCode(): String {
        return try {
            val packageInfo = getPackageInfo()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            "0"
        }
    }

    private fun getPackageInfo(): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}
