package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Shared Koin DI initialization for all platforms
 * 
 * This centralizes startup logging and DI setup to ensure consistency
 * between Android and iOS platform initialization.
 */
object KoinInitializer {
    
    private const val TAG = "KoinInitializer"
    
    /**
     * Initialize Koin DI with platform-specific module and configuration
     * 
     * @param platformModule The platform-specific DI module (androidModule or iosModule)
     * @param platformConfig Optional platform-specific Koin configuration
     */
    fun initializeKoin(
        platformModule: Module, 
        platformConfig: (org.koin.core.KoinApplication.() -> Unit)? = null
    ) {
        Logger.i(TAG, "🚀 Deadly app starting up")
        Logger.d(TAG, "Initializing Koin DI container")
        
        startKoin {
            modules(commonModule, platformModule)
            platformConfig?.invoke(this)
        }
        
        Logger.i(TAG, "✅ Deadly app startup complete")
    }
}