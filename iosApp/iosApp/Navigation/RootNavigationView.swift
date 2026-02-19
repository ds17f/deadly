import SwiftUI
import ComposeApp

/**
 * RootNavigationView - Main navigation container for iOS app
 *
 * Architecture:
 * - NavigationStack for push/pop navigation
 * - TabView for bottom navigation (5 tabs)
 * - MiniPlayer overlay above TabBar (Spotify-style)
 * - Full-screen modal presentations for Player and ShowDetail
 *
 * Matches Android AppScaffold layout exactly
 */
struct RootNavigationView: View {
    @StateObject private var coordinator = NavigationCoordinator()
    @Binding var pendingDeepLink: DeepLink?

    // Get services from Koin
    private let mediaService: MediaService
    private let showDetailService: ShowDetailService
    private let libraryService: LibraryService

    init(pendingDeepLink: Binding<DeepLink?>) {
        self._pendingDeepLink = pendingDeepLink
        self.mediaService = KoinHelper.shared.getMediaService()
        self.showDetailService = KoinHelper.shared.getShowDetailService()
        self.libraryService = KoinHelper.shared.getLibraryService()
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // TabView - full screen
            DeadlyTabView(coordinator: coordinator)
                .environmentObject(coordinator)

            // MiniPlayer positioned above TabBar
            // Using offset to position it just above the TabBar
            GeometryReader { geometry in
                VStack {
                    Spacer()
                    MiniPlayerView(mediaService: mediaService) {
                        coordinator.navigateToPlayer()
                    }
                    .padding(.bottom, 49) // TabBar height (standard iOS TabBar is ~49pt)
                }
            }
        }
        .ignoresSafeArea(.keyboard)
        .onChange(of: pendingDeepLink) { _, newValue in
            print("🔗 [DEEPLINK] RootNav onChange: \(String(describing: newValue))")
            if let deepLink = newValue {
                coordinator.handleDeepLink(deepLink)
                pendingDeepLink = nil
            }
        }
        .onAppear {
            print("🔗 [DEEPLINK] RootNav onAppear, pending: \(String(describing: pendingDeepLink))")
            if let deepLink = pendingDeepLink {
                coordinator.handleDeepLink(deepLink)
                pendingDeepLink = nil
            }
        }
        // Full-screen Player modal
        .fullScreenCover(isPresented: $coordinator.isPresentingPlayer) {
            PlayerView(mediaService: mediaService) { showId, recordingId in
                // Navigate to ShowDetail from Player
                coordinator.isPresentingPlayer = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    coordinator.navigateToShowDetail(showId: showId, recordingId: recordingId)
                }
            }
        }
        // Full-screen ShowDetail modal
        .fullScreenCover(isPresented: $coordinator.isPresentingShowDetail) {
            ShowDetailView(
                showId: coordinator.showDetailParams.showId,
                recordingId: coordinator.showDetailParams.recordingId,
                showDetailService: showDetailService,
                mediaService: mediaService,
                libraryService: libraryService
            )
        }
    }

    @ViewBuilder
    private func destinationView(for destination: NavigationCoordinator.Destination) -> some View {
        switch destination {
        case .showDetail(let showId, let recordingId):
            ShowDetailView(
                showId: showId,
                recordingId: recordingId,
                showDetailService: showDetailService,
                mediaService: mediaService,
                libraryService: libraryService
            )

        case .searchResults(let query):
            Text("Search results for: \(query)")
                .navigationTitle("Search Results")
        }
    }
}

#Preview {
    RootNavigationView(pendingDeepLink: .constant(nil))
}
