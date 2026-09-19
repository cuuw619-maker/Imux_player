package com.imux.player.playback

import com.imux.player.data.Track

enum class PlaybackStatus { Idle, Loading, Playing, Paused, Buffering, Ended, Error }
enum class RepeatMode { Off, All, One }

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val current: Track? = null,
    val queue: List<Track> = emptyList(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val speed: Float = 1f,
    val error: String? = null
)
