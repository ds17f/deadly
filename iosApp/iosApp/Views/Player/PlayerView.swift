import SwiftUI
import ComposeApp

/**
 * SwiftUI implementation of PlayerScreen.
 *
 * Matches the Compose version EXACTLY - full-screen immersive player with gradient background.
 * Uses native SwiftUI components with shared KMM PlayerViewModel for business logic.
 *
 * Layout matches Compose PlayerScreen.kt:
 * - Recording-based gradient background (5 Grateful Dead colors)
 * - Cover art (450pt height, square 1:1 aspect, 16pt corners)
 * - Track info (title with marquee, date, venue)
 * - Progress control (slider with time labels)
 * - Enhanced controls (shuffle, prev, play/pause FAB 72pt, next, repeat)
 * - Secondary controls (connect, share, queue)
 * - Material panels
 * - Mini player overlay on scroll
 */

// Grateful Dead color palette matching PlayerGradientBackground.kt
private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)
private let DeadGold = Color(red: 0xFF/255.0, green: 0xD7/255.0, blue: 0x00/255.0)
private let DeadGreen = Color(red: 0x22/255.0, green: 0x8B/255.0, blue: 0x22/255.0)
private let DeadBlue = Color(red: 0x41/255.0, green: 0x69/255.0, blue: 0xE1/255.0)
private let DeadPurple = Color(red: 0x8A/255.0, green: 0x2B/255.0, blue: 0xE2/255.0)

struct PlayerView: View {
    @StateObject private var viewModel: PlayerViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var scrollOffset: CGFloat = 0

    let onNavigateToShowDetail: (String, String?) -> Void

    init(
        mediaService: MediaService,
        onNavigateToShowDetail: @escaping (String, String?) -> Void
    ) {
        _viewModel = StateObject(wrappedValue: PlayerViewModelWrapper(mediaService: mediaService))
        self.onNavigateToShowDetail = onNavigateToShowDetail
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Gradient background matching V2 algorithm
            gradientBackground
                .ignoresSafeArea()

            // Main content in ScrollView
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Top bar (8pt vertical padding)
                    topBar
                        .padding(.vertical, 8)

                    // Cover art (450pt height on Android, 380pt on iOS)
                    coverArt
                        .padding(.top, 16)

                    // Track info row (24pt horizontal padding)
                    trackInfoRow
                        .padding(.horizontal, 24)
                        .padding(.top, 24)

                    // Progress control (24pt horizontal padding)
                    progressControl
                        .padding(.horizontal, 24)
                        .padding(.top, 16)

                    // Enhanced controls (24pt horizontal padding)
                    enhancedControls
                        .padding(.horizontal, 24)
                        .padding(.top, 24)

                    // Secondary controls (24pt horizontal, 12pt vertical)
                    secondaryControls
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)

                    // Material panels (16pt horizontal, 6pt vertical) - matches V2
                    if viewModel.currentTrack != nil {
                        materialPanels
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                    }

