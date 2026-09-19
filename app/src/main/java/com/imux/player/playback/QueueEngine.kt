package com.imux.player.playback

import com.imux.player.data.Track

object QueueEngine {
    fun indexOf(queue: List<Track>, uri: String?): Int =
        queue.indexOfFirst { it.uri == uri }.coerceAtLeast(0)

    fun remove(queue: List<Track>, uri: String): List<Track> =
        queue.filterNot { it.uri == uri }

    fun move(queue: List<Track>, from: Int, to: Int): List<Track> {
        if (from !in queue.indices || to !in queue.indices || from == to) return queue
        return queue.toMutableList().apply { add(to, removeAt(from)) }
    }
}
