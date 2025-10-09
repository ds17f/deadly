package com.grateful.deadly.di

import com.grateful.deadly.domain.home.HomeService
import com.grateful.deadly.domain.search.SearchService
import com.grateful.deadly.feature.search.SearchViewModel
import com.grateful.deadly.feature.showdetail.ShowDetailService
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.services.library.LibraryService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper for Swift code to access Koin-managed instances.
 *
 * Swift can't directly access Koin, so this provides simple getter functions.
 */
object KoinHelper : KoinComponent {

    fun getShowDetailService(): ShowDetailService {
        val service: ShowDetailService by inject()
        return service
    }

    fun getMediaService(): MediaService {
        val service: MediaService by inject()
        return service
    }

    fun getLibraryService(): LibraryService {
        val service: LibraryService by inject()
        return service
    }

    fun getHomeService(): HomeService {
        val service: HomeService by inject()
        return service
    }

    fun getRecentShowsService(): com.grateful.deadly.services.data.RecentShowsService {
        val service: com.grateful.deadly.services.data.RecentShowsService by inject()
        return service
    }

    fun getSearchService(): SearchService {
        val service: SearchService by inject()
        return service
    }

    fun getSearchViewModel(): SearchViewModel {
        val viewModel: SearchViewModel by inject()
        return viewModel
    }

    fun getLibraryViewModel(): com.grateful.deadly.feature.library.LibraryViewModel {
        val viewModel: com.grateful.deadly.feature.library.LibraryViewModel by inject()
        return viewModel
    }

    fun getRecordingSelectionService(): com.grateful.deadly.services.recording.RecordingSelectionService {
        val service: com.grateful.deadly.services.recording.RecordingSelectionService by inject()
        return service
    }
}
