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
- [ ] Replace `SearchServiceStub` with real `SearchServiceImpl` when ready
- [ ] Implement actual search backend integration
- [ ] Add proper error handling and loading states

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

## Phase 3 & 4 (Future)
- [ ] SQLDelight data layer integration
- [ ] Data import service from Archive.org
- [ ] Real search backend implementation