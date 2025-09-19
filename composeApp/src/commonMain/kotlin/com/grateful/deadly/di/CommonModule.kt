package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.services.search.SearchServiceImpl
import com.grateful.deadly.data.search.SearchRepository
import com.grateful.deadly.data.search.SearchRepositoryImpl
import com.grateful.deadly.services.data.DataImportService
import com.grateful.deadly.core.design.theme.ThemeManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Common Koin DI module for shared dependencies.
 */
val commonModule = module {

    // Network
    single<HttpClient> {
        Logger.d("CommonModule", "Creating HttpClient instance")
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    // Data Layer
    single<SearchRepository> {
        Logger.d("CommonModule", "Creating SearchRepository instance")
        SearchRepositoryImpl(get())
    }

    single {
        Logger.d("CommonModule", "Creating DataImportService instance")
        DataImportService(
            database = get(),
            httpClient = get(),
            settings = get()
        )
    }

    // Services
    single<SearchService> {
        Logger.d("CommonModule", "Creating SearchService (using real implementation)")
        SearchServiceImpl(
            repository = get(),
            settings = get()
        )
    }

    // Theme
    single {
        Logger.d("CommonModule", "Creating ThemeManager instance")
        ThemeManager()
    }

    // ViewModels
    factory {
        Logger.d("CommonModule", "Creating SearchViewModel instance")
        SearchViewModel(get())
    }
}