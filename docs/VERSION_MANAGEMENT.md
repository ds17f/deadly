# Automated Version Management

This document describes the automated version management system for the Deadly KMM app, which keeps Android and iOS versions synchronized.

## Overview

Version management in this project is fully automated using:
- **Single Source of Truth**: `version.properties` at project root
- **Conventional Commits**: Version bumps determined automatically from commit messages
- **Cross-Platform**: Both Android and iOS use the same version numbers
- **Automated Releases**: GitHub Actions builds and publishes releases when tags are pushed

## Version Properties File

The `version.properties` file at the project root contains:

```properties
# Version configuration for both Android and iOS
# This is the single source of truth for app versioning
VERSION_NAME=1.0.0
VERSION_CODE=16
```

- `VERSION_NAME`: User-facing version (e.g., "1.0.0")
- `VERSION_CODE`: Build number (increments with each release)

Both Android and iOS read from this file during the build process.

## How It Works

### Android
The `composeApp/build.gradle.kts` reads from `version.properties`:
```kotlin
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}

defaultConfig {
    versionCode = versionProps.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
    versionName = versionProps.getProperty("VERSION_NAME") ?: "1.0.0"
}
```

### iOS
The fastlane `build_release` and `deploy_testflight` lanes call `update_version_from_properties` which reads from `version.properties` and updates the Xcode project:
```ruby
version_props = File.read("../../version.properties")
version_name = version_props.match(/VERSION_NAME=(.+)/)[1].strip
version_code = version_props.match(/VERSION_CODE=(.+)/)[1].strip

increment_version_number(version_number: version_name)
increment_build_number(build_number: version_code)
```

## Creating Releases

### Automatic Version Bumping (Recommended)

The release script analyzes your commits using conventional commit format and automatically determines the version bump:

```bash
# Dry run to see what would happen
make release-dry-run

# Create release with automatic versioning
make release
```

**Version Bump Rules:**
- **Major** (1.0.0 → 2.0.0): Commits with breaking changes (`feat!:`, `fix!:`, etc.)
- **Minor** (1.0.0 → 1.1.0): New features (`feat:`)
- **Patch** (1.0.0 → 1.0.1): Bug fixes (`fix:`) or any other commits

### Manual Version Specification

To specify a version manually:

```bash
# Create release with specific version
make release-version VERSION=1.2.3
```

## Conventional Commits

Use these commit message prefixes to enable automatic version bumping:

- `feat:` - New feature (minor version bump)
- `fix:` - Bug fix (patch version bump)
- `feat!:` or `fix!:` - Breaking change (major version bump)
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Test changes
- `build:` - Build system changes
- `ci:` - CI/CD changes
- `perf:` - Performance improvements

**Examples:**
```bash
git commit -m "feat: add search history feature"
git commit -m "fix: resolve crash on startup"
git commit -m "feat!: redesign navigation system"
```

## Release Workflow

When you run `make release`:

1. **Analyze Commits**: Script checks commits since last tag using conventional commit format
2. **Determine Version**: Automatically calculates new version (major/minor/patch)
3. **Generate Changelog**: Creates CHANGELOG.md from commit messages
4. **Update Files**: Updates `version.properties` with new version
5. **Git Operations**: Commits changes, creates annotated tag, pushes to origin
6. **GitHub Actions**: Workflow triggers automatically on tag push

### GitHub Actions Build Process

When a tag matching `v*` is pushed:

1. **Build Android**: Builds signed APK and AAB
2. **Build iOS**: Builds signed IPA
3. **Create Release**: Creates GitHub release with:
   - Changelog from commit messages
   - Android APK and AAB files
   - iOS IPA file
   - Installation instructions

## Files Modified During Release

The `scripts/release.sh` script modifies:
- `version.properties` - Updates VERSION_NAME and VERSION_CODE
- `CHANGELOG.md` - Adds new version section with commits

During the build:
- Android: `composeApp/build.gradle.kts` reads from `version.properties`
- iOS: Fastlane updates the Xcode project from `version.properties`

## Common Tasks

### Check Current Version
```bash
cat version.properties
```

### Preview Release Without Making Changes
```bash
make release-dry-run
```

### Create Release Automatically
```bash
make release
```

### Create Release With Specific Version
```bash
make release-version VERSION=2.0.0
```

### View Recent Releases
```bash
git tag -l
git show v1.0.0  # Show tag details
```

## Troubleshooting

### "No changes detected since last release"
You need at least one commit since the last tag. Make a commit with a conventional commit message.

### "Tag already exists"
The version already has a tag. Either delete the tag or specify a different version:
```bash
git tag -d v1.0.0  # Delete local tag
git push origin :refs/tags/v1.0.0  # Delete remote tag
```

### GitHub Actions Not Triggering
Ensure:
1. Tag was pushed to origin: `git push origin v1.0.0`
2. Tag matches pattern `v*` (e.g., `v1.0.0`, not `1.0.0`)
3. GitHub Actions are enabled in repository settings

### Version Mismatch Between Platforms
The automated system prevents this by using `version.properties` as the single source of truth. Both platforms read from the same file during build.

## Manual Synchronization (Emergency Only)

If you need to manually update versions:

1. **Update version.properties**:
   ```bash
   VERSION_NAME=1.2.3
   VERSION_CODE=42
   ```

2. **Verify Android picks it up**:
   ```bash
   ./gradlew :composeApp:assembleDebug
   # Check build/outputs/apk/debug/output-metadata.json
   ```

3. **Verify iOS picks it up**:
   ```bash
   cd iosApp
   fastlane build_release
   # Check iosApp.xcodeproj for updated version
   ```

## Best Practices

1. **Use Conventional Commits**: Enables automatic version bumping and changelog generation
2. **Dry Run First**: Always run `make release-dry-run` before actual release
3. **Review Changelog**: Check that generated CHANGELOG.md is accurate
4. **Test Builds**: Run local builds before pushing tags
5. **Keep Versions Synced**: Never manually edit version.properties without also updating git tags

## References

- [Conventional Commits Specification](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [Fastlane Documentation](https://docs.fastlane.tools/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
