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
     * Create and initialize SmartQueuePlayer with playlist URLs.
     *
     * @param urls List of track URLs (can be remote https:// or local file://)
     * @param startIndex Index to start playback from
     * @return Player ID for subsequent operations
     */
    fun createPlayer(urls: List<String>, startIndex: Int): String {
        val playerId = "player_${Clock.System.now().toEpochMilliseconds()}"
        val command = SmartPlayerCommand(
            action = "create",
            playerId = playerId,
            urls = urls,
            startIndex = startIndex
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
        return playerId
    }

    /**
     * Start or resume playback.
     */
    fun play(playerId: String) {
        val command = SmartPlayerCommand(action = "play", playerId = playerId)
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Pause playback.
     */
    fun pause(playerId: String) {
        val command = SmartPlayerCommand(action = "pause", playerId = playerId)
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Skip to next track.
     *
     * @return true if successfully advanced, false if at end of playlist
     */
    fun playNext(playerId: String): Boolean {
        val command = SmartPlayerCommand(action = "playNext", playerId = playerId)
        val commandJson = json.encodeToString(command)
        val result = AppPlatform.sendSmartPlayerCommand(commandJson)
        return result == "true"
    }

    /**
     * Skip to previous track or restart current track.
     *
     * @return true if action was successful
     */
    fun playPrevious(playerId: String): Boolean {
        val command = SmartPlayerCommand(action = "playPrevious", playerId = playerId)
        val commandJson = json.encodeToString(command)
        val result = AppPlatform.sendSmartPlayerCommand(commandJson)
        return result == "true"
    }

    /**
     * Seek to specific position in current track.
     *
     * @param positionMs Position in milliseconds
     */
    fun seekTo(playerId: String, positionMs: Long) {
        val command = SmartPlayerCommand(
            action = "seek",
            playerId = playerId,
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
    fun getPlaybackState(playerId: String): SmartPlayerState {
        val command = SmartPlayerCommand(action = "getState", playerId = playerId)
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
    fun setTrackChangedCallback(playerId: String, callbackId: String) {
        val command = SmartPlayerCommand(
            action = "setTrackCallback",
            playerId = playerId,
            callbackId = callbackId
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Set playlist ended callback ID.
     */
    fun setPlaylistEndedCallback(playerId: String, callbackId: String) {
        val command = SmartPlayerCommand(
            action = "setEndCallback",
            playerId = playerId,
            callbackId = callbackId
        )
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
    }

    /**
     * Cleanup player instance.
     */
    fun releasePlayer(playerId: String) {
        val command = SmartPlayerCommand(action = "release", playerId = playerId)
        val commandJson = json.encodeToString(command)
        AppPlatform.sendSmartPlayerCommand(commandJson)
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
    val startIndex: Int? = null,
    val positionMs: Long? = null,
    val callbackId: String? = null
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