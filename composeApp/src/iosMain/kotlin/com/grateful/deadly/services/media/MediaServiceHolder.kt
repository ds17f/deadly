package com.grateful.deadly.services.media

/**
 * iOS-specific holder for MediaService to enable Swift lifecycle callbacks.
 *
 * Swift can't easily access Koin DI, so we store a weak reference here
 * that Swift can call to save playback state on background.
 */
object MediaServiceHolder {
    private var mediaServiceRef: MediaService? = null

    fun setMediaService(service: MediaService) {
        mediaServiceRef = service
    }

    fun savePlaybackState() {
        mediaServiceRef?.savePlaybackState()
    }
}
