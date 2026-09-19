package com.imux.player

import com.imux.player.data.Track
import com.imux.player.playback.QueueEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueEngineTest {
    private val tracks = listOf(Track("a", "A"), Track("b", "B"), Track("c", "C"))

    @Test fun moveReordersWithoutLosingItems() {
        assertEquals(listOf("b", "c", "a"), QueueEngine.move(tracks, 0, 2).map { it.uri })
    }

    @Test fun removeDropsOnlyRequestedTrack() {
        assertEquals(listOf("a", "c"), QueueEngine.remove(tracks, "b").map { it.uri })
    }
}
