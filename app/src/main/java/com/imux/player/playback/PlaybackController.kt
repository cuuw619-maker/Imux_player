package com.imux.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.imux.player.data.PlayerSettings
import com.imux.player.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PlaybackController(context: Context) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val future: ListenableFuture<MediaController> = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, ImuxPlaybackService::class.java))
    ).buildAsync()

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var knownTracks: List<Track> = emptyList()

    val state = MutableStateFlow(PlaybackState())

    init {
        future.addListener({
            runCatching { future.get() }.onSuccess { ready ->
                scope.launch {
                    controller = ready
                    ready.addListener(this@PlaybackController)
                    publish()
                    startTicker()
                }
            }.onFailure { error ->
                state.value = state.value.copy(status = PlaybackStatus.Error, error = error.message)
            }
        }, MoreExecutors.directExecutor())
    }

    fun play(track: Track, queue: List<Track> = listOf(track)) {
        scope.launch {
            knownTracks = queue.distinctBy { it.uri }
            val c = awaitController() ?: return@launch
            val items = knownTracks.map(::mediaItem)
            val index = knownTracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
            c.setMediaItems(items, index, 0L)
            c.prepare()
            c.play()
            publish()
        }
    }

    fun toggle() = scope.launch {
        awaitController()?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = scope.launch { awaitController()?.seekToNextMediaItem() }
    fun previous() = scope.launch { awaitController()?.seekToPreviousMediaItem() }

    fun seekTo(positionMs: Long) = scope.launch { awaitController()?.seekTo(positionMs) }

    fun setShuffle(enabled: Boolean) = scope.launch {
        awaitController()?.setShuffleModeEnabled(enabled)
        publish()
    }

    fun cycleRepeat() = scope.launch {
        val c = awaitController() ?: return@launch
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publish()
    }

    fun setRepeat(mode: RepeatMode) = scope.launch {
        awaitController()?.repeatMode = mode.toMedia3()
        publish()
    }

    fun setSpeed(speed: Float) = scope.launch {
        awaitController()?.setPlaybackSpeed(speed)
        publish()
    }

    fun applySettings(settings: PlayerSettings) = scope.launch {
        val c = awaitController() ?: return@launch
        c.shuffleModeEnabled = settings.shuffleDefault
        c.repeatMode = when (settings.repeatDefault) {
            "ALL" -> Player.REPEAT_MODE_ALL
            "ONE" -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        c.setPlaybackSpeed(settings.playbackSpeed)
        c.skipSilenceEnabled = settings.skipSilence
        publish()
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        return runCatching { future.get() }.getOrNull()?.also { ready ->
            controller = ready
            ready.addListener(this@PlaybackController)
        }
    }

    private fun mediaItem(track: Track): MediaItem =
        MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setMediaId(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            )
            .build()

    private fun startTicker() {
        if (ticker != null) return
        ticker = scope.launch {
            while (true) {
                publish()
                delay(250)
            }
        }
    }

    private fun publish() {
        val c = controller ?: return
        val current = knownTracks.firstOrNull { it.uri == c.currentMediaItem?.mediaId }
        val status = when {
            c.playerError != null -> PlaybackStatus.Error
            c.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            c.playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
            c.isPlaying -> PlaybackStatus.Playing
            c.playbackState == Player.STATE_IDLE -> PlaybackStatus.Idle
            else -> PlaybackStatus.Paused
        }
        state.value = state.value.copy(
            status = status,
            current = current,
            queue = knownTracks,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: current?.duration ?: 0L,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.All
                Player.REPEAT_MODE_ONE -> RepeatMode.One
                else -> RepeatMode.Off
            },
            speed = c.playbackParameters.speed,
            error = c.playerError?.message
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        scope.launch { publish() }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(this)
        MediaController.releaseFuture(future)
        scope.coroutineContext[Job]?.cancel()
    }
}

private fun RepeatMode.toMedia3(): Int = when (this) {
    RepeatMode.Off -> Player.REPEAT_MODE_OFF
    RepeatMode.All -> Player.REPEAT_MODE_ALL
    RepeatMode.One -> Player.REPEAT_MODE_ONE
}
