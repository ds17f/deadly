import UIKit
import SwiftUI
import ComposeApp

/**
 * Bridge to present SwiftUI ShowDetailView from Compose navigation.
 *
 * This allows us to gradually migrate iOS screens to SwiftUI
 * while keeping the rest in Compose Multiplatform.
 */
@objc public class ShowDetailBridge: NSObject {

    @objc public static let shared = ShowDetailBridge()

    private override init() {
        super.init()
    }

    /**
     * Present SwiftUI ShowDetailView from the current view controller.
     *
     * Called from Kotlin via AppPlatform when navigating to show detail.
     */
    @objc public func showDetail(showId: String, recordingId: String?) {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                print("❌ [ShowDetailBridge] Could not find root view controller")
                return
            }

            // Find the presented view controller (the Compose one)
            var currentVC = rootViewController
            while let presented = currentVC.presentedViewController {
                currentVC = presented
            }

            print("✅ [ShowDetailBridge] Presenting SwiftUI ShowDetailView for showId: \(showId)")

            // Get services from Koin via helper
            let showDetailService = KoinHelper.shared.getShowDetailService()
            let mediaService = KoinHelper.shared.getMediaService()
            let libraryService = KoinHelper.shared.getLibraryService()

            // Create SwiftUI view
            let showDetailView = ShowDetailView(
                showId: showId,
                recordingId: recordingId,
                showDetailService: showDetailService,
                mediaService: mediaService,
                libraryService: libraryService
            )

            // Wrap in UIHostingController and present
            let hostingController = UIHostingController(rootView: showDetailView)
            hostingController.modalPresentationStyle = .fullScreen
            currentVC.present(hostingController, animated: true)
        }
    }
}
