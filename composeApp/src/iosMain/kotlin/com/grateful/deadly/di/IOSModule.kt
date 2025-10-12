package com.grateful.deadly.di

import com.grateful.deadly.database.Database
import com.grateful.deadly.data.migration.DatabaseMigrationObserver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
// Phase 3: Platform Tools for iOS
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.MediaServiceHolder
import com.grateful.deadly.platform.AppVersionReader
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
        // SQLDelight automatically applies migrations from migrations/ directory
        // Migration 1.sqm: Adds recordingId to recent_shows (v1 -> v2)
        // Migration 2.sqm: Adds user_recording_preferences table (v2 -> v3)

        val logger = com.grateful.deadly.core.logging.Logger
        logger.i("IOSModule", "🔍 🗄️ Initializing database...")

        // Check if database file exists before trying to open it
        val supportDirectories = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        )
        val supportPath = supportDirectories.firstOrNull() as? String ?: ""
        val dbPath = "$supportPath/databases/deadly.db"
        val fileManager = NSFileManager.defaultManager
        val databaseExists = fileManager.fileExistsAtPath(dbPath)

        logger.i("IOSModule", "🔍 📂 Database path: $dbPath")
        logger.i("IOSModule", "🔍 📂 Database exists: $databaseExists")

        // Get current database version (only if database exists)
        val oldVersion = if (databaseExists) {
            try {
                // Open existing database WITHOUT schema to avoid creating it
                val testDriver = NativeSqliteDriver(
                    schema = Database.Schema,
                    name = "deadly.db"
                )
                val version = testDriver.executeQuery(
                    identifier = null,
                    sql = "PRAGMA user_version",
                    mapper = { cursor ->
                        app.cash.sqldelight.db.QueryResult.Value(
                            if (cursor.next().value) {
                                cursor.getLong(0) ?: 0L
                            } else {
                                0L
                            }
                        )
                    },
                    parameters = 0
                ).value
                testDriver.close()
                logger.i("IOSModule", "🔍 📊 Existing database version: $version")
                version
            } catch (e: Exception) {
                logger.e("IOSModule", "🔍 ⚠️ Error reading database version: ${e.message}")
                0L
            }
        } else {
            logger.i("IOSModule", "🔍 ✨ Fresh install - no database file exists")
            0L
        }

        val newVersion = Database.Schema.version
        logger.i("IOSModule", "🔍 📊 Target schema version: $newVersion")

        // Report migration start if versions differ (and oldVersion > 0 means database exists)
        if (oldVersion > 0 && oldVersion < newVersion) {
            logger.i("IOSModule", "🔍 🔄 Migration needed: v$oldVersion → v$newVersion")
            DatabaseMigrationObserver.onMigrationStart(oldVersion, newVersion)
        } else if (oldVersion == 0L) {
            logger.i("IOSModule", "🔍 ✨ Fresh install - will create schema v$newVersion")
        } else {
            logger.i("IOSModule", "🔍 ✅ Database already at current version v$oldVersion")
        }

        val driver = try {
            logger.i("IOSModule", "🔍 🔨 Creating NativeSqliteDriver (this may create database if it doesn't exist)...")
            val nativeDriver = NativeSqliteDriver(
                schema = Database.Schema,
                name = "deadly.db"
            )
            logger.i("IOSModule", "🔍 ✅ NativeSqliteDriver created")

            // Check if database was just created
            val dbExistsNow = fileManager.fileExistsAtPath(dbPath)
            if (!databaseExists && dbExistsNow) {
                logger.i("IOSModule", "🔍 🆕 Database file was created by NativeSqliteDriver")
            }

            // Only run migrations if database exists (oldVersion > 0)
            // Fresh installs (oldVersion = 0) get schema created automatically
            if (oldVersion > 0 && oldVersion < newVersion) {
                logger.i("IOSModule", "🔍 🔄 Running migration...")
                Database.Schema.migrate(nativeDriver, oldVersion, newVersion)
                DatabaseMigrationObserver.onMigrationSuccess(oldVersion, newVersion)
                logger.i("IOSModule", "🔍 ✅ Migration completed successfully")
            } else if (oldVersion == 0L) {
                logger.i("IOSModule", "🔍 ✅ Fresh database created at schema v$newVersion")
            }

            nativeDriver
        } catch (e: Exception) {
            logger.e("IOSModule", "🔍 ❌ Database initialization failed", e)
            // Report error if migration fails
            if (oldVersion > 0 && oldVersion < newVersion) {
                DatabaseMigrationObserver.onMigrationError(oldVersion, newVersion, e)
            }
            throw e
        }

        logger.i("IOSModule", "🔍 ✅ Database initialized successfully")
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

    single<AppVersionReader> {
        AppVersionReader()
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