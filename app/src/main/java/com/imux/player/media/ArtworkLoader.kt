package com.imux.player.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import com.imux.player.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class ArtworkData(val bitmap: ImageBitmap?, val accent: Color)

class ArtworkLoader(private val context: Context) {
    private val cache = ConcurrentHashMap<String, ArtworkData>()

    suspend fun load(track: Track, sizePx: Int = 900): ArtworkData = withContext(Dispatchers.IO) {
        cache[track.uri]?.let { return@withContext it }
        val result = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(track.uri))
            val bytes = retriever.embeddedPicture
            retriever.release()
            if (bytes == null) {
                ArtworkData(null, Color(0xFF303038))
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = calculateSample(bounds.outWidth, bounds.outHeight, sizePx)
                val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ArtworkData(bitmap?.asImageBitmap(), bitmap?.let(::accentColor) ?: Color(0xFF303038))
            }
        }.getOrElse { ArtworkData(null, Color(0xFF303038)) }
        cache[track.uri] = result
        result
    }

    fun clearMemory() = cache.clear()

    private fun calculateSample(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2
        return sample
    }

    private fun accentColor(bitmap: Bitmap): Color {
        var r = 0L; var g = 0L; var b = 0L; var count = 0L
        val step = (bitmap.width.coerceAtMost(bitmap.height) / 32).coerceAtLeast(1)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val c = bitmap.getPixel(x, y)
                r += android.graphics.Color.red(c)
                g += android.graphics.Color.green(c)
                b += android.graphics.Color.blue(c)
                count++
            }
        }
        return Color(
            ColorUtils.setAlphaComponent(
                android.graphics.Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt()),
                255
            )
        )
    }
}
