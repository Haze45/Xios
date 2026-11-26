package com.yourapp.music.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.yourapp.music.MainActivity
import com.yourapp.music.domain.repository.SongRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var listeningHistoryTracker: ListeningHistoryTracker

    private var mediaSession: MediaSession? = null

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var progressJob: Job? = null

    private val historyPlayerListener =
        object : Player.Listener {

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                val songId = mediaItem?.mediaId ?: return

                serviceScope.launch {
                    val song = withContext(Dispatchers.IO) {
                        songRepository.getSongById(songId)
                    }

                    if (song != null) {
                        listeningHistoryTracker.onSongTransition(
                            song = song,
                            transitionReason = reason,
                            isPlaying = exoPlayer.isPlaying,
                            durationMs = exoPlayer.duration.coerceAtLeast(
                                0L
                            )
                        )
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listeningHistoryTracker.onPlaybackChanged(
                    isPlaying = isPlaying,
                    positionMs = exoPlayer.currentPosition,
                    durationMs = exoPlayer.duration.coerceAtLeast(
                        0L
                    )
                )

                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_ENDED) {
                    listeningHistoryTracker.onPlaybackEnded()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        val playerActivityIntent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            putExtra(
                MainActivity.EXTRA_OPEN_PLAYER,
                true
            )

            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val sessionActivityPendingIntent =
            PendingIntent.getActivity(
                this,
                1001,
                playerActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        mediaSession = MediaSession.Builder(
            this,
            exoPlayer
        )
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        exoPlayer.addListener(historyPlayerListener)
    }

    private fun startProgressTracking() {
        if (progressJob?.isActive == true) return

        progressJob = serviceScope.launch {
            while (exoPlayer.isPlaying) {
                delay(15_000L.milliseconds)

                if (!exoPlayer.isPlaying) break

                listeningHistoryTracker.onProgress(
                    positionMs = exoPlayer.currentPosition,
                    durationMs = exoPlayer.duration.coerceAtLeast(
                        0L
                    )
                )
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        stopProgressTracking()

        listeningHistoryTracker.onServiceStopped(
            positionMs = exoPlayer.currentPosition,
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
        )

        exoPlayer.removeListener(historyPlayerListener)

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        serviceScope.cancel()

        super.onDestroy()
    }
}
