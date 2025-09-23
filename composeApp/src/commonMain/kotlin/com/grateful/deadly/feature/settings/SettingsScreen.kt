package com.grateful.deadly.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grateful.deadly.core.design.theme.ThemeMode
import com.grateful.deadly.core.design.theme.ThemeManager
import com.grateful.deadly.services.data.DataSyncOrchestrator
import com.grateful.deadly.services.data.SyncProgress
import com.grateful.deadly.services.data.SyncResult
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

object SettingsDIHelper : KoinComponent

/**
 * SettingsScreen - App configuration interface
 *
 * Simple settings content for theme management and configuration.
 * Scaffold-free content designed for use within AppScaffold.
 */
@Composable
fun SettingsScreen() {
    val themeManager: ThemeManager = remember { SettingsDIHelper.get() }
    val dataSyncOrchestrator: DataSyncOrchestrator = remember { SettingsDIHelper.get() }
    val currentTheme by themeManager.currentTheme.collectAsState(initial = ThemeMode.SYSTEM)
    val syncProgress by dataSyncOrchestrator.progress.collectAsState(initial = SyncProgress.Idle)

    val scope = rememberCoroutineScope()
    var importMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Database Management Section
        item {
            SettingsSection(title = "Database") {
                DatabaseManagement(
                    importProgress = syncProgress,
                    importMessage = importMessage,
                    onImportData = {
                        scope.launch {
                            importMessage = null
                            val result = dataSyncOrchestrator.syncData()
                            importMessage = when (result) {
                                is SyncResult.Success -> "Successfully imported ${result.showCount} shows, ${result.recordingCount} recordings"
                                is SyncResult.AlreadyExists -> "Database already contains ${result.showCount} shows, ${result.recordingCount} recordings"
                                is SyncResult.Error -> "Import failed: ${result.message}"
                                is SyncResult.Cleared -> "Database cleared"
                            }
                        }
                    },
                    onRefreshData = {
                        scope.launch {
                            importMessage = null
                            val result = dataSyncOrchestrator.forceRefreshData()
                            importMessage = when (result) {
                                is SyncResult.Success -> "Successfully refreshed ${result.showCount} shows, ${result.recordingCount} recordings"
                                is SyncResult.Error -> "Refresh failed: ${result.message}"
                                else -> "Refresh completed"
                            }
                        }
                    },
                    onClearData = {
                        scope.launch {
                            importMessage = null
                            val result = dataSyncOrchestrator.clearAllData()
                            importMessage = when (result) {
                                is SyncResult.Cleared -> "Database cleared successfully"
                                is SyncResult.Error -> "Clear failed: ${result.message}"
                                else -> "Clear completed"
                            }
                        }
                    }
                )
            }
        }

        // Themes Section
        item {
            SettingsSection(title = "Appearance") {
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeSelected = { themeManager.setThemeMode(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // App Info Section
        item {
            SettingsSection(title = "About") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deadly - Grateful Dead Archive",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Cross-platform app for exploring Grateful Dead recordings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Built with Kotlin Multiplatform + Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Reusable settings section component
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            content()
        }
    }
}

/**
 * Theme selector component with radio buttons
 */
@Composable
private fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { theme ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentTheme == theme,
                    onClick = { onThemeSelected(theme) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Database management component with import/clear/refresh operations and cache management
 */
@Composable
private fun DatabaseManagement(
    importProgress: SyncProgress,
    importMessage: String?,
    onImportData: () -> Unit,
    onRefreshData: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress indicator
        when (importProgress) {
            is SyncProgress.Idle -> {
                // No progress indicator when idle
            }
            is SyncProgress.Downloading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Downloading data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is SyncProgress.Extracting -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Extracting files...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SyncProgress.Clearing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Clearing database...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is SyncProgress.ImportingShows -> {
                LinearProgressIndicator(
                    progress = { if (importProgress.total > 0) importProgress.current.toFloat() / importProgress.total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Importing shows: ${importProgress.current}/${importProgress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is SyncProgress.ImportingRecordings -> {
                LinearProgressIndicator(
                    progress = { if (importProgress.total > 0) importProgress.current.toFloat() / importProgress.total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Importing recordings: ${importProgress.current}/${importProgress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status message
        importMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }


        // Action buttons
        val isOperationInProgress = importProgress !is SyncProgress.Idle

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onImportData,
                enabled = !isOperationInProgress,
                modifier = Modifier.weight(1f)
            ) {
                Text("Import Data")
            }

            OutlinedButton(
                onClick = onRefreshData,
                enabled = !isOperationInProgress,
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClearData,
                enabled = !isOperationInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Database")
            }

        }

        // Help text
        Text(
            text = "Import: Load show data into database\n" +
                    "Refresh: Clear database and re-download data\n" +
                    "Clear Database: Remove all shows from database\n" +
                    "Delete Cache: Remove cached data.zip file to force re-download",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

