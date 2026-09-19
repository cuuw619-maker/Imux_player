package com.imux.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.imux.player.ImuxApplication
import com.imux.player.data.Track
import com.imux.player.media.ArtworkData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArtworkImage(
    track: Track?,
    app: ImuxApplication,
    modifier: Modifier = Modifier,
    onAccent: (Color) -> Unit = {}
) {
    val data by produceState<ArtworkData?>(null, track?.uri) {
        value = if (track == null) null else withContext(Dispatchers.IO) { app.artwork.load(track) }
    }
    LaunchedEffect(data?.accent) { data?.accent?.let(onAccent) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(data?.accent?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val bitmap: ImageBitmap? = data?.bitmap
        if (bitmap != null) {
            Image(bitmap, contentDescription = track?.title, modifier = Modifier.aspectRatio(1f))
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
