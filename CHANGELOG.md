# Changelog

## [1.2.2] - 2025-10-07

### Bug Fixes
* parse runner-type as JSON array and add 5-minute timeout (1879d0a)


## [1.2.1] - 2025-10-07

### Bug Fixes
* remove invalid continue-on-error from reusable workflow calls (c86beeb)
## [1.2.0] - 2025-10-07

### New Features
* add app version display to Settings screen (be0d6a4)
* add hybrid runner strategy for deployment workflows (ce6fe45)
## [1.1.0] - 2025-10-07

### New Features
* add Android Play Store deployment with universal command naming (d2cdedd)
* add complete CI/CD pipeline with automated version management (7347a1d)
* Add a menu for "..." on show detail (389d77f)
* complete recording selection with user preferences and v2 UX patterns (e7172ab)
* add database migration system with recordingId tracking (1d0000c)
* add database migration system with recordingId tracking (1d0417b)
* separate temporary recording switch from permanent default (bbebb65)
* wire recording selection to ShowDetail view updates (a24c87e)
* implement recording selection modal feature (c200732)
* optimize iOS Player screen layout with platform-specific dimensions (984dfb2)
* implement event-driven iOS media player architecture (edd9cbe)
* complete PlatformMediaPlayer simplification and double-playing fix (4c4fbbb)
* implement replacePlaylist API and remove player ID management (626e624)
* refactor SmartQueuePlayerManager to single instance pattern (19dd96c)
* implement SmartQueuePlayer wrapper for reliable iOS gapless playback (c38c784)
* migrate iOS player from AVPlayer to AVQueuePlayer for gapless playback (9a93dab)
* implement iOS gapless playback with intelligent prefetching (4aa8138)
* eliminate ShowRepository duplication with Universal Service + Platform Tool pattern (6fb9642)
* implement iOS playback state persistence for app restart continuity (bcffcfc)
* enable iOS background audio playback and remote controls (0b9ac0e)
* enable iOS background audio playback and AirPlay support (0a07371)
* integrate ZIPFoundation for native iOS ZIP extraction via callback bridge (48c6a31)
* add ZIPFoundation dependency for native iOS ZIP extraction (748bcb3)
* add iOS network entitlements for proper app sandbox compliance (b86c799)
* add background prefetching for adjacent shows (30f7bd0)
* add cache cleanup commands for Android and iOS (59d5e05)
* complete library system implementation (Phases 7 & 9) (d447b45)
* complete Phase 6 library UI components with cross-platform support (60b0cba)
* implement complete library system with reactive UI updates (9b1ef5f)
* add library domain models (V2 pattern) (cf39785)
* add LibraryShow table for library system (V2 hybrid pattern) (2eddfc0)
* Docs/Plans (ce879f6)
* add splash screen for app initialization with data sync progress tracking (17cce02)
* add Deadly Grateful Dead logo as app icon for Android and iOS (936d382)
* implement proper MVVM architecture for PlayerScreen with V2 navigation pattern (fac1383)
* implement V2 ShowDetail play button behavior with smart show detection (ef83abe)
* integrate RecentShowsService with HomeScreen reactive architecture (0ab9d59)
* implement RecentShowsService with V2 smart play detection (9774295)
* implement MediaItem enrichment with V2 metadata parity (0173e2a)
* implement V2-style homepage with cross-platform components (c4bcea8)
* implement exact V2 Player UI with comprehensive component system (dfd8f32)
* implement V2 playlist pattern for cross-platform track navigation (92b1021)
* Add first player (8971eaf)
* implement universal icon alignment fix for Android (3b0f012)
* implement platform-specific spacing fixes for Android/iOS alignment (ee647c9)
* achieve true 1:1 V2 parity for ShowDetailScreen with proper navigation (2b8af64)
* implement V2-level ShowDetailScreen with rich UI components (f67882e)
* write cache to file (922cd14)
* Phase 3 - Replace ShowDetailViewModel with real service integration (a77e253)
* implement Phase 1 ShowDetailService with V2's database-first loading patterns (a29496c)
* implement V2-compliant ArchiveService with battle-tested patterns (17c1def)
* implement Phase 3 universal services with Archive.org business logic (e1ad2de)
* implement Phase 2 minimal platform tools (6a144f8)
* implement Phase 1 ShowDetail navigation foundation (9069fb0)
* implement FTS4 full-text search with enhanced date tokenization (833353f)
* implement comprehensive recording import system (33c50d9)
* add make commands for database extraction and require platform argument (1a5801b)
* add unified database extraction script for Android and iOS (1c69b01)
* implement native iOS ZIP extraction using POSIX system calls (84770db)
* implement Universal Service + Platform Tool architecture pattern (751b21a)
* implement complete data integration with SQLDelight and cross-platform database support (f4a34e7)
* implement complete light/dark theme system with bottom navigation (b0b5f8d)
* add run-remote-all command for cross-platform remote testing (2065871)
* add Android remote emulator support with proper zsh environment (877bd1c)
* implement complete SearchResultsScreen with V2 patterns (474cde1)
* implement AppScaffold with TopBar and feature-colocated bar configurations (0173f8f)
* integrate AppScreen sealed class navigation with Android NavHost (a6b140f)
* add AppScreen sealed interface and route conversion extensions (f14e5d3)
* add androidx.navigation.compose dependency for KMM navigation system (734ae87)
* add cross-platform logging system with unified log reader (d080555)
* implement complete Search feature with KMM architecture (1c6d3eb)
* implement Search domain models and SearchServiceStub (99c8d8e)
* add KMM foundation libraries and build configuration (b0908d2)
* add complete KMM project structure and documentation (c253e4d)
* add comprehensive Makefile for KMM build automation (70b4c95)

