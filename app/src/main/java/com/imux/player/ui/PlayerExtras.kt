package com.imux.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imux.player.ImuxApplication
import com.imux.player.data.Track

@Composable
fun PlayerArtistSheet(
    artist: String,
    tracks: List<Track>,
    vm: MainViewModel,
    app: ImuxApplication,
    onDismiss: () -> Unit
) {
    val artistName = artist.ifBlank { "Unknown artist" }
    val artistTracks = remember(tracks, artistName) {
        tracks.filter { it.artist.equals(artistName, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text(artistName, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${artistTracks.size} songs",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        vm.playQueue(artistTracks)
                        onDismiss()
                    },
                    label = { Text("Play artist") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                )
                AssistChip(
                    onClick = {
                        vm.playQueue(artistTracks.shuffled())
                        onDismiss()
                    },
                    label = { Text("Shuffle") },
                    leadingIcon = { Icon(Icons.Default.Shuffle, null) }
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(artistTracks, key = { it.uri }) { track ->
                    ListItem(
                        headlineContent = { Text(track.title, maxLines = 1) },
                        supportingContent = { Text(track.album.ifBlank { "Unknown album" }, maxLines = 1) },
                        leadingContent = {
                            ArtworkImage(track, app, Modifier.size(52.dp))
                        },
                        modifier = Modifier.clickable {
                            vm.playQueue(artistTracks.dropWhile { it.uri != track.uri })
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedChip(
    speed: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val label = remember(speed) {
        val rounded = (speed * 100).toInt() / 100f
        if (rounded == rounded.toInt().toFloat()) "${rounded.toInt()}x" else "${rounded}x"
    }
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.MusicNote, null) }
    )
}
