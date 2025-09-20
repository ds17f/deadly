package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.services.search.SearchServiceImpl
import com.grateful.deadly.data.search.SearchRepository
import com.grateful.deadly.data.search.SearchRepositoryImpl
// New architecture imports
import com.grateful.deadly.services.data.DataSyncOrchestrator
import com.grateful.deadly.services.data.FileExtractionService
import com.grateful.deadly.services.data.DataImportService as NewDataImportService
import com.grateful.deadly.services.data.DownloadService
import com.grateful.deadly.services.data.FileDiscoveryService
import com.grateful.deadly.services.data.platform.ZipExtractor
import com.grateful.deadly.services.data.platform.ShowRepository

// Old services (will be removed after migration)
import com.grateful.deadly.services.data.ZipExtractionService
import com.grateful.deadly.core.design.theme.ThemeManager
import okio.FileSystem
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

    // File System
    single<FileSystem> {
        Logger.d("CommonModule", "Creating FileSystem instance")
        FileSystem.SYSTEM
    }

    // New Architecture - Platform Tools
    single<ZipExtractor> {
        Logger.d("CommonModule", "Creating ZipExtractor platform tool")
        ZipExtractor()
    }

    single<ShowRepository> {
        Logger.d("CommonModule", "Creating ShowRepository platform tool")
        ShowRepository(get()) // Gets Database
    }

    // New Architecture - Universal Services
    single<FileExtractionService> {
        Logger.d("CommonModule", "Creating FileExtractionService")
        FileExtractionService(
            zipExtractor = get(),
            fileSystem = get()
        )
    }

    single<NewDataImportService> {
        Logger.d("CommonModule", "Creating DataImportService (new architecture)")
        NewDataImportService(
            showRepository = get(),
            fileSystem = get()
        )
    }

    single<DownloadService> {
        Logger.d("CommonModule", "Creating DownloadService")
        DownloadService(
            httpClient = get(),
            fileSystem = get()
        )
    }

    single<FileDiscoveryService> {
        Logger.d("CommonModule", "Creating FileDiscoveryService")
        FileDiscoveryService(
            httpClient = get(),
            fileSystem = get()
        )
    }

    // New Architecture - Orchestrator
    single<DataSyncOrchestrator> {
        Logger.d("CommonModule", "Creating DataSyncOrchestrator")
        DataSyncOrchestrator(
            downloadService = get(),
            fileDiscoveryService = get(),
            fileExtractionService = get(),
            dataImportService = get(),
            getAppFilesDir = get() // Platform-specific files directory provider
        )
    }

    // Data Layer
    single<SearchRepository> {
        Logger.d("CommonModule", "Creating SearchRepository instance")
        SearchRepositoryImpl(get())
    }

    // OLD SERVICES - Keep for backward compatibility during migration
    single<ZipExtractionService> {
        Logger.d("CommonModule", "Creating OLD ZipExtractionService (will be removed)")
        ZipExtractionService(get(), com.grateful.deadly.services.data.ZipExtractor(get()))
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