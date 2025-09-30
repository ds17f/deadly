package com.grateful.deadly.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.icons.AppIcon
import com.grateful.deadly.core.design.icons.Render
import com.grateful.deadly.feature.splash.model.SyncPhase
import deadly.composeapp.generated.resources.Res
import deadly.composeapp.generated.resources.deadly_logo
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * Splash screen for app initialization following V2 pattern.
 *
 * Displays:
 * - App logo and branding
 * - Loading state with progress indicators
 * - Phase-specific progress (downloading, extracting, importing)
 * - Current item being processed
 * - Elapsed time during import
 * - Error state with retry/skip options
 *
 * Automatically navigates to Home when initialization completes.
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    viewModel: SplashViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentTime by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }

    // Update timer every second while progress is showing
    LaunchedEffect(uiState.showProgress) {
        if (uiState.showProgress) {
            while (true) {
                delay(1000)
                currentTime = Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    // Navigate when ready
    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) {
            delay(500) // Brief delay to show completion message
            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Logo
            Image(
                painter = painterResource(Res.drawable.deadly_logo),
                contentDescription = "Deadly Logo",
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = "Deadly",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "The Killer App for the Golden Road",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Show progress, error, or completion state
            when {
                uiState.showError -> {
                    ErrorState(
                        errorMessage = uiState.errorMessage ?: "Unknown error",
                        onRetry = { viewModel.retry() },
                        onSkip = { viewModel.skip() }
                    )
                }

                uiState.showProgress -> {
                    ProgressState(
                        uiState = uiState,
                        currentTime = currentTime,
                        onSkip = { viewModel.skip() }
                    )
                }

                else -> {
                    // Initial loading state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )

                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error state with retry and skip options.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppIcon.Error.Render(
            size = 48.dp,
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Initialization Failed",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onSkip) {
                Text("Skip")
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Progress state with phase-specific indicators.
 */
@Composable
private fun ProgressState(
    uiState: com.grateful.deadly.feature.splash.model.SplashUiState,
    currentTime: Long,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Phase-specific progress indicator
        when (uiState.progress.phase) {
            SyncPhase.DOWNLOADING, SyncPhase.EXTRACTING -> {
                // Indeterminate progress for download/extraction
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }

            SyncPhase.IMPORTING_SHOWS, SyncPhase.IMPORTING_RECORDINGS -> {
                // Determinate progress for imports
                if (uiState.progress.totalItems > 0) {
                    val progress = uiState.progress.currentItems.toFloat() / uiState.progress.totalItems
                    LinearProgressIndicator(
                        progress = { if (progress.isFinite()) progress else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "${uiState.progress.currentItems} / ${uiState.progress.totalItems} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        Text(
            text = uiState.message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show current item being processed
        if (uiState.progress.currentItemName.isNotBlank()) {
            Text(
                text = uiState.progress.currentItemName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        // Show elapsed time during import
        if (uiState.progress.startTimeMs > 0L && uiState.progress.isInProgress) {
            Text(
                text = "Elapsed: ${uiState.progress.getElapsedTimeString(currentTime)}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // Skip button
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("Skip Import")
        }
    }
}
