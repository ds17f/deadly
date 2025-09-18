package com.grateful.deadly.core.design.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.interop.UIKitView
import platform.UIKit.*
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation using SF Symbols via UIKit interop
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppIcon.Render(size: Dp, tint: Color?) {
    // Use the same color logic as Android: tint parameter OR MaterialTheme primary
    val iconColor = tint ?: MaterialTheme.colorScheme.primary
    // Map your AppIcon enum to SF Symbol names
    val symbolName = when (this) {
        AppIcon.QrCodeScanner -> "qrcode.viewfinder"
        AppIcon.Home -> "house.fill"
        AppIcon.Settings -> "gearshape.fill"
        AppIcon.Search -> "magnifyingglass"
        AppIcon.ArrowBack -> "arrow.left"
        AppIcon.Clear -> "xmark"
        AppIcon.Edit -> "pencil"
        AppIcon.AlbumArt -> "music.note"
        AppIcon.CheckCircle -> "checkmark.circle.fill"
        AppIcon.LibraryMusic -> "music.note.list"
        AppIcon.Collections -> "folder.fill"
    }

    UIKitView(
        factory = {
            // Configure symbol size
            val config = UIImageSymbolConfiguration.configurationWithPointSize(size.value.toDouble())

            // Force the symbol into template mode so tintColor actually applies
            val image = UIImage.systemImageNamed(symbolName, config)
                ?.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate)
                ?: UIImage.systemImageNamed("questionmark.square.dashed", config)
                    ?.imageWithRenderingMode(UIImageRenderingMode.UIImageRenderingModeAlwaysTemplate)

            // Create UIImageView with the symbol
            val imageView = UIImageView(image).apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit

                // Convert the Compose Color to UIColor (same logic as Android's Text color)
                val argb = iconColor.toArgb()
                val alpha = ((argb shr 24) and 0xFF) / 255.0
                val red = ((argb shr 16) and 0xFF) / 255.0
                val green = ((argb shr 8) and 0xFF) / 255.0
                val blue = (argb and 0xFF) / 255.0
                tintColor = UIColor.colorWithRed(red, green, blue, alpha)

                // Ensure transparent background (like Android Text)
                backgroundColor = UIColor.clearColor
            }
            imageView
        },
        // Important: give the Compose wrapper a size, or the imageView may collapse
        modifier = Modifier.size(size)
    )
}