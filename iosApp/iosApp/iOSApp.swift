import SwiftUI
import AVFoundation

@main
struct iOSApp: App {

    init() {
        setupAudioSession()
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

            try audioSession.setCategory(.playback, mode: .default, options: [])
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
}