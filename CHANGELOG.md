# Changelog

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
