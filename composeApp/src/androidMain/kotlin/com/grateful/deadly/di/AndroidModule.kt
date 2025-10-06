package com.grateful.deadly.di

import android.content.Context
import com.grateful.deadly.database.Database
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
// Phase 3: Platform Tools for Android
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
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
        val driver = AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "deadly.db",
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onMigrate(
                    driver: app.cash.sqldelight.db.SqlDriver,
                    oldVersion: Long,
                    newVersion: Long
                ) {
                    // SQLDelight will automatically apply migrations from migrations/ directory
                    // Migration 1.sqm: Adds recordingId to recent_shows (v1 -> v2)
                }
            }
        )
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
}