# GitHub Actions CI/CD Workflows

This directory contains the GitHub Actions workflows for building and releasing the Deadly KMM app.

## Hybrid Runner Architecture

All deployment workflows use a **hybrid runner strategy** that prefers self-hosted runners with automatic fallback to GitHub-hosted runners:

1. **Primary**: Self-hosted MacBook runner (`[self-hosted, macOS, ARM64]`)
2. **Fallback**: GitHub-hosted runners (`macos-latest` or `ubuntu-latest`)

This approach provides:
- ✅ **Cost savings**: Free self-hosted runner when available
- ✅ **Reliability**: Automatic fallback if self-hosted runner is offline
- ✅ **Speed**: Faster builds on local hardware with cached dependencies
- ✅ **Zero manual intervention**: Workflow automatically detects and switches

### How It Works

```yaml
# Try self-hosted first
deploy-ios-self-hosted:
  runs-on: [self-hosted, macOS, ARM64]
  continue-on-error: true  # Don't fail workflow if offline

# Automatically fallback if self-hosted failed
deploy-ios-github-hosted:
  needs: deploy-ios-self-hosted
  if: needs.deploy-ios-self-hosted.result == 'failure' || ...
  runs-on: macos-latest
```

### Reusable Workflows

To eliminate duplication, deployment logic is extracted into reusable workflows:

- **`deploy-ios-testflight-reusable.yml`**: iOS deployment logic (called with different runners)
- **`deploy-android-playstore-reusable.yml`**: Android deployment logic (called with different runners)
- **`deploy-testing.yml`**: Orchestrates both with hybrid runner strategy

Benefits:
- Single source of truth for deployment steps
- Easy to maintain and update
- Consistent behavior across self-hosted and GitHub-hosted runners

## Workflows

### CI Workflows

#### `android-ci.yml`
Runs on every PR and push to main/develop branches. Builds debug APK and runs Android tests.

#### `ios-ci.yml`
Runs on every PR and push to main/develop branches. Builds the iOS framework and app for simulator.

### Deployment Workflows

#### `deploy-testing.yml`
**Triggered by**: Version tags (`v*`, excluding `production-v*`)

Deploys to testing environments using hybrid runner strategy:
- iOS → TestFlight
- Android → Play Store Internal Testing
- Creates GitHub pre-release

Example: `git tag v1.2.0 && git push origin v1.2.0`

#### `deploy-production.yml` (if exists)
Production deployment workflow (similar hybrid pattern)

### Reusable Workflows (Internal)

#### `deploy-ios-testflight-reusable.yml`
Reusable workflow for iOS TestFlight deployment. Accepts `runner-type` parameter.

#### `deploy-android-playstore-reusable.yml`
Reusable workflow for Android Play Store deployment. Accepts `runner-type` parameter.

### Legacy Release Workflows

#### `android-release.yml`
Builds signed release APK and AAB. Can be triggered manually or by pushing a tag like `android-v1.0.0`.

#### `ios-release.yml`
Builds signed release IPA. Can be triggered manually or by pushing a tag like `ios-v1.0.0`.

## Required GitHub Secrets

You must configure the following secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

### Android Secrets

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded keystore file
  ```bash
  base64 -i .secrets/my-release-key.jks | pbcopy  # macOS
  base64 -w 0 .secrets/my-release-key.jks          # Linux
  ```

- `ANDROID_KEYSTORE_PASSWORD`: Password for the keystore
- `ANDROID_KEY_ALIAS`: Alias of the key in the keystore
- `ANDROID_KEY_PASSWORD`: Password for the key

### iOS Secrets

- `IOS_CERTIFICATE_BASE64`: Base64-encoded P12 certificate
  ```bash
  base64 -i .secrets/DeadlyApp_AppStore2.p12 | pbcopy  # macOS
  base64 -w 0 .secrets/DeadlyApp_AppStore2.p12          # Linux
  ```

- `IOS_CERTIFICATE_PASSWORD`: Password for the P12 certificate
- `IOS_PROVISIONING_PROFILE_BASE64`: Base64-encoded provisioning profile
  ```bash
  base64 -i .secrets/profile.mobileprovision | pbcopy  # macOS
  base64 -w 0 .secrets/profile.mobileprovision          # Linux
  ```

- `IOS_TEAM_ID`: Your Apple Developer Team ID (10 characters, found in Apple Developer portal)

### App Store Connect API Key (for TestFlight uploads)

- `APP_STORE_CONNECT_API_KEY`: Base64-encoded API key (.p8 file)
  ```bash
  base64 -i .secrets/AuthKey_*.p8 | pbcopy  # macOS
  ```

### Play Store Service Account (for Play Store uploads)

- `PLAY_STORE_SERVICE_ACCOUNT_JSON`: Base64-encoded service account JSON
  ```bash
  base64 -i .secrets/play-store-service-account.json | pbcopy  # macOS
  ```

## Triggering Releases

### Testing Release (TestFlight + Play Store Internal)
```bash
# Create version tag
git tag v1.2.0
git push origin v1.2.0

# This triggers deploy-testing.yml which:
# 1. Tries self-hosted runner first
# 2. Falls back to GitHub-hosted if needed
# 3. Deploys iOS to TestFlight
# 4. Deploys Android to Play Store Internal
# 5. Creates GitHub pre-release
```

### Production Release
```bash
# Create production tag
git tag production-v1.2.0
git push origin production-v1.2.0

# This triggers deploy-production.yml
```

### Manual Trigger (Legacy)
Go to Actions → Select workflow → Run workflow → Enter version number

### Legacy Platform-Specific Releases
```bash
# Android release only
git tag android-v1.0.0
git push origin android-v1.0.0

# iOS release only
git tag ios-v1.0.0
git push origin ios-v1.0.0
```

## Self-Hosted Runner Setup

To use the self-hosted runner with your MacBook:

1. **Register the runner** in GitHub:
   - Go to Settings → Actions → Runners → New self-hosted runner
   - Follow the setup instructions for macOS
   - Apply labels: `self-hosted`, `macOS`, `ARM64`

2. **Install dependencies** on the runner:
   ```bash
   # Install Homebrew if not already installed
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

   # Install required tools
   brew install fastlane
   brew install --cask temurin17  # Java 17

   # Install Ruby for Fastlane (if using system Ruby)
   gem install fastlane
   ```

3. **Start the runner**:
   ```bash
   cd actions-runner
   ./run.sh
   ```

4. **Optional: Run as a service** (macOS):
   ```bash
   ./svc.sh install
   ./svc.sh start
   ```

### Troubleshooting

- **Runner offline**: Workflow automatically uses GitHub-hosted fallback
- **Check runner status**: Settings → Actions → Runners
- **View runner logs**: `actions-runner/_diag/` directory

## Security Notes

- Never commit secrets to the repository
- All secrets are only available during workflow execution
- Secrets are automatically masked in workflow logs
- Temporary files containing secrets are cleaned up after each run
- Self-hosted runners should be on trusted networks with proper security measures
