package com.imux.player.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imux.player.ImuxApplication
import com.imux.player.playback.PlaybackState
import com.imux.player.playback.PlaybackStatus
import com.imux.player.playback.RepeatMode
import com.imux.player.rendering.ImuxVisualizer
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: MainViewModel,
    app: ImuxApplication,
    state: PlaybackState,
    reducedMotion: Boolean,
    artworkAnimations: Boolean,
    onBack: () -> Unit
) {
    var accent by remember(state.current?.uri) { mutableStateOf(MaterialTheme.colorScheme.primary) }
    var dragging by remember(state.current?.uri) { mutableStateOf(false) }
    var dragProgress by remember(state.current?.uri) { mutableFloatStateOf(0f) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.42f), MaterialTheme.colorScheme.background))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onBack) { Icon(Icons.Default.KeyboardArrowDown, "Close player") }
                Spacer(Modifier.weight(1f))
                Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                IconButton({ queueOpen = true }) { Icon(Icons.Default.QueueMusic, "Queue") }
            }
            Spacer(Modifier.height(22.dp))
            ArtworkImage(state.current, app, Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 8.dp)) { accent = it }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth()) {
                AnimatedContent(
                        targetState = state.current?.uri,
                        transitionSpec = {
                            if (artworkAnimations && !reducedMotion) {
                                (fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) togetherWith
                                    fadeOut(animationSpec = androidx.compose.animation.core.tween(160)))
                            } else EnterTransition.None togetherWith ExitTransition.None
                        },
                        label = "track-change"
                    ) {
                    Column {
                        Text(state.current?.title ?: "Nothing playing", style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.current?.artist?.ifBlank { "Unknown artist" } ?: "Choose a song", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.current?.album?.isNotBlank() == true) Text(state.current.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Slider(
                value = if (dragging) dragProgress else progress.coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    dragging = false
                    if (state.durationMs > 0) vm.seekTo((dragProgress * state.durationMs).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs))
                Text("-\${formatTime(max(0L, state.durationMs - state.positionMs))}")
            }
            if (state.status == PlaybackStatus.Playing && !reducedMotion) {
                ImuxVisualizer(progress, true, accent, reducedMotion = reducedMotion)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconButton({ vm.shuffle(!state.shuffleEnabled) }) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) accent else LocalContentColor.current) }
                IconButton(vm::previous) { Icon(Icons.Default.SkipPrevious, "Previous") }
                FilledIconButton(vm::togglePlayback, modifier = Modifier.size(72.dp)) {
                    Icon(if (state.status == PlaybackStatus.Playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", modifier = Modifier.size(34.dp))
                }
                IconButton(vm::next) { Icon(Icons.Default.SkipNext, "Next") }
                IconButton(vm::cycleRepeat) {
                    Icon(if (state.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (state.repeatMode != RepeatMode.Off) accent else LocalContentColor.current)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ state.current?.let(vm::favorite) }) { Icon(Icons.Default.Favorite, "Favorite") }
                AssistChip(onClick = { queueOpen = true }, label = { Text("Queue \${state.queue.size}") }, leadingIcon = { Icon(Icons.Default.QueueMusic, null) })
                IconButton({}) { Icon(Icons.Default.MoreVert, "More actions") }
            }
        }
    }
    if (queueOpen) {
        ModalBottomSheet(onDismissRequest = { queueOpen = false }) {
            Text("Queue", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                items(state.queue, key = { it.uri }) { track ->
                    ListItem(
                        headlineContent = { Text(track.title) },
                        supportingContent = { Text(track.artist.ifBlank { "Unknown artist" }) },
                        leadingContent = { Icon(if (track.uri == state.current?.uri) Icons.Default.VolumeUp else Icons.Default.MusicNote, null) },
                        modifier = Modifier.clickable { vm.play(track); queueOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(state: PlaybackState, app: ImuxApplication, showProgress: Boolean, onOpen: () -> Unit, vm: MainViewModel) {
    AnimatedVisibility(
        visible = state.current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(onClick = onOpen, tonalElevation = 4.dp, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column {
                if (showProgress && state.durationMs > 0) {
                    LinearProgressIndicator({ (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(3.dp))
                }
                Row(Modifier.padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    ArtworkImage(state.current, app, Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        AnimatedContent(targetState = state.current?.title.orEmpty(), label = "mini-title") { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Text(state.current?.artist?.ifBlank { "Unknown artist" }.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(vm::togglePlayback) { Icon(if (state.status == PlaybackStatus.Playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause") }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
