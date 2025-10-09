import SwiftUI
import ComposeApp

/**
 * DeadlyTabView - Bottom tab navigation matching Android's 5-tab layout
 *
 * Tabs: Home, Search, Library, Collections, Settings
 * Uses SF Symbols for icons matching cross-platform AppIcon design
 */
struct DeadlyTabView: View {
    @ObservedObject var coordinator: NavigationCoordinator

    // Get services from Koin
    private let homeService: HomeService
    private let mediaService: MediaService

    init(coordinator: NavigationCoordinator) {
        self.coordinator = coordinator
        self.homeService = KoinHelper.shared.getHomeService()
        self.mediaService = KoinHelper.shared.getMediaService()
    }

    var body: some View {
        TabView(selection: $coordinator.selectedTab) {
            // Home Tab
            HomeView(homeService: homeService)
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(NavigationCoordinator.Tab.home)

            // Search Tab
            SearchPlaceholderView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(NavigationCoordinator.Tab.search)

            // Library Tab
            LibraryPlaceholderView()
                .tabItem {
                    Label("Library", systemImage: "music.note.list")
                }
                .tag(NavigationCoordinator.Tab.library)

            // Collections Tab
            CollectionsPlaceholderView()
                .tabItem {
                    Label("Collections", systemImage: "folder.fill")
                }
                .tag(NavigationCoordinator.Tab.collections)

            // Settings Tab
            SettingsPlaceholderView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(NavigationCoordinator.Tab.settings)
        }
        .accentColor(Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)) // DeadRed
    }
}

// MARK: - Placeholder Views (to be replaced with real implementations)

struct SearchPlaceholderView: View {
    var body: some View {
        VStack {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text("Search")
                .font(.title)
            Text("Search screen coming soon")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct LibraryPlaceholderView: View {
    var body: some View {
        VStack {
            Image(systemName: "music.note.list")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text("Library")
                .font(.title)
            Text("Library screen coming soon")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct CollectionsPlaceholderView: View {
    var body: some View {
        VStack {
            Image(systemName: "folder.fill")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text("Collections")
                .font(.title)
            Text("Collections screen coming soon")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct SettingsPlaceholderView: View {
    var body: some View {
        VStack {
            Image(systemName: "gearshape.fill")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text("Settings")
                .font(.title)
            Text("Settings screen coming soon")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}
