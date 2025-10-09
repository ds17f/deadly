package com.grateful.deadly.di

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
}
