package com.grateful.deadly

import android.app.Application
import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.di.commonModule
import com.grateful.deadly.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DeadlyApplication : Application() {
    companion object {
        private const val TAG = "DeadlyApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Logger.i(TAG, "🚀 Deadly Android app starting up")
        Logger.d(TAG, "Initializing Koin DI container")
        
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@DeadlyApplication)
            modules(commonModule, androidModule)
        }
        
        Logger.i(TAG, "✅ Deadly Android app startup complete")
    }
}