package com.grateful.deadly.di

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
        SearchServiceStub(get())
    }
    
    // ViewModels
    factory { SearchViewModel(get()) }
}