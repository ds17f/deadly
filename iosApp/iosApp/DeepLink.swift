import Foundation

enum DeepLink: Equatable {
    case show(showId: String)
    case showRecording(showId: String, recordingId: String)

    /// Parses both universal links and custom scheme URLs:
    /// - https://share.thedeadly.app/show/{showId}[/recording/{recordingId}]
    /// - deadly://show/{showId}[/recording/{recordingId}]
    static func from(url: URL) -> DeepLink? {
        let pathComponents: [String]

        if url.scheme == "deadly" {
            // deadly://show/{showId}/recording/{recordingId}
            // host is "show", path is "/{showId}/recording/{recordingId}"
            guard let host = url.host(), host == "show" else { return nil }
            pathComponents = ["show"] + url.pathComponents.filter { $0 != "/" }
        } else if url.host() == "share.thedeadly.app" {
            pathComponents = url.pathComponents.filter { $0 != "/" }
        } else {
            return nil
        }

        guard pathComponents.first == "show", pathComponents.count >= 2 else { return nil }

        let showId = pathComponents[1]

        if pathComponents.count >= 4, pathComponents[2] == "recording" {
            return .showRecording(showId: showId, recordingId: pathComponents[3])
        }

        return .show(showId: showId)
    }
}
