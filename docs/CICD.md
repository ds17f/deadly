# CI/CD Setup Guide

This document describes the CI/CD infrastructure for building and releasing the Deadly KMM application.

## Table of Contents

- [Overview](#overview)
- [Local Development](#local-development)
  - [Android Signed Builds](#android-signed-builds)
  - [iOS Device Builds](#ios-device-builds)
- [Fastlane](#fastlane)
  - [Android Lanes](#android-lanes)
  - [iOS Lanes](#ios-lanes)
- [GitHub Actions](#github-actions)
  - [CI Workflows](#ci-workflows)
  - [Release Workflows](#release-workflows)
  - [Required Secrets](#required-secrets)
- [Release Process](#release-process)
- [Troubleshooting](#troubleshooting)

---

## Overview

The CI/CD pipeline is structured in three tiers:

1. **Local Build Commands** - Makefile targets for day-to-day development
2. **Fastlane Automation** - Cross-platform build automation tool
3. **GitHub Actions** - Cloud-based CI/CD for automated testing and releases

This layered approach allows for:
- Quick local iteration with signed builds
- Consistent build processes via fastlane
- Automated CI/CD without manual intervention

---

## Local Development

### Android Signed Builds

#### Prerequisites

1. Configure `.secrets/keystore.properties`:
   ```properties
   storeFile=.secrets/my-release-key.jks
   storePassword=YOUR_KEYSTORE_PASSWORD
   keyAlias=YOUR_KEY_ALIAS
   keyPassword=YOUR_KEY_PASSWORD
   ```

2. Ensure your keystore file exists at `.secrets/my-release-key.jks`

#### Available Commands

```bash
# Build signed release APK
make build-release-signed-android

# Build signed App Bundle (for Play Store)
make build-bundle-android

# Build, sign, install, and run on connected device
make run-android-device-signed
```

#### Build Outputs

- APK: `composeApp/build/outputs/apk/release/`
- AAB: `composeApp/build/outputs/bundle/release/`

### iOS Device Builds

#### Prerequisites

1. Import your P12 certificate to Keychain:
   ```bash
   security import .secrets/DeadlyApp_AppStore2.p12 -k ~/Library/Keychains/login.keychain-db
   ```

2. Install provisioning profile:
   ```bash
   # Create directory if it doesn't exist
   mkdir -p ~/Library/Developer/Xcode/UserData/Provisioning\ Profiles

   # Copy provisioning profile
   cp .secrets/DeadlyApp_AppStore2.mobileprovision ~/Library/Developer/Xcode/UserData/Provisioning\ Profiles/

   # Or let Xcode manage it by double-clicking the .mobileprovision file
   open .secrets/DeadlyApp_AppStore2.mobileprovision
   ```

3. Ensure your iOS device is:
   - Connected via USB
   - Trusted (if first time, unlock device and tap "Trust This Computer")
   - Registered in your Apple Developer account

#### Available Commands

```bash
# Build, install, and run on connected iOS device
make run-ios-device
```

The Xcode project is already configured for manual code signing with:
- **Development builds**: Uses "Apple Development" identity
- **Device builds**: Uses "iPhone Distribution" identity
- **Provisioning profile**: DeadlyApp_AppStore2

---

## Fastlane

Fastlane provides a consistent, scriptable interface for building and releasing apps.

### Installation

```bash
# macOS (using Homebrew)
brew install fastlane

# Or using RubyGems
gem install fastlane
```

### Android Lanes

Located in `android/fastlane/Fastfile`

#### build_debug
Builds debug APK for testing
```bash
cd android
fastlane build_debug
```

#### build_release
Builds signed release APK
```bash
cd android
fastlane build_release
```

#### build_bundle
Builds signed App Bundle for Play Store
```bash
cd android
fastlane build_bundle
```

#### deploy_device
Installs debug build to connected device
```bash
cd android
fastlane deploy_device
```

#### deploy_device_release
Installs signed release build to connected device
```bash
cd android
fastlane deploy_device_release
```

#### deploy_internal
Uploads to Play Store Internal Testing track
```bash
cd android
fastlane deploy_internal
```

### iOS Lanes

Located in `iosApp/fastlane/Fastfile`

#### build_debug
Builds debug app for simulator
```bash
cd iosApp
fastlane build_debug
```

#### build_release
Builds signed release IPA
```bash
cd iosApp
fastlane build_release
```

#### deploy_device
Builds and installs debug build to connected device
```bash
cd iosApp
fastlane deploy_device
```

#### sync_certs
Syncs certificates and provisioning profiles (configure for your setup)
```bash
cd iosApp
fastlane sync_certs
```

#### deploy_testflight
Uploads to TestFlight
```bash
cd iosApp
fastlane deploy_testflight
```

---

## GitHub Actions

### CI Workflows

#### android-ci.yml
- **Triggers**: PR and push to main/develop
- **Runs on**: Ubuntu Linux
- **Actions**:
  - Builds debug APK
  - Runs unit tests
  - Uploads APK artifact (7 day retention)

#### ios-ci.yml
- **Triggers**: PR and push to main/develop
- **Runs on**: macOS (required for iOS builds)
- **Actions**:
  - Builds KMM framework for simulator
  - Builds iOS app
  - Uploads build artifacts (7 day retention)

### Release Workflows

#### android-release.yml
- **Triggers**:
  - Manual workflow dispatch
  - Git tags matching `android-v*`
- **Actions**:
  - Decodes keystore from secrets
  - Builds signed APK and AAB
  - Uploads artifacts (30 day retention)

#### ios-release.yml
- **Triggers**:
  - Manual workflow dispatch
  - Git tags matching `ios-v*`
- **Actions**:
  - Decodes certificate and provisioning profile
  - Imports certificate to temporary keychain
  - Builds signed IPA
  - Uploads artifact (30 day retention)

### Required Secrets

Configure these in: **Settings → Secrets and variables → Actions**

#### Android Secrets

| Secret | Description | How to Generate |
|--------|-------------|-----------------|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded keystore | `base64 -i .secrets/my-release-key.jks \| pbcopy` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password | From your keystore.properties |
| `ANDROID_KEY_ALIAS` | Key alias | From your keystore.properties |
| `ANDROID_KEY_PASSWORD` | Key password | From your keystore.properties |

#### iOS Secrets

| Secret | Description | How to Generate |
|--------|-------------|-----------------|
| `IOS_CERTIFICATE_BASE64` | Base64-encoded P12 cert | `base64 -i .secrets/DeadlyApp_AppStore2.p12 \| pbcopy` |
| `IOS_CERTIFICATE_PASSWORD` | P12 password | Password used when exporting certificate |
| `IOS_PROVISIONING_PROFILE_BASE64` | Base64-encoded profile | `base64 -i .secrets/profile.mobileprovision \| pbcopy` |
| `IOS_TEAM_ID` | Apple Team ID | Found in Apple Developer portal |

#### Optional: App Store Connect API Key

For automated TestFlight uploads:

| Secret | Description |
|--------|-------------|
| `APP_STORE_CONNECT_KEY_ID` | API Key ID |
| `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID |
| `APP_STORE_CONNECT_KEY_BASE64` | Base64-encoded .p8 key file |

---

## Release Process

### Android Release

#### Option 1: Manual Trigger
1. Go to **Actions** tab in GitHub
2. Select **Android Release** workflow
3. Click **Run workflow**
4. Enter version number (e.g., `1.0.0`)
5. Click **Run workflow**

#### Option 2: Git Tag
```bash
git tag android-v1.0.0
git push origin android-v1.0.0
```

#### Retrieving Artifacts
1. Go to workflow run in Actions tab
2. Download `release-apk` or `release-aab` artifact
3. Extract and distribute or upload to Play Store

### iOS Release

#### Option 1: Manual Trigger
1. Go to **Actions** tab in GitHub
2. Select **iOS Release** workflow
3. Click **Run workflow**
4. Enter version number (e.g., `1.0.0`)
5. Click **Run workflow**

#### Option 2: Git Tag
```bash
git tag ios-v1.0.0
git push origin ios-v1.0.0
```

#### Retrieving Artifacts
1. Go to workflow run in Actions tab
2. Download `release-ipa` artifact
3. Extract and upload to App Store Connect or TestFlight

---

## Troubleshooting

### Android

#### "keystore.properties not found"
**Solution**: Create `.secrets/keystore.properties` with your signing credentials

#### "Keystore was tampered with, or password was incorrect"
**Solution**: Verify passwords in keystore.properties match your keystore

#### APK installs but won't run on device
**Solution**: Ensure device is not enforcing Play Protect restrictions for side-loaded apps

### iOS

#### "No provisioning profile matches"
**Solution**:
1. Check provisioning profile name matches "DeadlyApp_AppStore2"
2. Install profile by double-clicking: `open .secrets/DeadlyApp_AppStore2.mobileprovision`
3. Or manually copy to: `~/Library/Developer/Xcode/UserData/Provisioning Profiles/`
4. Verify profile hasn't expired
5. Check profile UUID matches in Xcode project settings

#### "Code signing identity not found"
**Solution**:
1. Import P12 certificate: `security import .secrets/DeadlyApp_AppStore2.p12`
2. Verify identity exists: `security find-identity -v -p codesigning`

#### "No iOS device detected"
**Solution**:
1. Ensure device is connected via USB
2. Trust computer on device if prompted
3. Check device appears in: `xcrun xctrace list devices`

### GitHub Actions

#### "Decode keystore failed"
**Solution**: Verify base64 encoding has no line breaks:
```bash
# macOS
base64 -i file.jks | pbcopy

# Linux
base64 -w 0 file.jks
```

#### "Certificate import failed"
**Solution**:
1. Ensure P12 password is correct in secrets
2. Verify P12 is properly base64 encoded
3. Check certificate hasn't expired

#### iOS build fails with "xcodebuild: error"
**Solution**: Check that:
1. Xcode version on runner matches your project requirements
2. Provisioning profile and certificate match
3. Bundle ID in Xcode matches your provisioning profile

---

## Security Best Practices

1. **Never commit secrets** - All signing materials stay in `.secrets/` (gitignored)
2. **Rotate secrets regularly** - Update keystores, certificates, and passwords periodically
3. **Use separate keystores** - Different keystores for debug and release builds
4. **Limit secret access** - Only give GitHub secret access to necessary people
5. **Enable 2FA** - Require 2FA on accounts with access to signing credentials

---

## Next Steps

- [ ] Set up Play Store service account for automated uploads
- [ ] Configure fastlane match for team-wide iOS certificate management
- [ ] Add automated version bumping
- [ ] Set up beta distribution channels
- [ ] Add automated changelog generation
- [ ] Configure notification system for failed builds
