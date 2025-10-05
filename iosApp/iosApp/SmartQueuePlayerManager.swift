import Foundation

/**
 * Manager for single SmartQueuePlayer instance to handle commands from Kotlin.
 * Uses single global player to prevent double-playing audio streams.
 */
@objc public class SmartQueuePlayerManager: NSObject {

    @objc public static let shared = SmartQueuePlayerManager()

    private var globalPlayer: SmartQueuePlayer?
    private var trackCallback: ((Int) -> Void)?
    private var endCallback: (() -> Void)?

    private override init() {
        super.init()
    }

    /**
     * Handle SmartQueuePlayer command from Kotlin.
     * Takes JSON command and returns JSON result.
     */
    @objc public func handleCommand(_ commandJson: String) -> String {
        guard let commandData = commandJson.data(using: .utf8),
              let command = try? JSONSerialization.jsonObject(with: commandData) as? [String: Any],
              let action = command["action"] as? String,
              let playerId = command["playerId"] as? String else {
            return "error: invalid command"
        }

        switch action {
        case "create":
            return handleCreate(playerId: playerId, command: command)
        case "replacePlaylist":
            return handleReplacePlaylist(command: command)
        case "stop":
            return handleStop()
        case "play":
            return handlePlay(playerId: playerId)
        case "pause":
            return handlePause(playerId: playerId)
        case "playNext":
            return handlePlayNext(playerId: playerId)
        case "playPrevious":
            return handlePlayPrevious(playerId: playerId)
        case "seek":
            return handleSeek(playerId: playerId, command: command)
        case "getState":
            return handleGetState(playerId: playerId)
        case "setTrackCallback":
            return handleSetTrackCallback(playerId: playerId, command: command)
        case "setEndCallback":
            return handleSetEndCallback(playerId: playerId, command: command)
        case "release":
            return handleRelease(playerId: playerId)
        default:
            return "error: unknown action"
        }
    }

    private func handleCreate(playerId: String, command: [String: Any]) -> String {
        guard let urls = command["urls"] as? [String],
              let startIndex = command["startIndex"] as? Int else {
            return "error: missing urls or startIndex"
        }

        // Extract metadata if available
        let metadata = command["trackMetadata"] as? [[String: Any]] ?? []

        // Stop existing player if any
        globalPlayer?.pause()

        // Create new global player instance with metadata
        let player = SmartQueuePlayer(urls: urls, startIndex: startIndex, metadata: metadata)
        globalPlayer = player

        // Set up internal callbacks to route to registered Kotlin callbacks
        player.onTrackChanged = { [weak self] newIndex in
            self?.trackCallback?(newIndex)
        }

        player.onPlaylistEnded = { [weak self] in
            self?.endCallback?()
        }

        return "success"
    }

    private func handleReplacePlaylist(command: [String: Any]) -> String {
        guard let urls = command["urls"] as? [String],
              let startIndex = command["startIndex"] as? Int else {
            return "error: missing urls or startIndex"
        }

        // Extract metadata if available
        let metadata = command["trackMetadata"] as? [[String: Any]] ?? []
        print("📱 [METADATA] Received \(metadata.count) metadata items")
        if !metadata.isEmpty {
            print("📱 [METADATA] First item: \(metadata[0])")
        }

        // Stop existing player if any
        globalPlayer?.pause()

        // Create new global player instance with metadata
        let player = SmartQueuePlayer(urls: urls, startIndex: startIndex, metadata: metadata)
        globalPlayer = player

        // Set up internal callbacks to route to registered Kotlin callbacks
        player.onTrackChanged = { [weak self] newIndex in
            self?.trackCallback?(newIndex)
        }

        player.onPlaylistEnded = { [weak self] in
            self?.endCallback?()
        }

        return "success"
    }

    private func handleStop() -> String {
        globalPlayer?.pause()
        globalPlayer = nil
        trackCallback = nil
        endCallback = nil
        return "success"
    }

    private func handlePlay(playerId: String) -> String {
        guard let player = globalPlayer else {
            return "error: no active player"
        }
        player.play()
        return "success"
    }

    private func handlePause(playerId: String) -> String {
        guard let player = globalPlayer else {
            return "error: no active player"
        }
        player.pause()
        return "success"
    }

    private func handlePlayNext(playerId: String) -> String {
        guard let player = globalPlayer else {
            return "error: no active player"
        }
        let success = player.playNext()
        return success ? "true" : "false"
    }

    private func handlePlayPrevious(playerId: String) -> String {
        guard let player = globalPlayer else {
            return "error: no active player"
        }
        let success = player.playPrevious()
        return success ? "true" : "false"
    }

    private func handleSeek(playerId: String, command: [String: Any]) -> String {
        guard let player = globalPlayer,
              let positionMs = command["positionMs"] as? Int else {
            return "error: no active player or missing positionMs"
        }
        let positionSeconds = Double(positionMs) / 1000.0
        player.seek(to: positionSeconds)
        return "success"
    }

    private func handleGetState(playerId: String) -> String {
        guard let player = globalPlayer else {
            return "{\"isPlaying\":false,\"currentTime\":0,\"duration\":0,\"trackIndex\":0,\"trackCount\":0}"
        }

        let state = [
            "isPlaying": player.isPlaying,
            "currentTime": player.currentTime,
            "duration": player.duration,
            "trackIndex": player.trackIndex,
            "trackCount": player.trackCount
        ] as [String : Any]

        if let stateData = try? JSONSerialization.data(withJSONObject: state),
           let stateJson = String(data: stateData, encoding: .utf8) {
            return stateJson
        }

        return "{\"isPlaying\":false,\"currentTime\":0,\"duration\":0,\"trackIndex\":0,\"trackCount\":0}"
    }

    private func handleSetTrackCallback(playerId: String, command: [String: Any]) -> String {
        guard let callbackId = command["callbackId"] as? String else {
            return "error: missing callbackId"
        }

        // Store callback that will notify Kotlin when track changes
        trackCallback = { newIndex in
            // TODO: Notify Kotlin via callback mechanism
            print("Track changed: \(newIndex)")
        }

        return "success"
    }

    private func handleSetEndCallback(playerId: String, command: [String: Any]) -> String {
        guard let callbackId = command["callbackId"] as? String else {
            return "error: missing callbackId"
        }

        // Store callback that will notify Kotlin when playlist ends
        endCallback = {
            // TODO: Notify Kotlin via callback mechanism
            print("Playlist ended")
        }

        return "success"
    }

    private func handleRelease(playerId: String) -> String {
        globalPlayer?.pause()
        globalPlayer = nil
        trackCallback = nil
        endCallback = nil
        return "success"
    }
}