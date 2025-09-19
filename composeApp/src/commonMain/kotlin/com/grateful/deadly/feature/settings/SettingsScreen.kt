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
import com.grateful.deadly.services.data.DataImportService
import com.grateful.deadly.services.data.ImportProgress
import com.grateful.deadly.services.data.ImportResult
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
    val dataImportService: DataImportService = remember { SettingsDIHelper.get() }
    val currentTheme by themeManager.currentTheme.collectAsState(initial = ThemeMode.SYSTEM)
    val importProgress by dataImportService.progress.collectAsState(initial = ImportProgress.Idle)

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
                    importProgress = importProgress,
                    importMessage = importMessage,
                    onImportData = {
                        scope.launch {
                            importMessage = null
                            val result = dataImportService.importMockData()
                            importMessage = when (result) {
                                is ImportResult.Success -> "Successfully imported ${result.showCount} shows"
                                is ImportResult.AlreadyExists -> "Database already contains ${result.showCount} shows"
                                is ImportResult.Error -> "Import failed: ${result.message}"
                                is ImportResult.Cleared -> "Database cleared"
                            }
                        }
                    },
                    onRefreshData = {
                        scope.launch {
                            importMessage = null
                            val result = dataImportService.forceRefreshData()
                            importMessage = when (result) {
                                is ImportResult.Success -> "Successfully refreshed ${result.showCount} shows"
                                is ImportResult.Error -> "Refresh failed: ${result.message}"
                                else -> "Refresh completed"
                            }
                        }
                    },
                    onClearData = {
                        scope.launch {
                            importMessage = null
                            val result = dataImportService.clearAllData()
                            importMessage = when (result) {
                                is ImportResult.Cleared -> "Database cleared successfully"
                                is ImportResult.Error -> "Clear failed: ${result.message}"
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
 * Database management component with import/clear/refresh operations
 */
@Composable
private fun DatabaseManagement(
    importProgress: ImportProgress,
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
            is ImportProgress.Idle -> {
                // No progress indicator when idle
            }
            is ImportProgress.Downloading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Downloading data...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ImportProgress.Extracting -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Extracting files...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ImportProgress.Parsing -> {
                LinearProgressIndicator(
                    progress = { if (importProgress.total > 0) importProgress.current.toFloat() / importProgress.total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Parsing files: ${importProgress.current}/${importProgress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is ImportProgress.Importing -> {
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
            is ImportProgress.Clearing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Clearing database...",
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
        val isOperationInProgress = importProgress !is ImportProgress.Idle

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

        Button(
            onClick = onClearData,
            enabled = !isOperationInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Database")
        }

        // Help text
        Text(
            text = "Import: Load show data into database\nRefresh: Clear and reimport all data\nClear: Remove all data from database",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}