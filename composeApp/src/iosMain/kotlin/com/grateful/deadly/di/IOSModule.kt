package com.grateful.deadly.di

import com.grateful.deadly.database.Database
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

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
            name = "deadly.db"
        )
        Database(driver)
    }
}