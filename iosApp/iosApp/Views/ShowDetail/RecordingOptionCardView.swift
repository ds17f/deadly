import SwiftUI
import ComposeApp

/**
 * RecordingOptionCardView - Individual recording card for selection
 *
 * Matches Compose RecordingOptionCard.kt exactly:
 * - 4-line layout: source type + rating, taper, technical details, identifier
 * - Conditional background colors (selected/recommended/normal)
 * - 2dp primary border when selected
 * - Check icon on right when selected
 * - Tappable to trigger selection
 */

private let DeadRed = Color(red: 0xDC/255.0, green: 0x14/255.0, blue: 0x3C/255.0)

struct RecordingOptionCardView: View {
    let recording: RecordingOptionViewModel
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                // Main content column
                VStack(alignment: .leading, spacing: 4) {
                    // Line 1: Source type (bold) + Compact star rating
                    HStack(spacing: 8) {
                        Text(recording.sourceType)
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(UIColor.label))

                        // Compact star rating (12pt stars)
                        if let kotlinRating = recording.rating {
                            let rating = kotlinRating.floatValue
                            if rating > 0 {
                                CompactStarRating(rating: rating, starSize: 12)
                            }
                        }
                    }

                    // Line 2: Taper info (only when valid)
                    if let taperInfo = recording.taperInfo {
                        let taperName = taperInfo.hasPrefix("Taper: ")
                            ? String(taperInfo.dropFirst(7)).trimmingCharacters(in: .whitespaces)
                            : taperInfo.trimmingCharacters(in: .whitespaces)

                        let hasValidTaper = !taperName.isEmpty &&
                            taperName.lowercased() != "unknown" &&
                            taperName.lowercased() != "n/a"

                        if hasValidTaper {
                            Text(taperInfo)
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }

                    // Line 3: Technical details
                    if let technicalDetails = recording.technicalDetails {
                        // Clean HTML tags and normalize whitespace
                        let cleanDetails = technicalDetails
                            .replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
                            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
                            .trimmingCharacters(in: .whitespaces)

                        if !cleanDetails.isEmpty {
                            Text(cleanDetails)
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }

                    // Line 4: Archive ID (red-tinted, muted)
                    Text(recording.identifier)
                        .font(.system(size: 12))
                        .foregroundColor(.red.opacity(0.7))
                        .lineLimit(1)

                    // Match reason badge (if available)
                    if let matchReason = recording.matchReason {
                        Text(matchReason)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(recording.isRecommended ? Color.purple : DeadRed)
                    }
                }

                Spacer()

                // Selected check icon (24pt size)
                if recording.isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(DeadRed)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(cardBackgroundColor)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(recording.isSelected ? DeadRed : Color.clear, lineWidth: 2)
            )
            .shadow(radius: 2)
        }
        .buttonStyle(.plain)
    }

    // Background color based on state (matches Compose exactly)
    private var cardBackgroundColor: Color {
        if recording.isSelected {
            // primaryContainer
            return DeadRed.opacity(0.12)
        } else if recording.isRecommended {
            // tertiaryContainer
            return Color.purple.opacity(0.12)
        } else {
            // surface
            return Color(UIColor.secondarySystemBackground)
        }
    }
}

// MARK: - Compact Star Rating Component

struct CompactStarRating: View {
    let rating: Float
    let starSize: CGFloat

    var body: some View {
        HStack(spacing: 2) {
            ForEach(0..<5, id: \.self) { index in
                let starRating = rating - Float(index)
                let iconName = starIconName(for: starRating)

                Image(systemName: iconName)
                    .font(.system(size: starSize))
                    .foregroundColor(starRating > 0 ? DeadRed : Color(UIColor.label).opacity(0.3))
            }
        }
    }

    private func starIconName(for rating: Float) -> String {
        if rating >= 1.0 {
            return "star.fill"
        } else if rating >= 0.5 {
            return "star.leadinghalf.filled"
        } else {
            return "star"
        }
    }
}

// MARK: - Preview

#Preview {
    VStack(spacing: 8) {
        RecordingOptionCardView(
            recording: RecordingOptionViewModel(
                identifier: "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                sourceType: "SBD",
                taperInfo: "Taper: Betty Cantor",
                technicalDetails: "Soundboard, DAT → CD → FLAC",
                rating: 4.8,
                reviewCount: 23,
                rawSource: "Betty Soundboard",
                rawLineage: "SBD > DAT > CD > FLAC",
                isSelected: true,
                isCurrent: true,
                isRecommended: true,
                matchReason: "Recommended"
            ),
            onClick: {}
        )

        RecordingOptionCardView(
            recording: RecordingOptionViewModel(
                identifier: "gd1977-05-08.aud.miller.23456.flac16",
                sourceType: "AUD",
                taperInfo: "Taper: Mike Miller",
                technicalDetails: "Audience, Nak 700 → Cassette → DAT",
                rating: 4.2,
                reviewCount: 12,
                rawSource: "Audience",
                rawLineage: "AUD > Cassette > DAT > FLAC",
                isSelected: false,
                isCurrent: false,
                isRecommended: false,
                matchReason: "Popular Choice"
            ),
            onClick: {}
        )
    }
    .padding()
}
