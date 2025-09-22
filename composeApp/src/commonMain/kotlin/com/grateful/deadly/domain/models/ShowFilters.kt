package com.grateful.deadly.domain.models

/**
 * Type-safe filters for show queries.
 * All parameters are optional - null means "no filter applied".
 */
data class ShowFilters(
    // Date filtering
    val year: Int? = null,
    val startYear: Int? = null,
    val endYear: Int? = null,
    val yearMonth: String? = null, // Format: "1977-05"
    val startDate: String? = null, // Format: "1977-05-08"
    val endDate: String? = null,

    // Location filtering
    val venueName: String? = null,
    val city: String? = null,
    val state: String? = null,

    // Content filtering
    val hasSetlist: Boolean? = null,
    val songName: String? = null,
    val hasLineup: Boolean? = null,
    val memberName: String? = null,

    // Recording filtering
    val hasRecordings: Boolean? = null,
    val minRating: Double? = null,
    val minReviews: Int? = null,

    // Library filtering
    val isInLibrary: Boolean? = null,

    // Ordering and limiting
    val orderBy: ShowOrderBy = ShowOrderBy.DATE,
    val limit: Int? = null
) {
    companion object {
        /**
         * Get all shows (no filters)
         */
        fun all() = ShowFilters()

        /**
         * Get shows for a specific year
         */
        fun forYear(year: Int) = ShowFilters(year = year)

        /**
         * Get shows in a year range
         */
        fun forYearRange(startYear: Int, endYear: Int) = ShowFilters(
            startYear = startYear,
            endYear = endYear
        )

        /**
         * Get shows for a specific year-month
         */
        fun forYearMonth(yearMonth: String) = ShowFilters(yearMonth = yearMonth)

        /**
         * Get shows in a date range
         */
        fun forDateRange(startDate: String, endDate: String) = ShowFilters(
            startDate = startDate,
            endDate = endDate
        )

        /**
         * Search shows by venue name
         */
        fun forVenue(venueName: String) = ShowFilters(venueName = venueName)

        /**
         * Search shows by city
         */
        fun forCity(city: String) = ShowFilters(city = city)

        /**
         * Get shows by state
         */
        fun forState(state: String) = ShowFilters(state = state)

        /**
         * Get shows with setlists
         */
        fun withSetlists() = ShowFilters(hasSetlist = true)

        /**
         * Search shows by song name
         */
        fun withSong(songName: String) = ShowFilters(
            hasSetlist = true,
            songName = songName
        )

        /**
         * Get shows with lineups
         */
        fun withLineups() = ShowFilters(hasLineup = true)

        /**
         * Search shows by band member
         */
        fun withMember(memberName: String) = ShowFilters(
            hasLineup = true,
            memberName = memberName
        )

        /**
         * Get shows with recordings
         */
        fun withRecordings() = ShowFilters(hasRecordings = true)

        /**
         * Get top-rated shows
         */
        fun topRated(minReviews: Int = 5, limit: Int = 50) = ShowFilters(
            minReviews = minReviews,
            orderBy = ShowOrderBy.RATING,
            limit = limit
        )

        /**
         * Get shows in user's library
         */
        fun inLibrary() = ShowFilters(
            isInLibrary = true,
            orderBy = ShowOrderBy.LIBRARY
        )
    }
}

/**
 * Available ordering options for show queries
 */
enum class ShowOrderBy(val value: String) {
    DATE("date"),
    RATING("rating"),
    LIBRARY("library")
}