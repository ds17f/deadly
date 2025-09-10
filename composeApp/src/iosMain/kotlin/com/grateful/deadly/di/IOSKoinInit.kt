package com.grateful.deadly.di

/**
 * Initialize Koin for iOS
 * This should be called from iOS platform code (AppDelegate or similar)
 * 
 * Uses shared KoinInitializer for consistent startup logging across platforms
 */
fun initKoin() {
    KoinInitializer.initializeKoin(iosModule)
}