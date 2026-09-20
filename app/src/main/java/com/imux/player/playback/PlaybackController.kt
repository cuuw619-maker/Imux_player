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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the logical library queue in Imux instead of copying thousands of
 * MediaItems into Media3. Only the currently playing item is loaded into the
 * MediaController, preventing large queue mutations from blocking the UI.
 */
class PlaybackController(context: Context) : Player.Listener {
    private val appContext = context.applicationContext
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val future: ListenableFuture<MediaController> = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, ImuxPlaybackService::class.java))
    ).buildAsync()

    private var controller: MediaController? = null
    private var ticker: Job? = null

    @Volatile private var knownTracks: List<Track> = emptyList()
    @Volatile private var currentIndex = -1
    @Volatile private var shuffleEnabled = false
    @Volatile private var repeatMode = RepeatMode.Off

    val state = MutableStateFlow(PlaybackState())

    init {
        future.addListener({
            runCatching { future.get() }
                .onSuccess { ready ->
                    mainScope.launch {
                        controller = ready
                        ready.addListener(this@PlaybackController)
                        publish()
                        updateTicker()
                    }
                }
                .onFailure { error ->
                    state.value = state.value.copy(
                        status = PlaybackStatus.Error,
                        error = error.message
                    )
                }
        }, MoreExecutors.directExecutor())
    }

    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val cleanQueue = queue.distinctBy { it.uri }
        knownTracks = cleanQueue
        currentIndex = cleanQueue.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)

        mainScope.launch {
            val c = awaitController() ?: return@launch
            loadCurrent(c)
        }
    }

    fun toggle() = mainScope.launch {
        awaitController()?.let { c ->
            if (c.isPlaying) c.pause() else c.play()
            publish()
            updateTicker()
        }
    }

    fun next() = mainScope.launch {
        val c = awaitController() ?: return@launch
        if (advanceIndex(1)) {
            loadCurrent(c)
        } else {
            c.pause()
            c.seekTo(0L)
            publish()
            updateTicker()
        }
    }

    fun previous() = mainScope.launch {
        val c = awaitController() ?: return@launch
        if (c.currentPosition > 5_000L) {
            c.seekTo(0L)
            publish()
        } else if (advanceIndex(-1)) {
            loadCurrent(c)
        } else {
            c.seekTo(0L)
            publish()
        }
    }

    fun seekTo(positionMs: Long) = mainScope.launch {
        awaitController()?.let {
            it.seekTo(positionMs.coerceAtLeast(0L))
            publish()
        }
    }

    fun setShuffle(enabled: Boolean) {
        shuffleEnabled = enabled
        mainScope.launch { publish() }
    }

    fun cycleRepeat() = mainScope.launch {
        val c = awaitController() ?: return@launch
        repeatMode = when (repeatMode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        c.repeatMode = if (repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        publish()
    }

    fun setRepeat(mode: RepeatMode) = mainScope.launch {
        awaitController()?.let {
            repeatMode = mode
            it.repeatMode = if (mode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            publish()
        }
    }

    fun setSpeed(speed: Float) = mainScope.launch {
        awaitController()?.let {
            it.setPlaybackSpeed(speed.coerceIn(0.25f, 3f))
            publish()
        }
    }

    fun applySettings(settings: PlayerSettings) {
        shuffleEnabled = settings.shuffleDefault
        repeatMode = when (settings.repeatDefault) {
            "ALL" -> RepeatMode.All
            "ONE" -> RepeatMode.One
            else -> RepeatMode.Off
        }
        mainScope.launch {
            val c = awaitController() ?: return@launch
            c.repeatMode = if (repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            c.setPlaybackSpeed(settings.playbackSpeed.coerceIn(0.25f, 3f))
            publish()
        }
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        // Never call future.get() on the main thread.
        val ready = withContext(Dispatchers.IO) {
            runCatching { future.get() }.getOrNull()
        } ?: return null
        controller = ready
        ready.addListener(this@PlaybackController)
        return ready
    }

    private fun loadCurrent(c: MediaController) {
        val track = knownTracks.getOrNull(currentIndex) ?: return
        c.setMediaItem(mediaItem(track), 0L)
        c.prepare()
        c.play()
        publish()
        updateTicker()
    }

    private fun advanceIndex(direction: Int): Boolean {
        val size = knownTracks.size
        if (size == 0) return false

        if (shuffleEnabled && size > 1) {
            var candidate = currentIndex
            repeat(8) {
                val next = (0 until size).random()
                if (next != currentIndex) candidate = next
            }
            if (candidate != currentIndex) {
                currentIndex = candidate
                return true
            }
        }

        val next = currentIndex + direction
        if (next in knownTracks.indices) {
            currentIndex = next
            return true
        }

        if (repeatMode == RepeatMode.All) {
            currentIndex = if (direction > 0) 0 else size - 1
            return true
        }

        return false
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

    private fun updateTicker() {
        val c = controller
        if (c == null || !c.isPlaying) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return

        ticker = mainScope.launch {
            while (controller?.isPlaying == true) {
                publish()
                delay(500L)
            }
            ticker = null
        }
    }

    private fun publish() {
        val c = controller ?: return
        val track = knownTracks.getOrNull(currentIndex)
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
            current = track,
            queue = knownTracks,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it > 0 } ?: track?.duration ?: 0L,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            speed = c.playbackParameters.speed,
            error = c.playerError?.message
        )
    }

    override fun onEvents(player: Player, events: Player.Events) {
        mainScope.launch {
            if (player.playbackState == Player.STATE_ENDED && repeatMode == RepeatMode.All) {
                if (advanceIndex(1)) {
                    loadCurrent(player as MediaController)
                    return@launch
                }
            }
            publish()
            updateTicker()
        }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(this)
        MediaController.releaseFuture(future)
        mainScope.cancel()
    }
}

private fun RepeatMode.toMedia3(): Int = when (this) {
    RepeatMode.Off -> Player.REPEAT_MODE_OFF
    RepeatMode.All -> Player.REPEAT_MODE_ALL
    RepeatMode.One -> Player.REPEAT_MODE_ONE
}
