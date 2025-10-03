import SwiftUI
import AVFoundation
import ComposeApp

@main
struct iOSApp: App {

    init() {
        setupAudioSession()
        setupUnzipHandler()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
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
}