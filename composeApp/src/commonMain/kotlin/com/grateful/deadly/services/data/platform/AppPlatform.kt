package com.grateful.deadly.services.data.platform

/**
 * Platform app registration for host-provided operations.
 *
 * The host app (iOS) registers handlers for platform-specific operations
 * that the shared Kotlin code can request.
 */
object AppPlatform {
    private var unzipHandler: UnzipRequestHandler? = null
    private var savePlaybackStateHandler: SavePlaybackStateHandler? = null
    private var getPlaybackStateHandler: GetPlaybackStateHandler? = null
    private var smartPlayerHandler: SmartPlayerRequestHandler? = null

    // Media player event handlers
    private var trackChangeHandler: MediaPlayerTrackChangeHandler? = null
    private var playbackStateChangeHandler: MediaPlayerPlaybackStateChangeHandler? = null
    private var mediaReadyHandler: MediaPlayerMediaReadyHandler? = null

    // Navigation handlers for presenting SwiftUI views (iOS only)
    private var showDetailHandler: ShowDetailPresentationHandler? = null
    private var playerHandler: PlayerPresentationHandler? = null
    private var homeHandler: HomePresentationHandler? = null

    /**
     * Register a handler for unzip requests from Kotlin.
     * Should be called during app initialization by the host app.
     */
    fun registerUnzipRequestHandler(handler: UnzipRequestHandler) {
        unzipHandler = handler
    }

    /**
     * Register handlers for playback state persistence (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerPlaybackStateHandlers(
        saveHandler: SavePlaybackStateHandler,
        getHandler: GetPlaybackStateHandler
    ) {
        savePlaybackStateHandler = saveHandler
        getPlaybackStateHandler = getHandler
    }

    /**
     * Register handler for SmartQueuePlayer requests (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerSmartPlayerHandler(handler: SmartPlayerRequestHandler) {
        smartPlayerHandler = handler
    }

    /**
     * Register event handlers for media player events (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerMediaPlayerEventHandlers(
        onTrackChanged: MediaPlayerTrackChangeHandler,
        onPlaybackStateChanged: MediaPlayerPlaybackStateChangeHandler,
        onMediaReady: MediaPlayerMediaReadyHandler? = null
    ) {
        trackChangeHandler = onTrackChanged
        playbackStateChangeHandler = onPlaybackStateChanged
        mediaReadyHandler = onMediaReady
    }

    /**
     * Register handler for presenting SwiftUI ShowDetail view (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerShowDetailHandler(handler: ShowDetailPresentationHandler) {
        showDetailHandler = handler
    }

    /**
     * Register handler for presenting SwiftUI Player view (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerPlayerHandler(handler: PlayerPresentationHandler) {
        playerHandler = handler
    }

    /**
     * Register handler for presenting SwiftUI Home view (iOS only).
     * Should be called during app initialization by the host app.
     */
    fun registerHomeHandler(handler: HomePresentationHandler) {
        homeHandler = handler
    }

    /**
     * Request unzip from the registered host handler.
     * Called internally by PlatformUnzipBridge.
     */
    internal fun requestUnzipFromHost(sourcePath: String, destinationPath: String, overwrite: Boolean) {
        val handler = unzipHandler
            ?: throw IllegalStateException("No unzip handler registered. Call registerUnzipRequestHandler() in app startup.")
        handler(sourcePath, destinationPath, overwrite)
    }

    /**
     * Save playback state to platform storage (iOS UserDefaults).
     * Called internally by PlaybackStatePersistenceBridge.
     */
    internal fun savePlaybackStateToHost(stateJson: String) {
        val handler = savePlaybackStateHandler
            ?: return // Not registered - Android doesn't need this
        handler(stateJson)
    }

    /**
     * Get saved playback state from platform storage (iOS UserDefaults).
     * Called internally by PlaybackStatePersistenceBridge.
     * Returns null if no state saved or handler not registered.
     */
    internal fun getPlaybackStateFromHost(): String? {
        val handler = getPlaybackStateHandler ?: return null // Not registered - Android doesn't need this
        return handler()
    }

