package com.imux.player.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-local bridge between Android SystemUI media notification commands
 * and the logical Imux playback queue.
 */
object PlaybackCommandBus {
    const val NEXT = "next"
    const val PREVIOUS = "previous"
    const val SHUFFLE = "shuffle"
    const val REPEAT = "repeat"

    private val _commands = MutableSharedFlow<String>(
        extraBufferCapacity = 16
    )
    val commands = _commands.asSharedFlow()

    fun send(command: String) {
        _commands.tryEmit(command)
    }
}
