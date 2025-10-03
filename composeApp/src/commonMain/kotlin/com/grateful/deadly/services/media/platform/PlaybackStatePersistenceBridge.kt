package com.grateful.deadly.services.media.platform

import com.grateful.deadly.services.data.platform.AppPlatform
import com.grateful.deadly.services.media.EnrichedTrack
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bridge for iOS playback state persistence using the AppPlatform handler pattern.
 *
 * Similar to PlatformUnzipBridge, this allows Kotlin code to save/restore playback state
 * to/from iOS UserDefaults via registered Swift handlers.
 *
 * Android doesn't use this - MediaSessionService persists state automatically.
 */
object PlaybackStatePersistenceBridge {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Save current playback state to platform storage (iOS UserDefaults).
     *
     * @param enrichedTracks Current playlist
     * @param trackIndex Current track index in playlist
     * @param positionMs Current playback position in milliseconds
     * @param showId Current show ID
     * @param recordingId Current recording ID
     * @param format Current audio format
     */
    fun saveState(
        enrichedTracks: List<EnrichedTrack>,
        trackIndex: Int,
        positionMs: Long,
        showId: String,
        recordingId: String,
        format: String
    ) {
        if (enrichedTracks.isEmpty() || trackIndex < 0 || trackIndex >= enrichedTracks.size) {
            return // Invalid state - don't save
        }

        val state = SavedPlaybackState(
            enrichedTracks = enrichedTracks,
            trackIndex = trackIndex,
            positionMs = positionMs,
            showId = showId,
            recordingId = recordingId,
            format = format
        )

        val stateJson = json.encodeToString(state)
        AppPlatform.savePlaybackStateToHost(stateJson)
    }

    /**
     * Restore saved playback state from platform storage (iOS UserDefaults).
     *
     * Returns null if no state was saved or if state is invalid.
     */
    fun restoreState(): SavedPlaybackState? {
        val stateJson = AppPlatform.getPlaybackStateFromHost() ?: return null

        return try {
            json.decodeFromString<SavedPlaybackState>(stateJson)
        } catch (e: Exception) {
            null // Failed to parse - state is corrupt
        }
    }

    /**
     * Clear saved playback state.
     *
     * Called when user explicitly stops playback.
     */
    fun clearState() {
        AppPlatform.savePlaybackStateToHost("") // Empty string signals clear
    }
}

/**
 * Serializable playback state for persistence.
 */
@Serializable
data class SavedPlaybackState(
    val enrichedTracks: List<EnrichedTrack>,
    val trackIndex: Int,
    val positionMs: Long,
    val showId: String,
    val recordingId: String,
    val format: String
)
