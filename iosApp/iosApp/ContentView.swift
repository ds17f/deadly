import SwiftUI
import ComposeApp

/**
 * ContentView - Main entry point for iOS app
 *
 * Uses pure SwiftUI navigation with RootNavigationView
 * Initializes Koin DI before creating any views
 */
struct ContentView: View {
    init() {
        // Initialize Koin DI before any views are created
        IOSKoinInitKt.doInitKoin()
    }

    var body: some View {
        RootNavigationView()
    }
}



