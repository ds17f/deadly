package com.grateful.deadly.services.media

import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.platform.SavedPlaybackState

/**
 * iOS implementation - gets saved playback state from UserDefaults.
 */
actual fun getSavedPlaybackState(player: PlatformMediaPlayer): SavedPlaybackState? {
    return player.restoreSavedPlaybackState()
}

/**
 * iOS implementation - saves current playback state to UserDefaults.
 */
actual fun saveCurrentPlaybackState(player: PlatformMediaPlayer) {
    player.saveCurrentPlaybackState()
}
