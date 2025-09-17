package com.grateful.deadly.core.design.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
actual fun AppIcon.Render(size: Dp) {
    val symbolName = when (this) {
        AppIcon.QrCodeScanner -> "qrcode.viewfinder"
        AppIcon.Home -> "house.fill"
        AppIcon.Settings -> "gearshape.fill"
        AppIcon.Search -> "magnifyingglass"
    }

    UIKitView<UIImageView>(
        factory = {
            val config = UIImageSymbolConfiguration.configurationWithPointSize(size.value.toDouble())
            val image = UIImage.systemImageNamed(symbolName, config)!!
            val imageView = UIImageView(image)
            imageView.apply {
                // Use black for maximum visibility
                tintColor = UIColor.blackColor
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
            }
            imageView
        },
        modifier = Modifier
    )
}