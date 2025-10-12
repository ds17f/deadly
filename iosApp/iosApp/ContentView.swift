import SwiftUI
import ComposeApp

/**
 * ContentView - Main entry point for iOS app
 *
 * Shows Compose SplashScreen during initialization, then switches to
 * pure SwiftUI navigation with RootNavigationView after data sync completes.
 */
struct ContentView: View {
    @State private var showSplash = true

    init() {
        // Initialize Koin DI before any views are created
        IOSKoinInitKt.doInitKoin()
    }

    var body: some View {
        ZStack {
            if showSplash {
                // Use the Compose SplashScreen with full progress tracking
                ComposeSplashViewControllerRepresentable(
                    onSplashComplete: {
                        showSplash = false
                    }
                )
                .ignoresSafeArea()
            } else {
                RootNavigationView()
            }
        }
    }
}

/**
 * SwiftUI wrapper for the Compose SplashScreen.
 * Uses UIViewControllerRepresentable to embed the Compose UI in SwiftUI.
 */
struct ComposeSplashViewControllerRepresentable: UIViewControllerRepresentable {
    let onSplashComplete: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.createSplashViewController(
            onSplashComplete: onSplashComplete
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed
    }
}



