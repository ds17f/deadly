package com.grateful.deadly.services.data.platform

/**
 * Platform app registration for host-provided operations.
 *
 * The host app (iOS) registers handlers for platform-specific operations
 * that the shared Kotlin code can request.
 */
object AppPlatform {
    private var unzipHandler: UnzipRequestHandler? = null

    /**
     * Register a handler for unzip requests from Kotlin.
     * Should be called during app initialization by the host app.
     */
    fun registerUnzipRequestHandler(handler: UnzipRequestHandler) {
        unzipHandler = handler
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
}

/**
 * Handler for unzip requests from Kotlin to host platform.
 */
typealias UnzipRequestHandler = (sourcePath: String, destinationPath: String, overwrite: Boolean) -> Unit
