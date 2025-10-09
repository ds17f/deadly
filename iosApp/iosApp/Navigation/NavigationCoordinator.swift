import Foundation
import SwiftUI
import ComposeApp

/**
 * NavigationCoordinator - Centralized navigation state management for iOS
 *
 * Manages SwiftUI NavigationStack path and provides navigation methods.
 * Replaces the Compose navigation intercept pattern with native SwiftUI navigation.
 */
class NavigationCoordinator: ObservableObject {
    @Published var path = NavigationPath()
    @Published var selectedTab: Tab = .home

    // Track presentation state for full-screen modals
    @Published var isPresentingPlayer = false
    @Published var isPresentingShowDetail = false
    @Published var showDetailParams: (showId: String, recordingId: String?) = ("", nil)

    enum Tab: String, CaseIterable {
        case home = "Home"
        case search = "Search"
        case library = "Library"
        case collections = "Collections"
        case settings = "Settings"

        var icon: String {
            switch self {
            case .home: return "house.fill"
            case .search: return "magnifyingglass"
            case .library: return "music.note.list"
            case .collections: return "folder.fill"
            case .settings: return "gearshape.fill"
            }
        }
    }

    enum Destination: Hashable {
        case showDetail(showId: String, recordingId: String?)
        case searchResults(query: String)
        // Add more destinations as needed

        func hash(into hasher: inout Hasher) {
            switch self {
            case .showDetail(let showId, let recordingId):
                hasher.combine("showDetail")
                hasher.combine(showId)
                hasher.combine(recordingId ?? "")
            case .searchResults(let query):
                hasher.combine("searchResults")
                hasher.combine(query)
            }
        }

        static func == (lhs: Destination, rhs: Destination) -> Bool {
            switch (lhs, rhs) {
            case (.showDetail(let lhsId, let lhsRecId), .showDetail(let rhsId, let rhsRecId)):
                return lhsId == rhsId && lhsRecId == rhsRecId
            case (.searchResults(let lhsQuery), .searchResults(let rhsQuery)):
                return lhsQuery == rhsQuery
            default:
                return false
            }
        }
    }

    // MARK: - Navigation Methods

    func navigateToShowDetail(showId: String, recordingId: String? = nil) {
        showDetailParams = (showId, recordingId)
        isPresentingShowDetail = true
    }

    func navigateToPlayer() {
        isPresentingPlayer = true
    }

    func navigateToTab(_ tab: Tab) {
        selectedTab = tab
        // Clear navigation stack when switching tabs
        path = NavigationPath()
    }

    func navigateBack() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func popToRoot() {
        path = NavigationPath()
    }
}
