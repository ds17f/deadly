package com.grateful.deadly.feature.settings

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.uikit.LocalUIViewController
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDocumentPickerViewController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ShareTextButton(
    label: String,
    content: String,
    filename: String,
    enabled: Boolean
) {
    val uiViewController = LocalUIViewController.current
    OutlinedButton(
        onClick = {
            val tempPath = NSTemporaryDirectory() + filename
            @Suppress("CAST_NEVER_SUCCEEDS")
            (content as NSString).writeToFile(
                path = tempPath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            val fileUrl = NSURL.fileURLWithPath(tempPath)
            val documentPicker = UIDocumentPickerViewController(
                forExportingURLs = listOf(fileUrl)
            )
            uiViewController.presentViewController(
                viewControllerToPresent = documentPicker,
                animated = true,
                completion = null
            )
        },
        enabled = enabled
    ) {
        Text(label)
    }
}
