package com.imux.player
import com.imux.player.media.AudioFormatCatalog
import org.junit.Assert.*
import org.junit.Test
class AudioFormatCatalogTest{
 @Test fun commonFormatsAreRegistered(){val extensions=AudioFormatCatalog.formats.map{it.extension};assertTrue("mp3" in extensions);assertTrue("flac" in extensions);assertTrue("opus" in extensions);assertEquals(extensions.size,extensions.distinct().size)}
 @Test fun mimeTypesArePresent(){assertTrue(AudioFormatCatalog.formats.all{it.mime.isNotBlank()})}
}