package com.grateful.deadly

import android.app.Application
import com.grateful.deadly.di.commonModule
import com.grateful.deadly.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DeadlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@DeadlyApplication)
            modules(commonModule, androidModule)
        }
    }
}