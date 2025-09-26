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
        val driver = try {
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = context,
                name = "deadly.db"
            )
        } catch (e: Exception) {
            // If database initialization fails (likely schema mismatch),
            // delete the database file and recreate it with the new schema
            val dbFile = context.getDatabasePath("deadly.db")
            if (dbFile.exists()) {
                dbFile.delete()
            }
            AndroidSqliteDriver(
                schema = Database.Schema,
                context = context,
                name = "deadly.db"
            )
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
}