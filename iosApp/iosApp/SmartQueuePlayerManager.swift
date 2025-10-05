import Foundation

/**
 * Manager for SmartQueuePlayer instances to handle commands from Kotlin.
 * Manages multiple player instances and routes commands to the correct player.
 */
@objc public class SmartQueuePlayerManager: NSObject {

    @objc public static let shared = SmartQueuePlayerManager()

    private var players: [String: SmartQueuePlayer] = [:]
    private var trackCallbacks: [String: (Int) -> Void] = [:]
    private var endCallbacks: [String: () -> Void] = [:]

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

        let player = SmartQueuePlayer(urls: urls, startIndex: startIndex)
        players[playerId] = player

        // Set up internal callbacks to route to registered Kotlin callbacks
        player.onTrackChanged = { [weak self] newIndex in
            self?.trackCallbacks[playerId]?(newIndex)
        }

        player.onPlaylistEnded = { [weak self] in
            self?.endCallbacks[playerId]?()
        }

        return "success"
    }

    private func handlePlay(playerId: String) -> String {
        guard let player = players[playerId] else {
            return "error: player not found"
        }
        player.play()
        return "success"
    }

    private func handlePause(playerId: String) -> String {
        guard let player = players[playerId] else {
            return "error: player not found"
        }
        player.pause()
        return "success"
    }

    private func handlePlayNext(playerId: String) -> String {
        guard let player = players[playerId] else {
            return "error: player not found"
        }
        let success = player.playNext()
        return success ? "true" : "false"
    }

    private func handlePlayPrevious(playerId: String) -> String {
        guard let player = players[playerId] else {
            return "error: player not found"
        }
        let success = player.playPrevious()
        return success ? "true" : "false"
    }

    private func handleSeek(playerId: String, command: [String: Any]) -> String {
        guard let player = players[playerId],
              let positionMs = command["positionMs"] as? Int else {
            return "error: player not found or missing positionMs"
        }
        let positionSeconds = Double(positionMs) / 1000.0
        player.seek(to: positionSeconds)
        return "success"
    }

    private func handleGetState(playerId: String) -> String {
        guard let player = players[playerId] else {
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
        trackCallbacks[playerId] = { newIndex in
            // TODO: Notify Kotlin via callback mechanism
            print("Track changed for player \(playerId): \(newIndex)")
        }

        return "success"
    }

    private func handleSetEndCallback(playerId: String, command: [String: Any]) -> String {
        guard let callbackId = command["callbackId"] as? String else {
            return "error: missing callbackId"
        }

        // Store callback that will notify Kotlin when playlist ends
        endCallbacks[playerId] = {
            // TODO: Notify Kotlin via callback mechanism
            print("Playlist ended for player \(playerId)")
        }

        return "success"
    }

    private func handleRelease(playerId: String) -> String {
        players.removeValue(forKey: playerId)
        trackCallbacks.removeValue(forKey: playerId)
        endCallbacks.removeValue(forKey: playerId)
        return "success"
    }
}