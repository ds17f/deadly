# GitHub Actions CI/CD Workflows

This directory contains the GitHub Actions workflows for building and releasing the Deadly KMM app.

## Workflows

### `android-ci.yml`
Runs on every PR and push to main/develop branches. Builds debug APK and runs Android tests.

### `ios-ci.yml`
Runs on every PR and push to main/develop branches. Builds the iOS framework and app for simulator.

### `android-release.yml`
Builds signed release APK and AAB. Can be triggered manually or by pushing a tag like `android-v1.0.0`.

### `ios-release.yml`
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

### Optional: App Store Connect API Key (for TestFlight uploads)

- `APP_STORE_CONNECT_KEY_ID`: Key ID from App Store Connect
- `APP_STORE_CONNECT_ISSUER_ID`: Issuer ID from App Store Connect
- `APP_STORE_CONNECT_KEY_BASE64`: Base64-encoded API key (.p8 file)

## Triggering Releases

### Manual Trigger
Go to Actions → Select workflow → Run workflow → Enter version number

### Git Tag Trigger
```bash
# Android release
git tag android-v1.0.0
git push origin android-v1.0.0

# iOS release
git tag ios-v1.0.0
git push origin ios-v1.0.0
```

## Security Notes

- Never commit secrets to the repository
- All secrets are only available during workflow execution
- Secrets are automatically masked in workflow logs
- Temporary files containing secrets are cleaned up after each run
