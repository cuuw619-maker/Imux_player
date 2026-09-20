package com.imux.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imux.player.ImuxApplication
import com.imux.player.data.Track
import com.imux.player.playback.*
import com.imux.player.rendering.ImuxVisualizer
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    val primary = MaterialTheme.colorScheme.primary
    var accent by remember { mutableStateOf(primary) }
    LaunchedEffect(primary) { accent = primary }
    var dragging by remember(state.current?.uri) { mutableStateOf(false) }
    var dragProgress by remember(state.current?.uri) { mutableFloatStateOf(0f) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    var artistOpen by rememberSaveable { mutableStateOf(false) }
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f
    val motion = artworkAnimations && !reducedMotion

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.28f), MaterialTheme.colorScheme.background)
            )
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Now playing", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close player")
                    }
                },
                actions = {
                    IconButton(onClick = { queueOpen = true }) {
                        Icon(Icons.Default.QueueMusic, "Queue")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )

            Column(
                Modifier.fillMaxSize().weight(1f).verticalScroll(rememberScrollState())
                    .navigationBarsPadding().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    val artSize = maxWidth.coerceAtMost(390.dp)
                    val artScale by animateFloatAsState(
                        if (state.status == PlaybackStatus.Playing && motion) 1.018f else 1f,
                        spring(dampingRatio = 0.82f, stiffness = 280f),
                        label = "art-scale"
                    )
                    val haloRotation = rememberImuxRotation(motion && state.status == PlaybackStatus.Playing)
                    val artLift by animateFloatAsState(
                        if (state.status == PlaybackStatus.Playing && motion) 1.025f else 1f,
                        spring(dampingRatio = 0.78f, stiffness = 180f),
                        label = "art-lift"
                    )
                    Box(
                        Modifier.size(artSize).align(Alignment.Center)
                            .graphicsLayer {
                                translationX = swipeOffset.value
                                alpha = 1f - (abs(swipeOffset.value) / 700f).coerceIn(0f, 0.18f)
                            }
                            .pointerInput(state.current?.uri, reducedMotion) {
                                if (reducedMotion) return@pointerInput
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, amount ->
                                        change.consume()
                                        scope.launch {
                                            swipeOffset.snapTo(
                                                (swipeOffset.value + amount * 0.72f).coerceIn(-180f, 180f)
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        val distance = swipeOffset.value
                                        if (distance < -110f) vm.next()
                                        if (distance > 110f) vm.previous()
                                        scope.launch {
                                            swipeOffset.animateTo(
                                                0f,
                                                spring(dampingRatio = 0.82f, stiffness = 420f)
                                            )
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                Modifier.fillMaxSize(0.98f)
                                    .graphicsLayer {
                                        rotationZ = haloRotation
                                        scaleX = artLift
                                        scaleY = artLift
                                    }
                                    .border(
                                        2.dp,
                                        Brush.sweepGradient(
                                            listOf(
                                                accent.copy(alpha = 0.05f),
                                                accent.copy(alpha = 0.72f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                accent.copy(alpha = 0.05f)
                                            )
                                        ),
                                        RoundedCornerShape(36.dp)
                                    ),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(36.dp)
                            ) {}
                            Surface(
                                Modifier.fillMaxSize().scale(artScale),
                                shape = RoundedCornerShape(32.dp),
                                tonalElevation = 10.dp,
                                shadowElevation = 14.dp
                            ) {
                            ArtworkImage(
                                state.current,
                                app,
                                Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp))
                                    .pointerInput(state.current?.uri) {
                                        detectTapGestures(onDoubleTap = { vm.togglePlayback() })
                                    }
                            ) { extracted -> accent = extracted }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                AnimatedContent(
                    targetState = state.current,
                    transitionSpec = {
                        if (motion) {
                            (fadeIn(tween(220)) + scaleIn(0.97f, tween(240))) togetherWith
                                (fadeOut(tween(150)) + scaleOut(0.98f, tween(160)))
                        } else EnterTransition.None togetherWith ExitTransition.None
                    },
                    label = "track-details"
                ) { track ->
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            track?.title ?: "Nothing playing",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        TextButton(
                            onClick = { if (track != null) artistOpen = true },
                            enabled = track != null
                        ) {
                            Text(
                                track?.artist?.ifBlank { "Unknown artist" } ?: "Choose a song",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (!track?.album.isNullOrBlank()) {
                            Text(
                                track?.album.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Slider(
                    value = if (dragging) dragProgress else progress,
                    onValueChange = { dragging = true; dragProgress = it },
                    onValueChangeFinished = {
                        dragging = false
                        if (state.durationMs > 0) vm.seekTo((dragProgress * state.durationMs).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("-"+formatTime(max(0L, state.durationMs - state.positionMs)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (state.status == PlaybackStatus.Playing && !reducedMotion) {
                    Spacer(Modifier.height(8.dp))
                    ImuxVisualizer(progress, true, accent, reducedMotion = reducedMotion)
                }

                Spacer(Modifier.height(12.dp))
                ImuxPlaybackControls(
                    playing = state.status == PlaybackStatus.Playing,
                    onPrevious = vm::previous,
                    onPlayPause = vm::togglePlayback,
                    onNext = vm::next
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImuxPressableIconButton(
                        onClick = { state.current?.let(vm::favorite) },
                        selected = state.current?.favorite == true
                    ) {
                        Icon(
                            if (state.current?.favorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite"
                        )
                    }
                    ImuxPressableIconButton(
                        onClick = { vm.shuffle(!state.shuffleEnabled) },
                        selected = state.shuffleEnabled
                    ) { Icon(Icons.Default.Shuffle, "Shuffle") }
                    AssistChip(
                        onClick = { queueOpen = true },
                        label = { Text("Queue "+state.queue.size) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, null) }
                    )
                    ImuxPressableIconButton(
                        onClick = vm::cycleRepeat,
                        selected = state.repeatMode != RepeatMode.Off
                    ) {
                        Icon(
                            if (state.repeatMode == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "Repeat"
                        )
                    }
                    ImuxPressableIconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (artistOpen && state.current != null) {
        PlayerArtistSheet(
            artist = state.current.artist,
            tracks = state.queue,
            vm = vm,
            app = app,
            onDismiss = { artistOpen = false }
        )
    }
    if (queueOpen) {
        ModalBottomSheet(onDismissRequest = { queueOpen = false }) {
            Text(
                "Queue",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                items(state.queue, key = { it.uri }) { track ->
                    Surface(
                        onClick = { vm.play(track); queueOpen = false },
                        color = if (track.uri == state.current?.uri)
                            MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (track.uri == state.current?.uri) {
                                ImuxPlayingEqIcon(
                                    state.status == PlaybackStatus.Playing,
                                    Modifier.size(28.dp)
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, null, Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    track.artist.ifBlank { "Unknown artist" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                formatTime(track.duration),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    state: PlaybackState,
    app: ImuxApplication,
    showProgress: Boolean,
    onOpen: () -> Unit,
    vm: MainViewModel,
    animationsEnabled: Boolean = true
) {
    AnimatedContent(
        targetState = state.current != null,
        transitionSpec = {
            if (animationsEnabled) fadeIn(tween(180)) togetherWith fadeOut(tween(120))
            else EnterTransition.None togetherWith ExitTransition.None
        },
        label = "mini-visibility"
    ) { visible ->
        if (!visible) return@AnimatedContent
        val progress = if (state.durationMs > 0)
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
        Surface(
            onClick = onOpen,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                .pointerInput(state.current?.uri) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            totalDrag += amount
                        },
                        onDragEnd = {
                            if (abs(totalDrag) >= 100f) {
                                if (totalDrag < 0f) vm.next() else vm.previous()
                            }
                        }
                    )
                }
        ) {
            Column {
                if (showProgress && state.durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                    )
                }
                Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtworkImage(state.current, app, Modifier.size(54.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.current?.title.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            state.current?.artist?.ifBlank { "Unknown artist" }.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    FilledTonalIconButton(
                        onClick = vm::togglePlayback,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (state.status == PlaybackStatus.Playing) Icons.Default.Pause
                            else Icons.Default.PlayArrow,
                            "Play or pause"
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
