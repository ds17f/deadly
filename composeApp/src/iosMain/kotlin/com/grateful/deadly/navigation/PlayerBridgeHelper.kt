package com.grateful.deadly.navigation

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Helper to call Swift PlayerBridge from Kotlin.
 *
 * This bridges Compose navigation to SwiftUI presentation.
 */
object PlayerBridgeHelper {

    fun showPlayer(onNavigateToShowDetail: (String, String?) -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            try {
                // Call the platform-specific bridge implementation via AppPlatform
                com.grateful.deadly.services.data.platform.AppPlatform.showPlayer(onNavigateToShowDetail)

                println("✅ [PlayerBridgeHelper] Called Swift bridge to show Player")
            } catch (e: Exception) {
                println("❌ [PlayerBridgeHelper] Error calling Swift bridge: ${e.message}")
            }
        }
    }
}
