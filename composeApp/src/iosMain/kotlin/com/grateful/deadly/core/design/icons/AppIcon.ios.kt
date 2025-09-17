package com.grateful.deadly.core.design.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.interop.UIKitView
import platform.UIKit.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue

/**
 * iOS implementation using SF Symbols via UIKit interop
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AppIcon.Render(size: Dp, tint: Color?) {
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

    UIKitView<UIImageView>(
        factory = {
            val config = UIImageSymbolConfiguration.configurationWithPointSize(size.value.toDouble())
            val image = UIImage.systemImageNamed(symbolName, config)!!
            val imageView = UIImageView(image)
            imageView.apply {
                // Use tint color if provided, otherwise use system primary color
                tintColor = tint?.let {
                    val argb = it.toArgb()
                    val alpha = ((argb shr 24) and 0xFF) / 255.0
                    val red = ((argb shr 16) and 0xFF) / 255.0
                    val green = ((argb shr 8) and 0xFF) / 255.0
                    val blue = (argb and 0xFF) / 255.0
                    UIColor.colorWithRed(red, green, blue, alpha)
                } ?: UIColor.systemBlueColor
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            }
            imageView
        },
        modifier = Modifier
    )
}