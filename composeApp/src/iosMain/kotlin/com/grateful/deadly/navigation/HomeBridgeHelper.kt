package com.grateful.deadly.navigation

import com.grateful.deadly.services.data.platform.AppPlatform
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Helper to call Swift HomeBridge via AppPlatform.
 *
 * Used by DeadlyNavHost.ios.kt to present native SwiftUI HomeView
 * instead of Compose HomeScreen.
 */
object HomeBridgeHelper {
    fun showHome() {
        dispatch_async(dispatch_get_main_queue()) {
            try {
                AppPlatform.showHome()
                println("✅ [HomeBridgeHelper] Called Swift bridge to show Home")
            } catch (e: Exception) {
                println("❌ [HomeBridgeHelper] Error calling Swift bridge: ${e.message}")
            }
        }
    }
}
