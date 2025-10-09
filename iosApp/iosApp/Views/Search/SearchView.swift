//
//  SearchView.swift
//  iosApp
//
//  Native SwiftUI search/browse screen matching SearchScreen.kt
//

import SwiftUI
import ComposeApp

struct SearchView: View {
    @StateObject private var viewModel: SearchViewModelWrapper
    @EnvironmentObject private var coordinator: NavigationCoordinator

    init(searchViewModel: SearchViewModel) {
        _viewModel = StateObject(wrappedValue: SearchViewModelWrapper(searchViewModel: searchViewModel))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Search box
                SearchBox()
                    .padding(.vertical, 8)

                // Browse by decades
                BrowseSection()

                // Discover section
                DiscoverSection()

                // Browse All section
                BrowseAllSection()
            }
            .padding(16)
        }
        .navigationTitle("Search")
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Search Box

    @ViewBuilder
    private func SearchBox() -> some View {
        Button(action: {
            coordinator.path.append(NavigationCoordinator.Destination.searchResults(query: ""))
        }) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 28))
                    .foregroundColor(.gray)

                Text("What do you want to listen to?")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.black)

                Spacer()
            }
            .padding()
            .background(Color.white)
            .cornerRadius(8)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
            )
        }
    }

    // MARK: - Browse Section

    @ViewBuilder
    private func BrowseSection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Start Browsing")
                .font(.system(size: 24, weight: .bold))

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                DecadeCard(title: "1960s", colors: [Color(hex: "1976D2"), Color(hex: "42A5F5")])
                DecadeCard(title: "1970s", colors: [Color(hex: "388E3C"), Color(hex: "66BB6A")])
                DecadeCard(title: "1980s", colors: [Color(hex: "D32F2F"), Color(hex: "EF5350")])
                DecadeCard(title: "1990s", colors: [Color(hex: "7B1FA2"), Color(hex: "AB47BC")])
            }
            .frame(height: 180)
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func DecadeCard(title: String, colors: [Color]) -> some View {
        Button(action: {
            // TODO: Handle decade browse
        }) {
            ZStack(alignment: .bottomLeading) {
                LinearGradient(
                    colors: colors,
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .cornerRadius(8)

                Text(title)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .padding(12)
            }
            .frame(height: 80)
        }
    }

    // MARK: - Discover Section

    @ViewBuilder
    private func DiscoverSection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Discover")
                .font(.system(size: 24, weight: .bold))

            VStack(spacing: 8) {
                DiscoverItem(title: "Random Show", subtitle: "Feeling adventurous?")
                DiscoverItem(title: "This Day in History", subtitle: "Shows from this date")
                DiscoverItem(title: "Popular This Week", subtitle: "Trending among listeners")
            }
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func DiscoverItem(title: String, subtitle: String) -> some View {
        Button(action: {
            // TODO: Handle discover action
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.primary)

                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }

    // MARK: - Browse All Section

    @ViewBuilder
    private func BrowseAllSection() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Browse All")
                .font(.system(size: 24, weight: .bold))

            VStack(spacing: 8) {
                BrowseAllItem(title: "All Shows", subtitle: "Browse complete collection")
                BrowseAllItem(title: "By Venue", subtitle: "Find shows by location")
                BrowseAllItem(title: "By Year", subtitle: "Explore year by year")
            }
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder
    private func BrowseAllItem(title: String, subtitle: String) -> some View {
        Button(action: {
            // TODO: Handle browse all action
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(subtitle)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
    }
}

// MARK: - Color Extension

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)

        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}
