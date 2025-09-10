package com.grateful.deadly.core.design.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.interop.UIKitView
import platform.UIKit.*

/**
 * iOS implementation using SF Symbols via UIKit interop
 */
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
                // TODO: Match MaterialTheme.colorScheme.primary (see docs/TODO.md)
                tintColor = UIColor.systemBlueColor 
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            }
        },
        modifier = Modifier
    )
}