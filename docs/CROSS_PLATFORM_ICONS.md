# Cross-Platform Icons in KMM

## Overview

In a KMM app, we want to display native icons:
- **Android** → Material Symbols font glyphs
- **iOS** → SF Symbols via SwiftUI

We use an abstraction layer: a single shared enum (`AppIcon`) that represents semantic icons (e.g. "QR Scanner", "Settings"). Each platform renders these icons using its native approach.

## Why This Approach

- Keeps common code free of platform dependencies
- Provides a consistent API (`AppIcon.Render()`) across Android + iOS  
- Ensures the UI looks native on each platform (Material Symbols vs SF Symbols)
- Makes it easy to add new icons: just extend `AppIcon` and update platform implementations
- Uses platform-optimized rendering (font glyphs on Android, vector symbols on iOS)

## Implementation

### 1. Shared Icon Enum (commonMain)

```kotlin
// commonMain
enum class AppIcon {
    QrCodeScanner,
    Home, 
    Settings,
    Search,
    // Add more icons as needed
}

// Platform-specific rendering function
@Composable
expect fun AppIcon.Render(size: Dp = 24.dp)
```

### 2. Android Implementation (androidMain)

Uses Material Symbols font with Unicode codepoints:

```kotlin  
// androidMain
private val MaterialSymbols = FontFamily(
    Font(R.font.material_symbols_outlined)
)

@Composable
actual fun AppIcon.Render(size: Dp) {
    val codepoint = when (this) {
        AppIcon.QrCodeScanner -> "\uE4C6"  // qr_code_scanner
        AppIcon.Home -> "\uE88A"           // home
        AppIcon.Settings -> "\uE8B8"       // settings
        AppIcon.Search -> "\uE8B6"         // search
    }

    Text(
        text = codepoint,
        fontFamily = MaterialSymbols,
        fontSize = size.value.sp,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.size(size)
    )
}
```

### 3. iOS Implementation (iosMain)

Uses SF Symbols via UIKit interop:

```kotlin
// iosMain
@Composable
actual fun AppIcon.Render(size: Dp) {
    val symbolName = when (this) {
        AppIcon.QrCodeScanner -> "qrcode.viewfinder"
        AppIcon.Home -> "house.fill"
        AppIcon.Settings -> "gearshape.fill"
        AppIcon.Search -> "magnifyingglass"
    }

    UIKitView(
        factory = {
            val config = UIImage.SymbolConfiguration.configurationWithPointSize(size.value.toDouble())
            val image = UIImage.systemImageNamed(symbolName, config)!!
            UIImageView(image).apply {
                tintColor = UIColor.systemBlueColor // TODO: Match MaterialTheme colors
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            }
        },
        modifier = Modifier
    )
}
```

## Usage in Shared Code

```kotlin
// CRITICAL: Must explicitly import the expect function
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render

// In any @Composable function in commonMain:
AppIcon.QrCodeScanner.Render(size = 24.dp)
AppIcon.Search.Render(size = 32.dp)
```

**⚠️ Important**: The explicit `import Render` is required for expect extension functions in Kotlin Multiplatform. Without it, you'll get "Unresolved reference 'Render'" errors.

## Adding New Icons

1. Add the semantic name to `AppIcon` enum in `commonMain`
2. Add Material Symbols codepoint in `androidMain` 
3. Add SF Symbol name in `iosMain`
4. Use `AppIcon.NewIcon.Render()` in shared code

## Setup Requirements

### Android
- Add Material Symbols font file to `androidMain/res/font/material_symbols_outlined.ttf`
- Font available from [GitHub](https://github.com/google/material-design-icons/tree/master/variablefont)

### iOS  
- SF Symbols are built-in to iOS (no additional setup needed)

## Benefits

✅ Unified `Render()` API across platforms  
✅ Native rendering performance (font glyphs vs vector symbols)  
✅ Platform-appropriate styling and colors  
✅ Type-safe icon references  
✅ Easy to extend with new icons