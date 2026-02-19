import UIKit

enum ShareHelper {
    private static let baseURL = "https://share.thedeadly.app"

    static func shareURL(showId: String, recordingId: String? = nil) -> URL? {
        if let recordingId, !recordingId.isEmpty {
            return URL(string: "\(baseURL)/show/\(showId)/recording/\(recordingId)")
        }
        return URL(string: "\(baseURL)/show/\(showId)")
    }

    static func present(showId: String, recordingId: String? = nil, showName: String? = nil) {
        guard let url = shareURL(showId: showId, recordingId: recordingId) else { return }

        var items: [Any] = []
        if let showName {
            items.append("\(showName)\n\(url.absoluteString)")
        }
        items.append(url)

        let activityVC = UIActivityViewController(activityItems: items, applicationActivities: nil)

        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else { return }

        var presenter = rootVC
        while let presented = presenter.presentedViewController {
            presenter = presented
        }
        presenter.present(activityVC, animated: true)
    }
}
