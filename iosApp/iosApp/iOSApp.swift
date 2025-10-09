import SwiftUI
import AVFoundation
import ComposeApp

@main
struct iOSApp: App {
    @State private var hasStartedTracking = false

    init() {
        setupAudioSession()
        setupUnzipHandler()
        setupPlaybackStatePersistence()
        setupSmartQueuePlayerHandler()
        setupLifecycleObservers()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // Start tracking after UI is initialized (only once)
                    if !hasStartedTracking {
                        hasStartedTracking = true
                        // Delay slightly to ensure all services are ready
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            startRecentShowsTracking()
                        }
                    }
                }
        }
    }

    private func setupAudioSession() {
        do {
            let audioSession = AVAudioSession.sharedInstance()

            print("iOS Audio session before setup:")
            print("  Category: \(audioSession.category)")
            print("  Mode: \(audioSession.mode)")
            print("  Options: \(audioSession.categoryOptions)")
            print("  Is active: \(audioSession.isOtherAudioPlaying)")

            try audioSession.setCategory(.playback, mode: .default, options: [.allowAirPlay])
            try audioSession.setActive(true)

            print("iOS Audio session after setup:")
            print("  Category: \(audioSession.category)")
            print("  Mode: \(audioSession.mode)")
            print("  Options: \(audioSession.categoryOptions)")
            print("  Output volume: \(audioSession.outputVolume)")
            print("  iOS Audio session configured for playback successfully")
        } catch {
            print("Failed to setup audio session: \(error)")
        }
    }

    private func setupUnzipHandler() {
        AppPlatform.shared.registerUnzipRequestHandler { sourcePath, destinationPath, overwrite in
            let srcURL = NSURL.fileURL(withPath: sourcePath as String) as NSURL
            let destURL = NSURL.fileURL(withPath: destinationPath as String) as NSURL
            let shouldOverwrite = overwrite.boolValue

            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    let extracted = try UnzipHelper.unzipFile(
                        at: srcURL,
                        to: destURL,
                        overwriteExisting: shouldOverwrite
                    )
                    // Call back into Kotlin
                    PlatformUnzipBridge.shared.reportUnzipResult(
                        path: extracted.path,
                        errorMsg: nil
                    )
                } catch {
                    PlatformUnzipBridge.shared.reportUnzipResult(
                        path: nil,
                        errorMsg: error.localizedDescription
                    )
                }
            }
        }
    }

    private func setupPlaybackStatePersistence() {
        // Register handlers for playback state persistence (iOS only)
        AppPlatform.shared.registerPlaybackStateHandlers(
            saveHandler: { stateJson in
                PlaybackStatePersistence.shared.saveStateJson(stateJson)
            },
            getHandler: {
                return PlaybackStatePersistence.shared.getStateJson()
            }
        )
    }

    private func setupSmartQueuePlayerHandler() {
        // Register handler for SmartQueuePlayer commands from Kotlin
        AppPlatform.shared.registerSmartPlayerHandler { commandJson in
            return SmartQueuePlayerManager.shared.handleCommand(commandJson)
        }
    }

    private func startRecentShowsTracking() {
        // Start RecentShowsService tracking for home screen recent shows
        print("📱 Starting RecentShowsService tracking...")
        let recentShowsService = KoinHelper.shared.getRecentShowsService()
        recentShowsService.startTracking()
        print("📱 RecentShowsService tracking started")
    }

    private func setupLifecycleObservers() {
        // Save playback state when app goes to background
        NotificationCenter.default.addObserver(
            forName: UIApplication.willResignActiveNotification,
            object: nil,
            queue: .main
        ) { _ in
            // Call MediaService via holder to save state
            MediaServiceHolder.shared.savePlaybackState()
            print("📱 [LIFECYCLE] Saved playback state on background")
        }
    }
}