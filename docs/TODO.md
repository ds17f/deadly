# TODO List

This document tracks known issues, missing features, and improvements needed in the KMM migration.

## 🎯 Next Priority Features

### Feature Screens Implementation
- [ ] **LibraryScreen**: Complete library interface following V2 patterns
- [ ] **CollectionsScreen**: Collections management interface
- [ ] **HomeScreen**: Dashboard/feed interface
- [ ] **SettingsScreen**: App configuration interface
- [ ] **PlayerScreen**: Media player interface

### Search Feature Enhancements
- [ ] Implement navigation callbacks for decade browsing (SearchScreen → browse by decade)
- [ ] Implement navigation callbacks for discover items (SearchScreen → discover content)
- [ ] Implement navigation callbacks for browse all items (SearchScreen → browse categories)
- [ ] Add real search functionality to SearchResultsScreen (currently shows placeholder results)

### Resources & Assets
- [ ] Add background alt logo image for DecadeCard components (referenced in V2 as `com.deadly.v2.core.design.R.drawable.alt_logo`)
- [ ] Add Material Symbols font to `androidMain/res/font/material_symbols_outlined.ttf` (from https://github.com/google/material-design-icons/tree/master/variablefont)
- [ ] Set up resource handling for cross-platform image assets

### Service Implementation
- [ ] Replace hardcoded suggestions in SearchService with real database queries
- [ ] Parse setlist/lineup JSON fields in SearchRepository (currently returns null)
- [ ] Implement KMM-compatible date formatting (currently shows "Recent")
- [ ] Add cache management UI for settings (show file size, date, manual delete)

### UI Polish (Low Priority - After Core Features)
- [ ] **Polish Recent/Suggested Search UI**: Improve styling and UX in SearchResultsScreen - enhance visual design, spacing, and interaction patterns once core functionality is stable

### Dependencies & Updates (Low Priority)
- [ ] **Update kotlinx-coroutines**: Upgrade from current 1.8.1 to latest 1.10.2 for latest features and bug fixes

## ✅ Recently Completed

### Search Feature (Completed)
- ✅ **SearchScreen**: Complete search interface with QR scanner, browse sections, discovery
- ✅ **SearchResultsScreen**: Full search results UI with V2 patterns, integrated TopBar, pin indicators
- ✅ **Search Navigation**: Working search box → search results navigation flow
- ✅ **Cross-platform Icons**: AppIcon system with Material Symbols (Android) + SF Symbols (iOS)

### Navigation & Architecture (Completed)
- ✅ **Cross-platform Navigation**: Expect/actual `DeadlyNavHost` abstraction
- ✅ **AppScaffold**: TopBar/BottomBar coordination with feature-colocated configurations
- ✅ **5-Tab Bottom Navigation**: Home, Search, Library, Collections, Settings
- ✅ **ViewModel Lifecycle**: Fixed SearchViewModel recreation issues

### Development Workflow (Completed)
- ✅ **Remote iOS Builds**: `make run-ios-remotesim` (Linux→Mac)
- ✅ **Remote Android Builds**: `make run-android-remote-emu` (Linux→Mac)
- ✅ **Cross-platform Remote Testing**: `make run-remote-all` (both platforms)
- ✅ **Local Cross-platform Testing**: `make build-all`, `make run-all` (macOS)

### Data Architecture (Completed)
- ✅ **Universal Service + Platform Tool Pattern**: Clean architecture with maximum code sharing
- ✅ **SQLDelight Integration**: Cross-platform database with show data storage
- ✅ **Data Import System**: GitHub releases → ZIP extraction → JSON parsing → Database
- ✅ **Real Show Data**: 2313 Grateful Dead shows imported successfully on both platforms
- ✅ **Native ZIP Extraction**: Android (java.util.zip) + iOS (POSIX unzip) implementations

## 🚀 Next Major Features

### Recordings Import & Player
- [ ] **Recording Data Import**: Extend data import to handle recording files (~10k files)
- [ ] **Recording Schema**: Design RecordingJsonSchema and database entities
- [ ] **Player Architecture**: Audio streaming and playback using Universal Service + Platform Tool pattern
- [ ] **PlayerScreen**: Media player interface with show/recording selection