package com.grateful.deadly.di

import com.grateful.deadly.di.commonModule
import com.grateful.deadly.di.iosModule
import org.koin.core.context.startKoin

/**
 * Initialize Koin for iOS
 * This should be called from iOS platform code (AppDelegate or similar)
 */
fun initKoin() {
    startKoin {
        modules(commonModule, iosModule)
    }
}