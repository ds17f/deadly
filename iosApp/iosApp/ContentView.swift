import SwiftUI
import ComposeApp

/**
 * ContentView - Main entry point for iOS app
 *
 * Uses pure SwiftUI navigation with RootNavigationView
 * Initializes Koin DI before creating any views
 */
struct ContentView: View {
    @State private var isInitialized = false

    init() {
        // Initialize Koin DI before any views are created
        IOSKoinInitKt.doInitKoin()
    }

    var body: some View {
        ZStack {
            if isInitialized {
                RootNavigationView()
            } else {
                // Simple loading splash while data initializes
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading...")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    // Debug: Add skip button to bypass if stuck
                    Button("Skip (Debug)") {
                        print("🔍 ⏭️ User skipped initialization")
                        isInitialized = true
                    }
                    .font(.caption2)
                    .foregroundColor(.blue)
                    .padding(.top, 20)
                }
            }
        }
        .onAppear {
            // Trigger data sync on first launch
            if !isInitialized {
                Task {
                    print("🔍 📱 ContentView: Starting data initialization...")
                    await initializeData()
                    print("🔍 📱 ContentView: Data initialization completed, showing main UI")
                    isInitialized = true
                }
            }
        }
    }

    private func initializeData() async {
        // Get DataSyncOrchestrator from Koin and synchronize data
        let orchestrator = KoinHelper.shared.getDataSyncOrchestrator()
        do {
            // Call syncData() which is a suspend function exposed to Swift
            let result = try await orchestrator.syncData()
            print("🔍 ✅ Data synchronization completed: \(result)")
        } catch {
            print("🔍 ❌ Data synchronization failed: \(error)")
            // Still mark as initialized so user can access the app
            // The UI will show empty states and retry buttons
        }
    }
}



