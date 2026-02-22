package com.grateful.deadly.services.library

import com.grateful.deadly.data.library.LibraryDao
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class LibraryExportEntry(
    val showId: String,
    val addedToLibraryAt: Long,
    val isPinned: Boolean,
    val libraryNotes: String? = null,
    val customRating: Double? = null,
    val lastAccessedAt: Long? = null,
    val tags: List<String>? = null
)

@Serializable
data class LibraryExportFile(
    val version: Int,
    val exportedAt: Long,
    val app: String,
    val library: List<LibraryExportEntry>
)

class LibraryExportService(private val libraryDao: LibraryDao) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
    }

    suspend fun exportLibrary(): String {
        val rows = libraryDao.getAllLibraryShowsOnce()
        val entries = rows.map { row ->
            LibraryExportEntry(
                showId = row.showId,
                addedToLibraryAt = row.addedToLibraryAt,
                isPinned = row.isPinned == 1L,
                libraryNotes = row.libraryNotes,
                customRating = row.customRating,
                lastAccessedAt = row.lastAccessedAt,
                tags = row.tags
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.ifEmpty { null }
            )
        }
        val export = LibraryExportFile(
            version = 1,
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            app = "deadly-kmp",
            library = entries
        )
        return json.encodeToString(export)
    }

    fun exportFilename(): String {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "grateful-dead-library-$date.json"
    }
}
