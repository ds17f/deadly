package com.grateful.deadly.services.media

import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.platform.SavedPlaybackState

/**
 * Android doesn't need playback state persistence - MediaSessionService handles it.
 */
actual fun getSavedPlaybackState(player: PlatformMediaPlayer): SavedPlaybackState? = null

/**
 * Android doesn't need playback state persistence - MediaSessionService handles it.
 */
actual fun saveCurrentPlaybackState(player: PlatformMediaPlayer) {
    // No-op on Android
}
