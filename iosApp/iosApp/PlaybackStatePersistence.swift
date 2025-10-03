import Foundation

/**
 * Persists playback state to UserDefaults for restoration after app termination.
 *
 * iOS doesn't support persistent background services like Android's MediaSessionService.
 * Instead, we save the current playback state when the app backgrounds and restore it on launch.
 *
 * Works with Kotlin's PlaybackStatePersistenceBridge via JSON serialization.
 */
@objc public class PlaybackStatePersistence: NSObject {
    @objc public static let shared = PlaybackStatePersistence()

    private let defaults = UserDefaults.standard
    private let stateKey = "playback_state_json"

    private override init() {}

    /**
     * Save playback state JSON to UserDefaults.
     * Called from Kotlin via AppPlatform bridge.
     */
    @objc public func saveStateJson(_ stateJson: String) {
        if stateJson.isEmpty {
            // Empty string signals clear
            clearPlaybackState()
            return
        }

        defaults.set(stateJson, forKey: stateKey)
        print("📱 [PERSISTENCE] Saved playback state JSON (\(stateJson.count) chars)")
    }

    /**
     * Get saved playback state JSON from UserDefaults.
     * Called from Kotlin via AppPlatform bridge.
     * Returns nil if no state was saved.
     */
    @objc public func getStateJson() -> String? {
        let stateJson = defaults.string(forKey: stateKey)
        if let json = stateJson {
            print("📱 [PERSISTENCE] Retrieved playback state JSON (\(json.count) chars)")
        } else {
            print("📱 [PERSISTENCE] No saved playback state found")
        }
        return stateJson
    }

    /**
     * Clear saved playback state.
     */
    private func clearPlaybackState() {
        defaults.removeObject(forKey: stateKey)
        print("📱 [PERSISTENCE] Cleared playback state")
    }
}
