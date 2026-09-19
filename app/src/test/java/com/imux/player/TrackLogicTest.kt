package com.imux.player
import com.imux.player.data.Track
import org.junit.Assert.*
import org.junit.Test
class TrackLogicTest{
 @Test fun titleSortIsStable(){val a=Track("a","Alpha");val b=Track("b","beta");assertEquals(listOf(a,b),listOf(b,a).sortedBy{it.title.lowercase()})}
 @Test fun favoriteDefaultsToFalse(){assertFalse(Track("x","Song").favorite)}
}