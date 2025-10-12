package com.grateful.deadly.di

import android.content.Context
import com.grateful.deadly.database.Database
import com.grateful.deadly.data.migration.DatabaseMigrationObserver
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
// Phase 3: Platform Tools for Android
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.platform.AppVersionReader
import org.koin.dsl.module

/**
 * Android-specific Koin DI module.
 */
val androidModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            delegate = get<Context>().getSharedPreferences("deadly_settings", Context.MODE_PRIVATE)
        )
    }

    single<Database> {
        val context: Context = get()
        // SQLDelight automatically applies migrations from migrations/ directory
        // Migration 1.sqm: Adds recordingId to recent_shows (v1 -> v2)
        // Migration 2.sqm: Adds user_recording_preferences table (v2 -> v3)

        // Get current database version before migration
        val oldVersion = try {
            val testDriver = AndroidSqliteDriver(
                schema = Database.Schema,
                context = context,
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
            version
        } catch (e: Exception) {
            0L // Fresh install - database doesn't exist
        }

        val newVersion = Database.Schema.version

        // Report migration start if versions differ (and oldVersion > 0 means database exists)
        if (oldVersion > 0 && oldVersion < newVersion) {
            DatabaseMigrationObserver.onMigrationStart(oldVersion, newVersion)
        }

        val driver = try {
            val androidDriver = AndroidSqliteDriver(
                schema = Database.Schema,
                context = context,
                name = "deadly.db"
            )

            // Only run migrations if database exists (oldVersion > 0)
            // Fresh installs (oldVersion = 0) get schema created automatically
            if (oldVersion > 0 && oldVersion < newVersion) {
                Database.Schema.migrate(androidDriver, oldVersion, newVersion)
                DatabaseMigrationObserver.onMigrationSuccess(oldVersion, newVersion)
            }

            androidDriver
        } catch (e: Exception) {
            // Report error if migration fails
            if (oldVersion > 0 && oldVersion < newVersion) {
                DatabaseMigrationObserver.onMigrationError(oldVersion, newVersion, e)
            }
            throw e
        }

        Database(driver)
    }

    single<() -> String> {
        val context: Context = get()
        return@single { context.cacheDir.absolutePath }  // V2 pattern: use cache directory
    }

    // Phase 3: Platform Tools (Android implementations)
    single<NetworkClient> {
        NetworkClient()
    }


    single<PlatformMediaPlayer> {
        PlatformMediaPlayer(get<Context>())
    }

    single<AppVersionReader> {
        AppVersionReader(get<Context>())
    }
}