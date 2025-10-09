package com.grateful.deadly.navigation

import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Helper to call Swift ShowDetailBridge from Kotlin.
 *
 * This bridges Compose navigation to SwiftUI presentation.
 *
 * NOTE: This expects ShowDetailBridge to be accessible via AppPlatform.
 * The actual bridge call is handled by the iOS platform implementation.
 */
object ShowDetailBridgeHelper {

    fun showDetail(showId: String, recordingId: String?) {
        dispatch_async(dispatch_get_main_queue()) {
            try {
                // Call the platform-specific bridge implementation
                com.grateful.deadly.services.data.platform.AppPlatform.showShowDetail(showId, recordingId)

                println("✅ [ShowDetailBridgeHelper] Called Swift bridge for showId: $showId")
            } catch (e: Exception) {
                println("❌ [ShowDetailBridgeHelper] Error calling Swift bridge: ${e.message}")
            }
        }
    }
}
