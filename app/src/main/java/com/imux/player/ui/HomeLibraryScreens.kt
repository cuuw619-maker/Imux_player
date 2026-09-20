package com.imux.player.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val playback by vm.playbackState.collectAsState()
    val recent = remember(tracks) { tracks.filter { it.lastPlayed != null }.sortedByDescending { it.lastPlayed } }
    val added = remember(tracks) { tracks.sortedByDescending { it.addedAt } }
    val favorites = remember(tracks) { tracks.filter { it.favorite } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Imux", style = MaterialTheme.typography.displaySmall)
                Text(
                    if (tracks.isEmpty()) "Add your music folder to get started"
                    else tracks.size.toString() + " songs in your library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (playback.current != null) {
            item {
                ImuxTonalCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkImage(playback.current, app, Modifier.size(84.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Continue listening", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(playback.current.title, style = MaterialTheme.typography.titleLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(playback.current.artist.ifBlank { "Unknown artist" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = openNowPlaying) {
                                Icon(
                                    if (playback.status == com.imux.player.playback.PlaybackStatus.Playing)
                                        Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                    null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (playback.status == com.imux.player.playback.PlaybackStatus.Playing) "Open player" else "Resume")
                            }
                        }
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                ImuxAnimatedPresence(visible = true) {
                    TrackSection("Recently played", recent.take(12), vm, app, openNowPlaying, playback.current?.uri)
                }
            }
        }
        if (added.isNotEmpty()) {
            item {
                ImuxAnimatedPresence(visible = true) {
                    TrackSection("Recently added", added.take(12), vm, app, openNowPlaying, playback.current?.uri)
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item {
                ImuxAnimatedPresence(visible = true) {
                    TrackSection("Favorites", favorites.take(12), vm, app, openNowPlaying, playback.current?.uri)
                }
            }
        }
        if (playlists.isNotEmpty()) {
            item {
                Column {
                    ImuxSectionHeader("Playlists")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            FilterChip(
                                selected = false,
                                onClick = { vm.playPlaylist(playlist.id); openNowPlaying() },
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
private fun TrackSection(
    title: String,
    tracks: List<Track>,
    vm: MainViewModel,
    app: ImuxApplication,
    openNowPlaying: () -> Unit,
    currentUri: String?
) {
    if (tracks.isEmpty()) return
    Column {
        ImuxSectionHeader(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks, key = { it.uri }) { track ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Box {
                            ArtworkImage(track, app, Modifier.fillMaxWidth().aspectRatio(1f))
                            if (track.uri == currentUri) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                ) {
                                    ImuxPlayingEqIcon(
                                        playing = true,
                                        modifier = Modifier.padding(7.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            track.artist.ifBlank { "Unknown artist" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { vm.play(track); openNowPlaying() },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) { Text("Play") }
                    }
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
    val albumGroups = remember(tracks) {
        tracks.filter { it.album.isNotBlank() }
            .groupBy { it.album.trim().lowercase() }
            .values
            .sortedBy { it.firstOrNull()?.album?.lowercase() ?: "" }
    }
    val artistGroups = remember(tracks) {
        tracks.groupBy { it.artist.trim().ifBlank { "Unknown artist" }.lowercase() }
            .values
            .sortedBy { it.firstOrNull()?.artist?.lowercase() ?: "" }
    }
    val filtered = remember(tracks, albumGroups, artistGroups, query, tab) {
        val q = query.trim().lowercase()
        when (tab) {
            0 -> tracks.filter { q.isBlank() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
            1 -> albumGroups.filter {
                q.isBlank() || it.firstOrNull()?.album?.lowercase()?.contains(q) == true
            }.mapNotNull { it.firstOrNull() }
            2 -> artistGroups.filter {
                q.isBlank() || it.firstOrNull()?.artist?.lowercase()?.contains(q) == true
            }.mapNotNull { it.firstOrNull() }
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
            2 -> ArtistList(artistGroups, query, vm, app, playback.current?.uri, openNowPlaying)
            1 -> AlbumList(albumGroups, query, vm, app, playback.current?.uri, openNowPlaying)
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
                                    val group = when (tab) {
                                        1 -> tracks.filter { it.album.equals(track.album, ignoreCase = true) }
                                        2 -> tracks.filter { it.artist.equals(track.artist, ignoreCase = true) }
                                        else -> emptyList()
                                    }
                                    if (group.isNotEmpty()) vm.playQueue(group) else vm.play(track)
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
                        supportingContent = {
                            when (tab) {
                                0 -> Text(track.artist.ifBlank { "Unknown artist" })
                                1 -> Text(track.artist.ifBlank { "Unknown artist" })
                                2 -> Text(
                                    tracks.count { it.artist.equals(track.artist, ignoreCase = true) }.toString() + " songs"
                                )
                                else -> Text(track.title)
                            }
                        },
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
private fun ArtistList(
    groups: List<List<Track>>,
    query: String,
    vm: MainViewModel,
    app: ImuxApplication,
    currentUri: String?,
    openNowPlaying: () -> Unit
) {
    val filteredGroups = remember(groups, query) {
        val q = query.trim().lowercase()
        groups.filter {
            q.isBlank() || it.firstOrNull()?.artist?.lowercase()?.contains(q) == true
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        items(filteredGroups, key = { it.first().artist.lowercase() }) { group ->
            val first = group.first()
            ListItem(
                headlineContent = {
                    Text(first.artist.ifBlank { "Unknown artist" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(group.size.toString() + " songs")
                },
                leadingContent = {
                    ArtworkImage(first, app, Modifier.size(64.dp))
                },
                trailingContent = {
                    Row {
                        IconButton({
                            vm.playQueue(group)
                            openNowPlaying()
                        }) {
                            Icon(Icons.Default.PlayArrow, "Play artist")
                        }
                        IconButton({
                            vm.playQueue(group.shuffled())
                            openNowPlaying()
                        }) {
                            Icon(Icons.Default.Shuffle, "Shuffle artist")
                        }
                    }
                },
                modifier = Modifier.animateContentSize()
            )
        }
    }
}

@Composable
private fun AlbumList(
    groups: List<List<Track>>,
    query: String,
    vm: MainViewModel,
    app: ImuxApplication,
    currentUri: String?,
    openNowPlaying: () -> Unit
) {
    val filteredGroups = remember(groups, query) {
        val q = query.trim().lowercase()
        groups.filter {
            q.isBlank() || it.firstOrNull()?.album?.lowercase()?.contains(q) == true
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        items(filteredGroups, key = { it.first().album.lowercase() }) { group ->
            val first = group.first()
            ListItem(
                headlineContent = {
                    Text(first.album.ifBlank { "Unknown album" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text((first.artist.ifBlank { "Unknown artist" }) + " • " + group.size + " songs")
                },
                leadingContent = {
                    ArtworkImage(first, app, Modifier.size(64.dp))
                },
                trailingContent = {
                    Row {
                        IconButton({
                            vm.playQueue(group)
                            openNowPlaying()
                        }) {
                            Icon(Icons.Default.PlayArrow, "Play album")
                        }
                        IconButton({
                            vm.playQueue(group.shuffled())
                            openNowPlaying()
                        }) {
                            Icon(Icons.Default.Shuffle, "Shuffle album")
                        }
                    }
                },
                modifier = Modifier.animateContentSize()
            )
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