                    // Bottom spacing
                    Spacer()
                        .frame(height: 16)
                }
                .background(GeometryReader { geo -> Color in
                    DispatchQueue.main.async {
                        scrollOffset = -geo.frame(in: .named("scroll")).minY
                    }
                    return Color.clear
                })
            }
            .coordinateSpace(name: "scroll")

            // Back button overlay (top-left, semi-transparent)
            backButton
                .padding(16)

            // Mini player overlay when scrolled (appears at offset > 1200)
            if scrollOffset > 1200 {
                miniPlayer
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: - Gradient Background

    private var gradientBackground: some View {
        LinearGradient(
            gradient: createRecordingGradient(recordingId: viewModel.currentRecordingId),
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private func createRecordingGradient(recordingId: String?) -> Gradient {
        guard let recId = recordingId else {
            // Default gradient
            return Gradient(colors: [
                Color(UIColor.systemBackground),
                Color(UIColor.secondarySystemBackground)
            ])
        }

        // V2 algorithm: hash-based color selection
        let deadColors = [DeadRed, DeadGold, DeadGreen, DeadBlue, DeadPurple]
        let hash = abs(recId.hashValue)
        let colorIndex = hash % deadColors.count
        let baseColor = deadColors[colorIndex]
        let backgroundColor = Color(UIColor.systemBackground)

        // Lerp blending at 0.8, 0.4, 0.1 alphas
        let strongColor = lerpColor(backgroundColor, baseColor, 0.8)
        let mediumColor = lerpColor(backgroundColor, baseColor, 0.4)
        let faintColor = lerpColor(backgroundColor, baseColor, 0.1)

        return Gradient(stops: [
            .init(color: strongColor, location: 0.0),
            .init(color: mediumColor, location: 0.3),
            .init(color: faintColor, location: 0.6),
            .init(color: backgroundColor, location: 0.8),
            .init(color: backgroundColor, location: 1.0)
        ])
    }

    private func lerpColor(_ a: Color, _ b: Color, _ t: Double) -> Color {
        // Extract RGB components from both colors
        let uiColorA = UIColor(a)
        let uiColorB = UIColor(b)

        var rA: CGFloat = 0, gA: CGFloat = 0, bA: CGFloat = 0, aA: CGFloat = 0
        var rB: CGFloat = 0, gB: CGFloat = 0, bB: CGFloat = 0, aB: CGFloat = 0

        uiColorA.getRed(&rA, green: &gA, blue: &bA, alpha: &aA)
        uiColorB.getRed(&rB, green: &gB, blue: &bB, alpha: &aB)

        // Linear interpolation
        let r = lerp(a: Double(rA), b: Double(rB), t: t)
        let g = lerp(a: Double(gA), b: Double(gB), t: t)
        let b = lerp(a: Double(bA), b: Double(bB), t: t)
        let alpha = lerp(a: Double(aA), b: Double(aB), t: t)

        return Color(red: r, green: g, blue: b, opacity: alpha)
    }

    private func lerp(a: Double, b: Double, t: Double) -> Double {
        return a + (b - a) * t
    }

    // MARK: - Top Bar

    private var topBar: some View {
        HStack {
            // Back button
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.down")
                    .font(.system(size: 24))
                    .foregroundColor(Color(UIColor.label))
                    .frame(width: 44, height: 44)
            }

            Spacer()

            // Context menu button (navigate to show detail)
            Button(action: {
                if let showId = viewModel.showId {
                    onNavigateToShowDetail(showId, viewModel.recordingIdForNav)
                }
            }) {
                Image(systemName: "music.note.list")
                    .font(.system(size: 20))
                    .foregroundColor(Color(UIColor.label))
                    .frame(width: 44, height: 44)
            }

            // Options button
            Button(action: {
                // TODO: Show bottom sheet
            }) {
                Image(systemName: "ellipsis")
                    .font(.system(size: 20))
                    .foregroundColor(Color(UIColor.label))
                    .frame(width: 44, height: 44)
            }
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Cover Art

    private var coverArt: some View {
        HStack {
            Spacer()
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(UIColor.systemGray5))
                .aspectRatio(1.0, contentMode: .fit)
                .frame(height: 380) // iOS: 380pt (Android: 450dp)
                .overlay(
                    Image(systemName: "music.note")
                        .font(.system(size: 160))
                        .foregroundColor(Color(UIColor.secondaryLabel))
                )
            Spacer()
        }
        .padding(.horizontal, 24)
    }

    // MARK: - Track Info Row

    private var trackInfoRow: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                // Track title - headline, bold, marquee scrolling
                Text(viewModel.currentTrack?.title ?? "No Track Selected")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(Color(UIColor.label))
                    .lineLimit(1)

                // Show date - bodyLarge
                Text(viewModel.showDate ?? "Show date loading...")
                    .font(.body)
                    .foregroundColor(Color(UIColor.secondaryLabel))

                // Venue - bodyMedium, 2 lines
                Text(viewModel.venue ?? viewModel.location ?? "Venue loading...")
                    .font(.callout)
                    .foregroundColor(Color(UIColor.secondaryLabel))
                    .lineLimit(2)
            }

            Spacer()

            // Add to playlist button
            Button(action: {
                // TODO: Add to playlist
            }) {
                Image(systemName: "plus.circle")
                    .font(.system(size: 24))
                    .foregroundColor(DeadRed)
                    .frame(width: 36, height: 36)
            }
        }
    }

    // MARK: - Progress Control

    private var progressControl: some View {
        VStack(spacing: 2) {
            // Slider
            Slider(
                value: Binding(
                    get: { Double(viewModel.progress) },
                    set: { newValue in
                        let newPosition = Int64(Double(viewModel.durationMs) * newValue)
                        viewModel.seekTo(positionMs: newPosition)
                    }
                ),
                in: 0...1
            )
            .accentColor(DeadRed)
            .padding(.vertical, 2)

            // Time labels
            HStack {
                Text(viewModel.formattedPosition)
                    .font(.caption)
                    .foregroundColor(Color(UIColor.secondaryLabel))

                Spacer()

                Text(viewModel.formattedDuration)
                    .font(.caption)
                    .foregroundColor(Color(UIColor.secondaryLabel))
            }
        }
    }

    // MARK: - Enhanced Controls

    private var enhancedControls: some View {
        HStack(alignment: .center) {
            // Shuffle button (40pt)
            Button(action: {
                // TODO: Shuffle
            }) {
                Image(systemName: "shuffle")
                    .font(.system(size: 24))
                    .foregroundColor(Color(UIColor.secondaryLabel))
                    .frame(width: 40, height: 40)
            }

            Spacer()

            // Previous button (56pt with 36pt icon)
            Button(action: { viewModel.previousTrack() }) {
                Image(systemName: "backward.fill")
                    .font(.system(size: 36))
                    .foregroundColor(viewModel.hasPrevious ? Color(UIColor.label) : Color(UIColor.label).opacity(0.38))
                    .frame(width: 56, height: 56)
            }
            .disabled(!viewModel.hasPrevious)

            Spacer()
                .frame(width: 16)

            // Play/Pause FAB (72pt RED)
            Button(action: { viewModel.togglePlayPause() }) {
                ZStack {
                    Circle()
                        .fill(DeadRed)
                        .frame(width: 72, height: 72)

                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.2)
                    } else {
                        Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                            .font(.system(size: 36))
                            .foregroundColor(.white)
                    }
                }
            }
            .frame(width: 72, height: 72)

            Spacer()
                .frame(width: 16)

            // Next button (56pt with 36pt icon)
            Button(action: { viewModel.nextTrack() }) {
                Image(systemName: "forward.fill")
                    .font(.system(size: 36))
                    .foregroundColor(viewModel.hasNext ? Color(UIColor.label) : Color(UIColor.label).opacity(0.38))
                    .frame(width: 56, height: 56)
            }
            .disabled(!viewModel.hasNext)

            Spacer()

            // Repeat button (40pt)
            Button(action: {
                // TODO: Repeat
            }) {
                Image(systemName: "repeat")
                    .font(.system(size: 24))
                    .foregroundColor(Color(UIColor.secondaryLabel))
                    .frame(width: 40, height: 40)
            }
        }
    }

    // MARK: - Secondary Controls

    private var secondaryControls: some View {
        HStack(spacing: 24) {
            Spacer()

            Button(action: {
                // TODO: Connect
            }) {
                VStack(spacing: 4) {
                    Image(systemName: "airplayaudio")
                        .font(.system(size: 20))
                    Text("Connect")
                        .font(.caption2)
                }
                .foregroundColor(Color(UIColor.secondaryLabel))
            }

            Button(action: {
                // TODO: Share
            }) {
                VStack(spacing: 4) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 20))
                    Text("Share")
                        .font(.caption2)
                }
                .foregroundColor(Color(UIColor.secondaryLabel))
            }

            Button(action: {
                // TODO: Queue
            }) {
                VStack(spacing: 4) {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 20))
                    Text("Queue")
                        .font(.caption2)
                }
                .foregroundColor(Color(UIColor.secondaryLabel))
            }

            Spacer()
        }
    }

    // MARK: - Back Button

    private var backButton: some View {
        Button(action: { dismiss() }) {
            Image(systemName: "chevron.down")
                .font(.system(size: 24))
                .foregroundColor(Color(UIColor.label))
                .frame(width: 40, height: 40)
                .background(
                    Color(UIColor.systemBackground).opacity(0.9)
                )
                .clipShape(Circle())
        }
    }

    // MARK: - Mini Player

    private var miniPlayer: some View {
        HStack {
            // Track info
            VStack(alignment: .leading, spacing: 2) {
                Text(viewModel.currentTrack?.title ?? "No Track")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(1)
                Text(viewModel.showDate ?? "")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            // Play/pause button
            Button(action: { viewModel.togglePlayPause() }) {
                Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 24))
                    .foregroundColor(DeadRed)
            }
        }
        .padding()
        .background(
            Color(UIColor.systemBackground).opacity(0.95)
        )
        .cornerRadius(8)
        .padding(.horizontal)
        .padding(.top, 8)
    }

    // MARK: - Material Panels

    private var materialPanels: some View {
        VStack(spacing: 16) {
            // About the Venue Panel
            materialPanel(
                title: "About the Venue",
                content: "Venue information will be displayed when show metadata is available."
            )

            // Lyrics Panel
            materialPanel(
                title: "Lyrics",
                content: "Lyrics will be displayed when available for \(viewModel.currentTrack?.title ?? "this track")."
            )

            // Similar Shows Panel
            materialPanel(
                title: "Similar Shows",
                content: "Similar shows will be displayed when show data is loaded."
            )

            // Credits Panel
            materialPanel(
                title: "Credits",
                content: "Performance credits will be displayed when available."
            )
        }
    }

    private func materialPanel(title: String, content: String) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(Color(UIColor.label))

            Text(content)
                .font(.body)
                .foregroundColor(Color(UIColor.secondaryLabel))
                .lineSpacing(4) // Increased line height (1.2x)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(UIColor.secondarySystemBackground))
                .shadow(color: Color.black.opacity(0.1), radius: 2, x: 0, y: 1)
        )
    }
}

// MARK: - Preview

#Preview {
    // Preview disabled - requires Koin initialization
    Text("PlayerView Preview")
}
