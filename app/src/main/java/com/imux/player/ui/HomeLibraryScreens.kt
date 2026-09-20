package com.imux.player.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imux.player.ImuxApplication
import com.imux.player.data.*
import androidx.compose.ui.Alignment

enum class AppDestination { Home, Library, Settings }

@Composable
fun HomeScreen(vm: MainViewModel, app: ImuxApplication, openNowPlaying: () -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val recent = remember(tracks) { tracks.sortedByDescending { it.lastPlayed ?: 0L } }
    val added = remember(tracks) { tracks.sortedByDescending { it.addedAt } }
    val playback by vm.playbackState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().animateContentSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Imux", style = MaterialTheme.typography.displaySmall)
                Text("Your music, locally.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ImuxAnimatedPresence(visible = recent.isNotEmpty()) {
                TrackSection("Continue listening", recent.take(12), vm, app, openNowPlaying, playback.current?.uri)
            }
        }
        item {
            ImuxAnimatedPresence(visible = added.isNotEmpty()) {
                TrackSection("Recently added", added.take(12), vm, app, openNowPlaying, playback.current?.uri)
            }
        }
        item {
            ImuxAnimatedPresence(visible = tracks.any { it.favorite }) {
                TrackSection("Favorites", tracks.filter { it.favorite }.take(12), vm, app, openNowPlaying, playback.current?.uri)
            }
        }
        item {
            if (playlists.isNotEmpty()) {
                Column {
                    Text("Playlists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(10.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            AssistChip(
                                onClick = { vm.playPlaylist(playlist.id) },
                                label = { Text(playlist.name) },
                                leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackSection(title: String, tracks: List<Track>, vm: MainViewModel, app: ImuxApplication, openNowPlaying: () -> Unit, currentUri: String?) {
    if (tracks.isEmpty()) return
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(tracks, key = { it.uri }) { track ->
                Column(Modifier.width(148.dp)) {
                    Box {
                        ArtworkImage(track, app, Modifier.fillMaxWidth().aspectRatio(1f))
                        if (track.uri == currentUri) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                tonalElevation = 3.dp,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                ImuxPlayingEqIcon(
                                    playing = true,
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(track.artist.ifBlank { "Unknown artist" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { vm.play(track); openNowPlaying() }, contentPadding = PaddingValues(0.dp)) { Text("Play") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(vm: MainViewModel, app: ImuxApplication, openNowPlaying: () -> Unit) {
    val tracks by vm.tracks.collectAsState()
    val folders by vm.folders.collectAsState()
    val playlists by vm.playlists.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    val playback by vm.playbackState.collectAsState()
    val tabs = listOf("Songs", "Albums", "Artists", "Folders", "Playlists")
    val filtered = remember(tracks, query, tab) {
        val q = query.trim().lowercase()
        when (tab) {
            0 -> tracks.filter { q.isBlank() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
            1 -> tracks.filter { q.isBlank() || it.album.lowercase().contains(q) }.distinctBy { it.album.lowercase() }
            2 -> tracks.filter { q.isBlank() || it.artist.lowercase().contains(q) }.distinctBy { it.artist.lowercase() }
            else -> tracks
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") },
            actions = {
                IconButton({ vm.scan() }) { Icon(Icons.Default.Refresh, "Rescan") }
                IconButton({ selecting = !selecting; if (!selecting) selected.clear() }) {
                    Icon(if (selecting) Icons.Default.Close else Icons.Default.Checklist, "Selection")
                }
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search songs, artists, albums") },
            shape = MaterialTheme.shapes.large
        )
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
            tabs.forEachIndexed { index, title -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) }) }
        }
        when (tab) {
            3 -> FolderList(folders)
            4 -> PlaylistList(playlists, vm)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
                items(filtered, key = { it.uri }) { track ->
                    val isSelected = track.uri in selected
                    ListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (selecting) {
                                    if (isSelected) selected.remove(track.uri) else selected.add(track.uri)
                                } else {
                                    vm.play(track)
                                    openNowPlaying()
                                }
                            },
                            onLongClick = {
                                selecting = true
                                if (!isSelected) selected.add(track.uri)
                            }
                        ),
                        headlineContent = {
                            Text(
                                if (tab == 1) track.album.ifBlank { "Unknown album" }
                                else if (tab == 2) track.artist.ifBlank { "Unknown artist" }
                                else track.title
                            )
                        },
                        supportingContent = { Text(if (tab == 0) track.artist.ifBlank { "Unknown artist" } else track.title) },
                        leadingContent = {
                            Box {
                                ArtworkImage(track, app, Modifier.size(52.dp))
                                if (track.uri == playback.current?.uri) {
                                    ImuxPlayingEqIcon(
                                        playing = playback.status == com.imux.player.playback.PlaybackStatus.Playing,
                                        modifier = Modifier.padding(15.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (selecting) {
                                Checkbox(checked = isSelected, onCheckedChange = { if (it) selected.add(track.uri) else selected.remove(track.uri) })
                            } else {
                                IconButton({ vm.favorite(track) }) {
                                    Icon(if (track.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    if (selecting && selected.isNotEmpty()) {
        BottomAppBar {
            Text("\${selected.size} selected", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.weight(1f))
            IconButton({ vm.playQueue(tracks.filter { it.uri in selected }); openNowPlaying() }) { Icon(Icons.Default.PlayArrow, "Play selected") }
            IconButton({ selected.forEach { uri -> tracks.firstOrNull { it.uri == uri }?.let(vm::favorite) } }) { Icon(Icons.Default.Favorite, "Favorite selected") }
        }
    }
}

@Composable
private fun FolderList(folders: List<Folder>) {
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        items(folders, key = { it.uri }) {
            ListItem(
                headlineContent = { Text(it.name) },
                supportingContent = { Text(it.uri, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(Icons.Default.Folder, null) }
            )
        }
    }
}

@Composable
private fun PlaylistList(playlists: List<Playlist>, vm: MainViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    Column {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("New playlist") })
            IconButton({ vm.createPlaylist(name); name = "" }) { Icon(Icons.Default.Add, "Create playlist") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(playlists, key = { it.id }) {
                ListItem(
                    headlineContent = { Text(it.name) },
                    leadingContent = { Icon(Icons.Default.QueueMusic, null) },
                    trailingContent = { IconButton({ vm.deletePlaylist(it.id) }) { Icon(Icons.Default.DeleteOutline, "Delete playlist") } }
                )
            }
        }
    }
}