    /**
     * Send SmartQueuePlayer command to platform handler.
     * Called internally by SmartQueuePlayerBridge.
     * Returns result string from platform handler.
     */
    internal fun sendSmartPlayerCommand(commandJson: String): String {
        val handler = smartPlayerHandler
            ?: throw IllegalStateException("No SmartPlayer handler registered. Call registerSmartPlayerHandler() in app startup.")
        return handler(commandJson)
    }

    /**
     * Notify registered handlers of track change event.
     * Called from Swift when track changes (manual navigation or auto-advance).
     */
    fun notifyTrackChanged(newIndex: Int) {
        println("🎯 🟢 [S→K] AppPlatform.notifyTrackChanged: routing trackChange event to Kotlin: newIndex=$newIndex")
        trackChangeHandler?.invoke(newIndex)
    }

    /**
     * Notify registered handlers of playback state change event.
     * Called from Swift when play/pause state changes.
     */
    fun notifyPlaybackStateChanged(isPlaying: Boolean) {
        playbackStateChangeHandler?.invoke(isPlaying)
    }

    /**
     * Notify registered handlers that media is ready to play.
     * Called from Swift when AVPlayerItem transitions to .readyToPlay state.
     */
    fun notifyMediaReady() {
        println("🎯 🟢 [S→K] AppPlatform.notifyMediaReady: routing mediaReady event to Kotlin")
        mediaReadyHandler?.invoke()
    }

    /**
     * Present SwiftUI ShowDetail view via registered handler.
     * Called from Kotlin navigation to present native iOS view.
     */
    fun showShowDetail(showId: String, recordingId: String?) {
        val handler = showDetailHandler
            ?: return // Not registered - Android doesn't need this
        handler(showId, recordingId)
    }

    /**
     * Present SwiftUI Player view via registered handler.
     * Called from Kotlin navigation to present native iOS view.
     */
    fun showPlayer(onNavigateToShowDetail: (String, String?) -> Unit) {
        val handler = playerHandler
            ?: return // Not registered - Android doesn't need this
        handler(onNavigateToShowDetail)
    }

    /**
     * Present SwiftUI Home view via registered handler.
     * Called from Kotlin navigation to present native iOS view.
     */
    fun showHome() {
        val handler = homeHandler
            ?: return // Not registered - Android doesn't need this
        handler()
    }
}

/**
 * Handler for unzip requests from Kotlin to host platform.
 */
typealias UnzipRequestHandler = (sourcePath: String, destinationPath: String, overwrite: Boolean) -> Unit

/**
 * Handler for saving playback state to platform storage.
 */
typealias SavePlaybackStateHandler = (stateJson: String) -> Unit

/**
 * Handler for retrieving saved playback state from platform storage.
 */
typealias GetPlaybackStateHandler = () -> String?

/**
 * Handler for SmartQueuePlayer commands from Kotlin to host platform.
 * Takes JSON command string and returns JSON result string.
 */
typealias SmartPlayerRequestHandler = (commandJson: String) -> String

/**
 * Handler for media player track change events.
 * Called when track changes due to manual navigation or auto-advance.
 */
typealias MediaPlayerTrackChangeHandler = (newIndex: Int) -> Unit

/**
 * Handler for media player playback state change events.
 * Called when play/pause state changes.
 */
typealias MediaPlayerPlaybackStateChangeHandler = (isPlaying: Boolean) -> Unit

/**
 * Handler for media player ready events.
 * Called when AVPlayerItem is ready to play (finished loading/buffering).
 */
typealias MediaPlayerMediaReadyHandler = () -> Unit

/**
 * Handler for presenting SwiftUI ShowDetail view.
 * Called from Kotlin navigation to present native iOS view.
 */
typealias ShowDetailPresentationHandler = (showId: String, recordingId: String?) -> Unit

/**
 * Handler for presenting SwiftUI Player view.
 * Called from Kotlin navigation to present native iOS view.
 */
typealias PlayerPresentationHandler = (onNavigateToShowDetail: (String, String?) -> Unit) -> Unit

/**
 * Handler for presenting SwiftUI Home view.
 * Called from Kotlin navigation to present native iOS view.
 */
typealias HomePresentationHandler = () -> Unit
