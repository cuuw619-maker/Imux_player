package com.imux.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imux.player.data.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: MainViewModel) {
    val tracks by vm.tracks.collectAsState()
    val current by vm.current.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Imux Player") }) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (current != null) {
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(current!!.title) },
                        supportingContent = { Text("Now playing") },
                        trailingContent = {
                            IconButton(vm::togglePlayback) {
                                Icon(Icons.Default.PlayArrow, "Play or pause")
                            }
                        }
                    )
                }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Songs", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.weight(1f))
                        IconButton(vm::scan) {
                            Icon(Icons.Default.Refresh, "Rescan")
                        }
                    }
                }
                items(tracks, key = { it.uri }) {
                    TrackRow(it, vm)
                }
            }
        }
    }
}

@Composable
private fun TrackRow(t: Track, vm: MainViewModel) {
    ListItem(
        headlineContent = { Text(t.title) },
        supportingContent = { Text(t.artist.ifBlank { "Unknown artist" }) },
        leadingContent = {
            IconButton({ vm.play(t) }) {
                Icon(Icons.Default.PlayArrow, "Play")
            }
        },
        trailingContent = {
            IconButton({ vm.favorite(t) }) {
                Icon(
                    if (t.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite"
                )
            }
        }
    )
}
