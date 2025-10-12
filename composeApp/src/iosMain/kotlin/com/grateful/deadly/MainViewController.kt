package com.grateful.deadly

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    com.grateful.deadly.core.logging.Logger.i("MainViewController", "🔍 🎬 MainViewController: About to invoke App()")
    App()
    com.grateful.deadly.core.logging.Logger.i("MainViewController", "🔍 ✅ MainViewController: App() returned")
}