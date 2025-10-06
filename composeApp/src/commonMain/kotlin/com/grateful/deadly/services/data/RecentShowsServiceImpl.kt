package com.grateful.deadly.services.data

import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.services.show.ShowService
import com.grateful.deadly.services.show.RecentShowsStats
import com.grateful.deadly.services.media.MediaService
import com.grateful.deadly.services.media.PlaybackStatus
import com.grateful.deadly.core.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

class RecentShowsServiceImpl(
    private val showService: ShowService,
    private val mediaService: MediaService,
    private val applicationScope: CoroutineScope
) : RecentShowsService {

    companion object {
        private const val TAG = "RecentShowsService"
        // V2 constants (from analysis)
        private const val MEANINGFUL_PLAY_DURATION_MS = 10_000L // 10 seconds
        private const val MEANINGFUL_PLAY_PERCENTAGE = 0.25f // 25% of track
        private const val DEFAULT_RECENT_LIMIT = 8
    }

    private val _recentShows = MutableStateFlow<List<Show>>(emptyList())
    override val recentShows: StateFlow<List<Show>> = _recentShows.asStateFlow()

    // Track state for debouncing (matches V2 pattern)
    private var currentTrackShowId: String? = null
    private var currentTrackRecordingId: String? = null
    private var currentTrackStartTime: Long = 0
    private var hasRecordedCurrentTrack = false
    private var trackingJob: Job? = null

    override fun startTracking() {
        Logger.d(TAG, "📱 Starting RecentShowsService tracking...")
        stopTracking() // Ensure clean state

        // Start observing recent shows from database (V2 pattern)
        startObservingRecentShows()

        // Start observing playback for automatic tracking (V2 pattern)
        startObservingPlayback()
        Logger.d(TAG, "📱 RecentShowsService tracking started successfully")
    }

    override fun stopTracking() {
        Logger.d(TAG, "📱 Stopping RecentShowsService tracking...")
        trackingJob?.cancel()
        trackingJob = null
        resetTrackingState()
        Logger.d(TAG, "📱 RecentShowsService tracking stopped")
    }

    private fun startObservingRecentShows() {
        Logger.d(TAG, "📱 Starting database observation for recent shows...")
        applicationScope.launch {
            // Convert database flow to StateFlow (matches V2)
            showService.getRecentShowsFlow(DEFAULT_RECENT_LIMIT)
                .flowOn(Dispatchers.IO)
                .collect { shows ->
                    Logger.d(TAG, "📱 Database updated: ${shows.size} recent shows")
                    _recentShows.value = shows
                }
        }
    }

    private fun startObservingPlayback() {
        Logger.d(TAG, "📱 Starting MediaService playback observation...")
        trackingJob = applicationScope.launch {
            // Observe MediaService state changes (V2 pattern)
            combine(
                mediaService.currentShowId,
                mediaService.currentRecordingId,
                mediaService.playbackStatus,
                mediaService.isPlaying
            ) { showId, recordingId, playbackStatus, isPlaying ->
                PlaybackInfo(showId, recordingId, playbackStatus, isPlaying)
            }
                .distinctUntilChanged()
                .collect { playbackInfo ->
                    handlePlaybackStateChange(playbackInfo)
                }
        }
    }

    private suspend fun handlePlaybackStateChange(playbackInfo: PlaybackInfo) {
        val showId = playbackInfo.showId ?: return
        val recordingId = playbackInfo.recordingId

        // Track changes (V2 debouncing pattern) - reset if EITHER show OR recording changes
        if (showId != currentTrackShowId || recordingId != currentTrackRecordingId) {
            Logger.d(TAG, "📱 Track change detected: $currentTrackShowId/$currentTrackRecordingId -> $showId/$recordingId")
            resetTrackingState()
            currentTrackShowId = showId
            currentTrackRecordingId = recordingId
            currentTrackStartTime = Clock.System.now().toEpochMilliseconds()
            hasRecordedCurrentTrack = false
        }

        // Only record once per track when playing and meets threshold
        if (playbackInfo.isPlaying &&
            !hasRecordedCurrentTrack &&
            shouldRecordPlay(playbackInfo.playbackStatus)) {

            val position = playbackInfo.playbackStatus.currentPosition
            val duration = playbackInfo.playbackStatus.duration
            val recordingId = playbackInfo.recordingId
            Logger.d(TAG, "📱 Play threshold met: ${position/1000}s / ${duration/1000}s - recording show play (recordingId: $recordingId)")
            Logger.d(TAG, "🔴 RecentShowsService received recordingId from mediaService: $recordingId")
            recordShowPlay(showId, recordingId = recordingId)
            hasRecordedCurrentTrack = true
        }
    }

    private fun shouldRecordPlay(playbackStatus: PlaybackStatus): Boolean {
        val position = playbackStatus.currentPosition
        val duration = playbackStatus.duration

        if (duration <= 0) return false

        // Long tracks: simple 10 second rule (V2 logic)
        if (duration > 40_000L) {
            return position >= MEANINGFUL_PLAY_DURATION_MS
        }

        // Short tracks: 25% rule, capped at 10 seconds (V2 logic)
        val percentageThreshold = (duration * MEANINGFUL_PLAY_PERCENTAGE).toLong()
        val actualThreshold = minOf(percentageThreshold, MEANINGFUL_PLAY_DURATION_MS)
        return position >= actualThreshold
    }

    private fun resetTrackingState() {
        currentTrackShowId = null
        currentTrackRecordingId = null
        currentTrackStartTime = 0
        hasRecordedCurrentTrack = false
    }

    override suspend fun recordShowPlay(showId: String, playTimestamp: Long, recordingId: String?) {
        Logger.d(TAG, "📱 Recording show play: $showId at timestamp $playTimestamp (recordingId: $recordingId)")
        showService.recordShowPlay(showId, playTimestamp, recordingId)
        Logger.d(TAG, "📱 Show play recorded successfully")
    }

    override suspend fun getRecentShows(limit: Int): List<Show> {
        return showService.getRecentShows(limit)
    }

    override suspend fun isShowInRecent(showId: String): Boolean {
        return showService.isShowInRecent(showId)
    }

    override suspend fun removeShow(showId: String) {
        showService.removeRecentShow(showId)
    }

    override suspend fun clearRecentShows() {
        showService.clearAllRecentShows()
    }

    override suspend fun getRecentShowsStats(): Map<String, Any> {
        val stats = showService.getRecentShowsStats()
        return mapOf(
            "totalShows" to stats.totalShows,
            "avgPlayCount" to stats.avgPlayCount,
            "maxPlayCount" to stats.maxPlayCount,
            "oldestPlayTimestamp" to (stats.oldestPlayTimestamp ?: 0L),
            "newestPlayTimestamp" to (stats.newestPlayTimestamp ?: 0L)
        )
    }
}

private data class PlaybackInfo(
    val showId: String?,
    val recordingId: String?,
    val playbackStatus: PlaybackStatus,
    val isPlaying: Boolean
)