package com.grateful.deadly

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.ComposeUIViewController
import com.grateful.deadly.feature.splash.SplashScreen
import com.grateful.deadly.feature.splash.SplashViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

fun MainViewController() = ComposeUIViewController {
    com.grateful.deadly.core.logging.Logger.i("MainViewController", "🔍 🎬 MainViewController: About to invoke App()")
    App()
    com.grateful.deadly.core.logging.Logger.i("MainViewController", "🔍 ✅ MainViewController: App() returned")
}

/**
 * Creates a UIViewController that shows the Compose SplashScreen.
 * Used by iOS SwiftUI to embed the splash screen during initialization.
 */
fun createSplashViewController(onSplashComplete: () -> Unit) = ComposeUIViewController {
    // Get SplashViewModel from Koin
    val koinHelper = object : KoinComponent {}
    val splashViewModel: SplashViewModel = koinHelper.get()

    // Get theme manager to apply proper theme
    val themeManager: com.grateful.deadly.core.design.theme.ThemeManager = koinHelper.get()
    val currentTheme = themeManager.currentTheme.collectAsState(
        initial = com.grateful.deadly.core.design.theme.ThemeMode.SYSTEM
    )

    com.grateful.deadly.core.design.theme.DeadlyTheme(themeMode = currentTheme.value) {
        SplashScreen(
            onSplashComplete = onSplashComplete,
            viewModel = splashViewModel
        )
    }
}