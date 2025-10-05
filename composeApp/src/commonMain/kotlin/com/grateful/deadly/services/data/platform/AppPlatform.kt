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
