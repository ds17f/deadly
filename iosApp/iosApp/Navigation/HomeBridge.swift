import Foundation
import UIKit
import SwiftUI
import ComposeApp

/**
 * Bridge to present native SwiftUI HomeView from Compose navigation.
 *
 * Called from Kotlin DeadlyNavHost.ios.kt when navigating to Home screen.
 * Uses UIHostingController to present SwiftUI view in UIKit hierarchy.
 */
@objc public class HomeBridge: NSObject {
    @objc public static let shared = HomeBridge()

    /**
     * Present SwiftUI HomeView as full screen
     */
    @objc public func showHome() {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                print("❌ [HomeBridge] No root view controller found")
                return
            }

            // Get current view controller (might be presented VC)
            var currentVC = rootViewController
            while let presentedVC = currentVC.presentedViewController {
                currentVC = presentedVC
            }

            // Get HomeService from Koin
            let homeService = KoinHelper.shared.getHomeService()

            // Create SwiftUI view
            let homeView = HomeView(homeService: homeService)

            // Present as full screen
            let hostingController = UIHostingController(rootView: homeView)
            hostingController.modalPresentationStyle = .fullScreen

            currentVC.present(hostingController, animated: true) {
                print("✅ [HomeBridge] Presented SwiftUI HomeView")
            }
        }
    }
}
