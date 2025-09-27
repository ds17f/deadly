package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.feature.player.PlayerViewModel
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
import com.grateful.deadly.services.search.platform.ShowSearchDao
// Phase 3: Universal Services + Platform Tools
import com.grateful.deadly.services.archive.platform.NetworkClient
import com.grateful.deadly.services.archive.platform.CacheManager
import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.archive.ArchiveService
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.feature.showdetail.ShowDetailService
import com.grateful.deadly.feature.showdetail.ShowDetailServiceImpl

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

    single<ShowSearchDao> {
        Logger.d("CommonModule", "Creating ShowSearchDao platform tool")
        ShowSearchDao(get()) // Gets Database
    }

    // New Architecture - Universal Services
    single<FileExtractionService> {
        Logger.d("CommonModule", "Creating FileExtractionService")
        FileExtractionService(
            zipExtractor = get(),
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

    single<NewDataImportService> {
        Logger.d("CommonModule", "Creating DataImportService")
        NewDataImportService(
            showRepository = get(),
            showSearchDao = get()
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

    // Phase 3: Universal Services (Platform-agnostic business logic)
    single<CacheManager> {
        Logger.d("CommonModule", "Creating CacheManager using Okio FileSystem")
        CacheManager(
            fileSystem = get(),
            getAppFilesDir = get()
        )
    }

    single<ArchiveService> {
        Logger.d("CommonModule", "Creating ArchiveService")
        ArchiveService(
            networkClient = get(),
            cacheManager = get()
        )
    }

    single<MediaService> {
        Logger.d("CommonModule", "Creating MediaService")
        MediaService(
            platformMediaPlayer = get()
        )
    }

    single<ShowDetailService> {
        Logger.d("CommonModule", "Creating ShowDetailService")
        ShowDetailServiceImpl(
            showRepository = get(),
            archiveService = get()
        )
    }

    // Data Layer
    single<SearchRepository> {
        Logger.d("CommonModule", "Creating SearchRepository instance")
        SearchRepositoryImpl(get())
    }


    // Services
    single<SearchService> {
        Logger.d("CommonModule", "Creating SearchService (using V2 pattern)")
        SearchServiceImpl(
            showRepository = get(),
            showSearchDao = get(),
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

    factory {
        Logger.d("CommonModule", "Creating PlayerViewModel instance")
        PlayerViewModel(get())
    }
}