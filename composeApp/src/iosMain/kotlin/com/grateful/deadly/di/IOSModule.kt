package com.grateful.deadly.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific Koin DI module.
 */
val iosModule = module {
    single<Settings> {
        NSUserDefaultsSettings(delegate = NSUserDefaults.standardUserDefaults)
    }
}