package com.grateful.deadly.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Show domain model components - value objects that compose the Show domain model.
 */

@Serializable
data class Venue(
    val name: String,
    val city: String?,
    val state: String?,
    val country: String
) {
    val displayLocation: String
        get() = listOfNotNull(city, state, country.takeIf { it != "USA" })
            .joinToString(", ")
}

@Serializable
data class Location(
    val displayText: String,
    val city: String?,
    val state: String?
) {
    companion object {
        fun fromRaw(raw: String?, city: String?, state: String?): Location {
            val display = raw ?: listOfNotNull(city, state).joinToString(", ").ifEmpty { "Unknown Location" }
            return Location(display, city, state)
        }
    }
}

@Serializable
data class Setlist(
    val status: String,
    val sets: List<SetlistSet>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Setlist? {
            if (json.isNullOrBlank() || status.isNullOrBlank()) return null

            return try {
                // For now, create a simple setlist structure from the JSON string
                // This will be enhanced when we need full parsing
                Setlist(
                    status = status,
                    sets = emptyList(), // TODO: Parse JSON when needed
                    raw = json
                )
            } catch (e: Exception) {
                // If parsing fails, return null
                null
            }
        }
    }
}

@Serializable
data class SetlistSet(
    val name: String,
    val songs: List<SetlistSong>
)

@Serializable
data class SetlistSong(
    val name: String,
    val position: Int,
    val hasSegue: Boolean = false,
    val segueSymbol: String? = null
) {
    val displayName: String
        get() = if (hasSegue && segueSymbol != null) {
            "$name $segueSymbol"
        } else {
            name
        }
}

@Serializable
data class Lineup(
    val status: String,
    val members: List<LineupMember>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Lineup? {
            if (json.isNullOrBlank() || status.isNullOrBlank()) return null

            return try {
                // For now, create a simple lineup structure from the JSON string
                // This will be enhanced when we need full parsing
                Lineup(
                    status = status,
                    members = emptyList(), // TODO: Parse JSON when needed
                    raw = json
                )
            } catch (e: Exception) {
                // If parsing fails, return basic structure
                Lineup(
                    status = status ?: "unknown",
                    members = emptyList(),
                    raw = json
                )
            }
        }
    }
}

@Serializable
data class LineupMember(
    val name: String,
    val instruments: String
)