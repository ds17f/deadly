package com.grateful.deadly.services.media

import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.platform.SavedPlaybackState

/**
 * Get saved playback state from platform storage (expect/actual pattern).
 * iOS returns SavedPlaybackState, Android returns null (uses MediaSessionService).
 */
expect fun getSavedPlaybackState(player: PlatformMediaPlayer): SavedPlaybackState?

/**
 * Save current playback state to platform storage (expect/actual pattern).
 * iOS saves to UserDefaults, Android no-op (uses MediaSessionService).
 */
expect fun saveCurrentPlaybackState(player: PlatformMediaPlayer)
