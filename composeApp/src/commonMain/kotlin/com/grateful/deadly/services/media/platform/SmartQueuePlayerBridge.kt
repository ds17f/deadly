package com.grateful.deadly.services.media.platform

import com.grateful.deadly.services.data.platform.AppPlatform
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bridge for SmartQueuePlayer using the AppPlatform handler pattern.
 *
 * This allows Kotlin code to control the iOS SmartQueuePlayer via registered Swift handlers.
 * Uses JSON-based communication for cross-platform compatibility.
 */
object SmartQueuePlayerBridge {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Replace current playlist with new URLs and metadata, start playback.
     * Stops any existing playback and creates new global player instance.
     *
     * @param urls List of track URLs (can be remote https:// or local file://)
     * @param metadata List of rich track metadata for professional notifications
     * @param startIndex Index to start playback from
     */
    fun replacePlaylist(urls: List<String>, metadata: List<TrackMetadata>, startIndex: Int) {
        val command = SmartPlayerCommand(
            action = "replacePlaylist",
            playerId = "global", // Fixed ID since we use single instance
            urls = urls,
            trackMetadata = metadata,
            startIndex = startIndex
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Stop playback and release player resources.
     */
    fun stop() {
        val command = SmartPlayerCommand(
            action = "stop",
            playerId = "global"
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Start or resume playback.
     */
    fun play() {
        val command = SmartPlayerCommand(action = "play", playerId = "global")
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Pause playback.
     */
    fun pause() {
        val command = SmartPlayerCommand(action = "pause", playerId = "global")
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Skip to next track.
     *
     * @return true if successfully advanced, false if at end of playlist
     */
    fun playNext(): Boolean {
        val command = SmartPlayerCommand(action = "playNext", playerId = "global")
        val commandJson = json.encodeToString(command)
        val result = AppPlatform.sendSmartPlayerCommand(commandJson)
        return result == "true"
    }

    /**
     * Skip to previous track or restart current track.
     *
     * @return true if action was successful
     */
    fun playPrevious(): Boolean {
        val command = SmartPlayerCommand(action = "playPrevious", playerId = "global")
        val commandJson = json.encodeToString(command)
        val result = AppPlatform.sendSmartPlayerCommand(commandJson)
        return result == "true"
    }

    /**
     * Seek to specific position in current track.
     *
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        val command = SmartPlayerCommand(
            action = "seek",
            playerId = "global",
            positionMs = positionMs
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Get current playback state.
     *
     * @return SmartPlayerState with current playback info
     */
    fun getPlaybackState(): SmartPlayerState {
        val command = SmartPlayerCommand(action = "getState", playerId = "global")
        val commandJson = json.encodeToString(command)
        val resultJson = AppPlatform.sendSmartPlayerCommand(commandJson)

        return try {
            json.decodeFromString<SmartPlayerState>(resultJson)
        } catch (e: Exception) {
            SmartPlayerState() // Return default state on parse error
        }
    }

    /**
     * Set track changed callback ID.
     * The actual callback will be handled via Swift and routed back through AppPlatform.
     */
    fun setTrackChangedCallback(callbackId: String) {
        val command = SmartPlayerCommand(
            action = "setTrackCallback",
            playerId = "global",
            callbackId = callbackId
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Set playlist ended callback ID.
     */
    fun setPlaylistEndedCallback(callbackId: String) {
        val command = SmartPlayerCommand(
            action = "setEndCallback",
            playerId = "global",
            callbackId = callbackId
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Release player resources (replaced by stop()).
     * @deprecated Use stop() instead for clearer semantics
     */
    @Deprecated("Use stop() instead", ReplaceWith("stop()"))
    fun releasePlayer() {
        stop()
    }
}

/**
 * Command structure for SmartQueuePlayer operations.
 */
@Serializable
data class SmartPlayerCommand(
    val action: String,
    val playerId: String,
    val urls: List<String>? = null,
    val trackMetadata: List<TrackMetadata>? = null,
    val startIndex: Int? = null,
    val positionMs: Long? = null,
    val callbackId: String? = null
)

/**
 * Rich metadata for tracks passed to iOS for professional notification display.
 */
@Serializable
data class TrackMetadata(
    val title: String,
    val artist: String = "Grateful Dead",
    val album: String,  // "May 8, 1977 - Barton Hall"
    val venue: String,
    val date: String,
    val duration: Long?,
    val recordingId: String,
    val showId: String
)

/**
 * State structure for SmartQueuePlayer status.
 */
@Serializable
data class SmartPlayerState(
    val isPlaying: Boolean = false,
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val trackIndex: Int = 0,
    val trackCount: Int = 0
)