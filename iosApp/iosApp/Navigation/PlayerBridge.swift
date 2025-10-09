import Foundation
import UIKit
import SwiftUI
import ComposeApp

/**
 * Bridge to present SwiftUI PlayerView from Compose navigation.
 *
 * Called from Kotlin when navigating to Player screen.
 * Creates UIHostingController and presents it fullscreen.
 */
@objc public class PlayerBridge: NSObject {
    @objc public static let shared = PlayerBridge()

    @objc public func showPlayer(onNavigateToShowDetail: @escaping (String, String?) -> Void) {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                print("❌ [PlayerBridge] Could not find root view controller")
                return
            }

            var currentVC = rootViewController
            while let presented = currentVC.presentedViewController {
                currentVC = presented
            }

            print("✅ [PlayerBridge] Presenting SwiftUI PlayerView")

            // Get MediaService from Koin
            let mediaService = KoinHelper.shared.getMediaService()

            // Create SwiftUI view
            let playerView = PlayerView(
                mediaService: mediaService,
                onNavigateToShowDetail: onNavigateToShowDetail
            )

            let hostingController = UIHostingController(rootView: playerView)
            hostingController.modalPresentationStyle = .fullScreen
            currentVC.present(hostingController, animated: true)
        }
    }
}
