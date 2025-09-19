package com.grateful.deadly.di

import android.content.Context
import com.grateful.deadly.database.Database
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
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
        val driver = AndroidSqliteDriver(
            schema = Database.Schema,
            context = get(),
            name = "deadly.db"
        )
        Database(driver)
    }
}