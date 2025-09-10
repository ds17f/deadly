package com.grateful.deadly

import android.app.Application
import com.grateful.deadly.di.KoinInitializer
import com.grateful.deadly.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class DeadlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Use shared DI initialization with Android-specific configuration
        KoinInitializer.initializeKoin(androidModule) { 
            androidLogger(Level.INFO)
            androidContext(this@DeadlyApplication)
        }
    }
}