package com.grateful.deadly.di

import com.grateful.deadly.database.Database
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
// Phase 3: Platform Tools for iOS
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.archive.platform.CacheManager
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager

/**
 * iOS-specific Koin DI module.
 */
val iosModule = module {
    single<Settings> {
        NSUserDefaultsSettings(delegate = NSUserDefaults.standardUserDefaults)
    }

    single<Database> {
        val driver = try {
            NativeSqliteDriver(
                schema = Database.Schema,
                name = "deadly.db"
            )
        } catch (e: Exception) {
            // If database initialization fails (likely schema mismatch),
            // delete the database file and recreate it with the new schema
            deleteIOSDatabaseFile()
            NativeSqliteDriver(
                schema = Database.Schema,
                name = "deadly.db"
            )
        }
        Database(driver)
    }

    single<() -> String> {
        return@single {
            val documentDirectories = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )
            documentDirectories.firstOrNull() as? String ?: ""
        }
    }

    // Phase 3: Platform Tools (iOS implementations)
    single<NetworkClient> {
        NetworkClient()
    }

    single<CacheManager> {
        CacheManager()
    }

    single<PlatformMediaPlayer> {
        PlatformMediaPlayer()
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