### Bug Fixes
* decode provisioning profile to temp file for PlistBuddy (9ec71a0)
* use PlistBuddy to extract provisioning profile UUID (c7e159d)
* install iOS provisioning profile with UUID-based filename (0786c21)
* update deploy-testing workflow to use correct GitHub secret names (0f86540)
* browse navigation now respects user recording preferences (c7cc8f2)
* preserve playback state during iOS navigation and reduce log noise (9e71ec7)
* pass metadata through SmartQueuePlayerManager to enable rich notifications (eff6400)
* resolve AVPlayerItem reuse bug and improve SmartQueuePlayer reliability (13e85d8)
* filter shows without recordings from TIGDH and search results (bf41bcb)
* enable iOS player auto-advance to next track on completion (a12c26a)
* ensure Today in GD History displays oldest→newest chronologically (afc8a49)
* remove alpha channel from iOS app icon to meet Apple requirements (f453e84)
* sort tracks by filename instead of unreliable track metadata (7af293e)
* prevent stale track display when browsing shows quickly (92ed7c8)
* implement efficient DB-level navigation for ShowDetail browse (30e06f6)
* resolve Android icon clipping for small icons in library cards (cd2d18c)
* Launch the home screen after loading is complete (1fdabe5)
* iOS ShowDetail pause button showing spinner instead of play icon (d7769fc)
* update PlayerTopBar to match V2 layout and spacing exactly (a10082e)
* update Android adaptive icon configuration to use Deadly logo (ea246d4)
* implement real show IDs for home screen navigation with proper domain layer (5c12fa2)
* resolve root cause of missing show metadata in player UI (a0c9276)
* resolve player information display issues with proper V2 data patterns (8b6d4cc)
* cache directory usage and implement V2 smart format selection (438d06f)
* refactor CacheManager to use Okio FileSystem for cross-platform consistency (e7bc56b)
* clean up search UI and documentation (cf170c8)
* resolve iOS database schema mismatch by correcting SQLDelight file paths (0c62adb)
* implement cross-platform ZIP extraction with real GitHub data integration (90049bb)
* implement proper iOS navigation with immediate state synchronization (393a4c3)
* Make all the icons fit (46c1446)
* resolve Android TopBar visibility with cross-platform navigation state tracking (ceda1be)
* improve bottom navigation spacing to prevent label truncation (181c7dc)
* resolve iOS icon transparency and touch event issues with proper resource loading (1588adb)
* achieve cross-platform icon color consistency for iOS (6880484)
* enable iOS remote builds with cross-platform icon system (74323b2)
* resolve iOS UI issues with AppScaffold padding and navigation (d33e729)
* prevent SearchViewModel recreation on recomposition (fab0c31)
* move navigation-compose dependency to androidMain only (b68d5c9)
* resolve iOS build failures with cross-platform navigation abstraction (e944263)
* replace iOS println logging with NSLog for system visibility (fbf6153)
* resolve Koin DI initialization crash on iOS app startup (880ea2c)
* resolve KMM compatibility issues and add comprehensive documentation (5f8ceb0)
* migrate to compilerOptions DSL for Kotlin 2.2.10 compatibility (7669402)

### Performance Improvements
* make playNext() instant by using rebuildQueue() approach (4154532)

### Code Refactoring
* eliminate dead onNavigateToPlayer callback noise (63594d6)
* eliminate 99% duplicate code in library system architecture (b83fc4c)
* remove problematic icons from SearchResultCard (f5c5410)
* perfect V2 parity with precise UI matching and proper data presentation (a428fc8)
* remove dead code and clean up obsolete references (9264fd0)
* clean up dead services and add local build-all commands (713a655)
* unify DI initialization and adopt consistent Makefile naming (3eaebd6)

### Documentation Updates
* add ShowDetail screen implementation plan (78a6787)
* update documentation to reflect current implementation status (c8cbcf5)
* add comprehensive data import architecture documentation (35d1bac)
* add comprehensive navigation implementation guide (8491341)
* update navigation implementation guide to use sealed class approach (011d3b1)
* Create a strategy for KMM migration (fc5e924)

### Other Changes
* chore: release workflows (f820ffb)
* chore: ignore fastlane generated content and xcode crap (1f8688d)
* micro: align recording selection UI with V2 patterns (0199db6)
* micro: add RecordingSelectionService business logic following V2 pattern (be27bba)
* micro: add SQLDelight Recording → Domain Recording mapper and update ShowService (aa3abc2)
* micro: add recording selection data models with comprehensive tests (e63cfe9)
* chore: remove Package.resolved from version control (0c62f8c)
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - TBD

### Initial Release

This is the first tracked release of the Deadly app.

### Added
- Kotlin Multiplatform Mobile architecture (Android + iOS)
- 5-tab bottom navigation (Home, Search, Library, Collections, Settings)
- Search functionality with QR scanner and browse sections
- Cross-platform navigation system using expect/actual pattern
- AppIcon system supporting Material Symbols (Android) and SF Symbols (iOS)
- Recording playback with ExoPlayer (Android) and AVPlayer (iOS)
- SQLDelight database for local data persistence
- Ktor for network requests
- Koin for dependency injection
- Remote development workflow (Linux → Mac)
- Automated CI/CD pipeline with GitHub Actions
- Version management system with conventional commits
- Fastlane automation for iOS and Android builds
- Code signing for both platforms
- TestFlight upload support for iOS
- Comprehensive build system via Makefile
