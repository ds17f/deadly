package com.grateful.deadly.di

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.domain.home.HomeService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.feature.player.PlayerViewModel
import com.grateful.deadly.feature.home.HomeViewModel
import com.grateful.deadly.services.search.SearchServiceImpl
import com.grateful.deadly.services.home.HomeServiceImpl
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
import com.grateful.deadly.services.data.RecentShowsService
import com.grateful.deadly.services.data.RecentShowsServiceImpl
import com.grateful.deadly.feature.showdetail.ShowDetailService
import com.grateful.deadly.feature.showdetail.ShowDetailServiceImpl
import com.grateful.deadly.feature.splash.SplashService
import com.grateful.deadly.feature.splash.SplashViewModel
// Library system imports (V2 pattern)
import com.grateful.deadly.services.library.platform.LibraryRepository
import com.grateful.deadly.services.library.LibraryService
import com.grateful.deadly.services.library.LibraryServiceImpl
import com.grateful.deadly.feature.library.LibraryViewModel
import com.grateful.deadly.feature.showdetail.ShowDetailViewModel

import com.grateful.deadly.core.design.theme.ThemeManager
import okio.FileSystem
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    // Application scope for long-running services
    single<CoroutineScope> {
        Logger.d("CommonModule", "Creating application CoroutineScope")
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
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

    single<RecentShowsService> {
        Logger.d("CommonModule", "Creating RecentShowsService")
        RecentShowsServiceImpl(
            showRepository = get(),
            mediaService = get(),
            applicationScope = get()
        )
    }

    single<ShowDetailService> {
        Logger.d("CommonModule", "Creating ShowDetailService")
        ShowDetailServiceImpl(
            showRepository = get(),
            archiveService = get()
        )
    }

    single<SplashService> {
        Logger.d("CommonModule", "Creating SplashService")
        SplashService(
            dataSyncOrchestrator = get()
        )
    }

    // Library system (V2 pattern) - Platform Tool + Universal Service
    single<LibraryRepository> {
        Logger.d("CommonModule", "Creating LibraryRepository platform tool")
        LibraryRepository(database = get()) // Gets Database
    }

    single<LibraryService> {
        Logger.d("CommonModule", "Creating LibraryService")
        LibraryServiceImpl(
            libraryRepository = get(),
            coroutineScope = get()
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

    single<HomeService> {
        Logger.d("CommonModule", "Creating HomeService")
        HomeServiceImpl(
            showRepository = get(),
            recentShowsService = get(),
            mediaService = get()
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
        Logger.d("CommonModule", "Creating HomeViewModel instance")
        HomeViewModel(get())
    }

    factory {
        Logger.d("CommonModule", "Creating PlayerViewModel instance")
        PlayerViewModel(get())
    }

    factory {
        Logger.d("CommonModule", "Creating SplashViewModel instance")
        SplashViewModel(get())
    }

    factory {
        Logger.d("CommonModule", "Creating LibraryViewModel instance")
        LibraryViewModel(get())
    }

    factory {
        Logger.d("CommonModule", "Creating ShowDetailViewModel instance")
        ShowDetailViewModel(
            showDetailService = get(),
            mediaService = get(),
            libraryService = get()
        )
    }
}