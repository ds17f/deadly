package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.services.search.SearchServiceStub
import org.koin.dsl.module

/**
 * Common Koin DI module for shared dependencies.
 */
val commonModule = module {
    
    // Services
    // TODO: Replace SearchServiceStub with SearchServiceImpl when real implementation is ready
    single<SearchService> { 
        Logger.d("CommonModule", "Creating SearchService (using stub implementation)")
        SearchServiceStub(get())
    }
    
    // ViewModels
    factory { 
        Logger.d("CommonModule", "Creating SearchViewModel instance")
        SearchViewModel(get()) 
    }
}