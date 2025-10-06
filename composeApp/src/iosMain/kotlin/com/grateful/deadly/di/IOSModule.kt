package com.grateful.deadly.di

import com.grateful.deadly.database.Database
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
// Phase 3: Platform Tools for iOS
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.MediaServiceHolder
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager

/**
 * iOS-specific Koin DI module.
 */
val iosModule = module {
    single<Settings> {
        NSUserDefaultsSettings(delegate = NSUserDefaults.standardUserDefaults)
    }

    single<Database> {
        val driver = NativeSqliteDriver(
            schema = Database.Schema,
            name = "deadly.db",
            onConfiguration = { config ->
                config.copy(extendedConfig = config.extendedConfig.copy(busyTimeout = 5000))
            },
            onMigrate = { driver, oldVersion, newVersion ->
                // SQLDelight will automatically apply migrations from migrations/ directory
                // Migration 1.sqm: Adds recordingId to recent_shows (v1 -> v2)
            }
        )
        Database(driver)
    }

    single<() -> String> {
        return@single {
            // V2 pattern: use NSCachesDirectory for cache files
            val cacheDirectories = NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true
            )
            cacheDirectories.firstOrNull() as? String ?: ""
        }
    }

    // Phase 3: Platform Tools (iOS implementations)
    single<NetworkClient> {
        NetworkClient()
    }


    single<PlatformMediaPlayer> {
        PlatformMediaPlayer()
    }

    // Register MediaService in holder for Swift lifecycle access
    single {
        val mediaService = com.grateful.deadly.services.media.MediaService(get())
        MediaServiceHolder.setMediaService(mediaService)
        mediaService
    }
}

/**
 * Helper function to delete the iOS database file when schema changes occur.
 */
@OptIn(ExperimentalForeignApi::class)
private fun deleteIOSDatabaseFile() {
    try {
        // Get Application Support directory where NativeSqliteDriver actually creates the database
        val supportDirectories = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        )
        val supportPath = supportDirectories.firstOrNull() as? String ?: return
        val dbDir = "$supportPath/databases"
        val dbPath = "$dbDir/deadly.db"

        val fileManager = NSFileManager.defaultManager

        // Delete main database file
        if (fileManager.fileExistsAtPath(dbPath)) {
            fileManager.removeItemAtPath(dbPath, null)
        }

        // Delete SQLite WAL (Write-Ahead Logging) files that persist schema
        val shmPath = "$dbDir/deadly.db-shm"
        if (fileManager.fileExistsAtPath(shmPath)) {
            fileManager.removeItemAtPath(shmPath, null)
        }

        val walPath = "$dbDir/deadly.db-wal"
        if (fileManager.fileExistsAtPath(walPath)) {
            fileManager.removeItemAtPath(walPath, null)
        }
    } catch (e: Exception) {
        // Ignore deletion errors - database will be recreated anyway
    }
}