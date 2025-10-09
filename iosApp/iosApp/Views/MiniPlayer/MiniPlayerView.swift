import SwiftUI
import ComposeApp

/**
 * MiniPlayerView - Global mini player matching Android V2 design EXACTLY
 *
 * V2 Exact Features:
 * - 68pt height Card with top rounded corners only (12pt radius)
 * - Dark red-brown color (#2D1B1B) for V2 theme consistency
 * - Track title and show date/venue subtitle
 * - Play/pause button with loading states (40pt)
 * - Progress bar at bottom (2pt height) with DeadRed color
 * - 10pt padding for compact V2 layout
 * - White text on dark background
 * - Only shows when there's a current track
 */

// V2 color palette
private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)
private let MiniPlayerBackground = Color(red: 0x2D/255.0, green: 0x1B/255.0, blue: 0x1B/255.0) // Dark red-brown

struct MiniPlayerView: View {
    @StateObject private var viewModel: MiniPlayerViewModel
    let onPlayerClick: () -> Void

    init(mediaService: MediaService, onPlayerClick: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: MiniPlayerViewModel(mediaService: mediaService))
        self.onPlayerClick = onPlayerClick
    }

    var body: some View {
        // Only show when there's a current track
        if viewModel.currentTrack != nil {
            miniPlayerContent
        }
    }

    private var miniPlayerContent: some View {
        VStack(spacing: 0) {
            // Main content row with 10pt padding (V2 exact)
            HStack(spacing: 0) {
                // Track information (no cover art in V2)
                VStack(alignment: .leading, spacing: 2) {
                    // Track title
                    Text(viewModel.currentTrack?.title ?? "Unknown Track")
                        .font(.callout)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .lineLimit(1)

                    // Show subtitle (date - venue)
                    Text(viewModel.displaySubtitle)
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.7))
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Play/pause button (40pt)
                Button(action: {
                    viewModel.togglePlayPause()
                }) {
                    ZStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 18))
                                .foregroundColor(.white)
                        }
                    }
                    .frame(width: 40, height: 40)
                }
            }
            .padding(10)

            // Progress bar at bottom (2pt height, V2 exact)
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    // Track background
                    Rectangle()
                        .fill(DeadRed.opacity(0.3))
                        .frame(height: 2)

                    // Progress foreground
                    Rectangle()
                        .fill(DeadRed)
                        .frame(width: geometry.size.width * CGFloat(viewModel.progress), height: 2)
                }
            }
            .frame(height: 2)
        }
        .frame(height: 68)
        .background(MiniPlayerBackground)
        .cornerRadius(12, corners: [.topLeft, .topRight])
        .shadow(color: Color.black.opacity(0.3), radius: 8, x: 0, y: -2)
        .onTapGesture {
            onPlayerClick()
        }
    }
}

/**
 * ViewModel for MiniPlayer
 * Observes MediaService playback state
 */
class MiniPlayerViewModel: ObservableObject {
    private let mediaService: MediaService

    @Published var currentTrack: Track?
    @Published var showDate: String?
    @Published var venue: String?
    @Published var location: String?
    @Published var isPlaying: Bool = false
    @Published var isLoading: Bool = false
    @Published var currentPositionMs: Int64 = 0
    @Published var durationMs: Int64 = 0

    var displaySubtitle: String {
        if let showDate = showDate, let venue = venue {
            return "\(showDate) - \(venue)"
        } else if let showDate = showDate {
            return showDate
        } else if let venue = venue {
            return venue
        } else {
            return "Grateful Dead"
        }
    }

    var progress: Float {
        guard durationMs > 0 else { return 0 }
        return Float(currentPositionMs) / Float(durationMs)
    }

    init(mediaService: MediaService) {
        self.mediaService = mediaService

        // Observe playback state using FlowCollector
        let collector = PlaybackStateCollector { [weak self] state in
            guard let self = self else { return }
            guard let playbackState = state as? MediaPlaybackState else { return }

            DispatchQueue.main.async {
                self.currentTrack = playbackState.currentTrack
                self.showDate = playbackState.showDate
                self.venue = playbackState.venue
                self.location = playbackState.location
                self.isPlaying = playbackState.isPlaying
                self.isLoading = playbackState.isLoading
                self.currentPositionMs = playbackState.currentPositionMs
                self.durationMs = playbackState.durationMs
            }
        }

        mediaService.playbackState.collect(collector: collector) { error in
            if let err = error {
                print("❌ [MiniPlayer] Error collecting playback state: \(String(describing: err))")
            }
        }
    }

    func togglePlayPause() {
        Task {
            if isPlaying {
                try? await mediaService.pause()
            } else {
                try? await mediaService.resume()
            }
        }
    }
}

// Extension for rounded specific corners
extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

// FlowCollector for playback state
private class PlaybackStateCollector: Kotlinx_coroutines_coreFlowCollector {
    private let block: (Any?) -> Void

    init(block: @escaping (Any?) -> Void) {
        self.block = block
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        block(value)
        completionHandler(nil)
    }
}
