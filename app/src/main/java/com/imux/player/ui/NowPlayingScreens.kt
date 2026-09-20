package com.imux.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.imux.player.playback.*
import com.imux.player.rendering.ImuxVisualizer
import kotlinx.coroutines.CoroutineScope
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
    val motion = artworkAnimations && !reducedMotion

    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Text("Now playing", style = MaterialTheme.typography.titleMedium)
                },
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
                )
            )

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val viewport = imuxViewport(maxWidth, maxHeight)

                if (viewport.landscape) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = viewport.sidePadding, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .weight(0.48f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            NowPlayingArtwork(
                                state = state,
                                app = app,
                                accent = accent,
                                artworkSize = viewport.artworkSize,
                                motion = motion,
                                reducedMotion = reducedMotion,
                                swipeOffset = swipeOffset,
                                scope = scope,
                                onNext = vm::next,
                                onPrevious = vm::previous,
                                onToggle = vm::togglePlayback,
                                onAccent = { accent = it }
                            )
                        }

                        PlayerDetailsAndControls(
                            state = state,
                            vm = vm,
                            progress = progress,
                            dragging = dragging,
                            dragProgress = dragProgress,
                            reducedMotion = reducedMotion,
                            motion = motion,
                            queueOpen = { queueOpen = true },
                            artistOpen = { artistOpen = true },
                            onDragging = { dragging = it },
                            onDragProgress = { dragProgress = it },
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = viewport.sidePadding)
                    ) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            NowPlayingArtwork(
                                state = state,
                                app = app,
                                accent = accent,
                                artworkSize = viewport.artworkSize,
                                motion = motion,
                                reducedMotion = reducedMotion,
                                swipeOffset = swipeOffset,
                                scope = scope,
                                onNext = vm::next,
                                onPrevious = vm::previous,
                                onToggle = vm::togglePlayback,
                                onAccent = { accent = it }
                            )
                        }
                        Spacer(Modifier.height(viewport.sectionGap))
                        PlayerDetailsAndControls(
                            state = state,
                            vm = vm,
                            progress = progress,
                            dragging = dragging,
                            dragProgress = dragProgress,
                            reducedMotion = reducedMotion,
                            motion = motion,
                            queueOpen = { queueOpen = true },
                            artistOpen = { artistOpen = true },
                            onDragging = { dragging = it },
                            onDragProgress = { dragProgress = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
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
                        onClick = {
                            vm.play(track)
                            queueOpen = false
                        },
                        color = if (track.uri == state.current?.uri) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
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
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
private fun NowPlayingArtwork(
    state: PlaybackState,
    app: ImuxApplication,
    accent: Color,
    artworkSize: androidx.compose.ui.unit.Dp,
    motion: Boolean,
    reducedMotion: Boolean,
    swipeOffset: Animatable<Float, *>,
    scope: CoroutineScope,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onAccent: (Color) -> Unit
) {
    val playing = state.status == PlaybackStatus.Playing
    val artScale by animateFloatAsState(
        targetValue = if (playing && motion) 1.012f else 1f,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 420f),
        label = "art-scale"
    )
    val haloRotation = rememberImuxRotation(playing && motion)

    Box(
        Modifier
            .size(artworkSize)
            .graphicsLayer {
                translationX = swipeOffset.value
                alpha = 1f - (abs(swipeOffset.value) / 520f).coerceIn(0f, 0.22f)
            }
            .pointerInput(state.current?.uri, reducedMotion) {
                if (reducedMotion) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        scope.launch {
                            swipeOffset.snapTo(
                                (swipeOffset.value + amount * 0.72f)
                                    .coerceIn(-artworkSize.value * 0.48f, artworkSize.value * 0.48f)
                            )
                        }
                    },
                    onDragEnd = {
                        val distance = swipeOffset.value
                        when {
                            distance < -artworkSize.value * 0.28f -> onNext()
                            distance > artworkSize.value * 0.28f -> onPrevious()
                        }
                        scope.launch {
                            swipeOffset.animateTo(
                                0f,
                                spring(dampingRatio = 0.86f, stiffness = 500f)
                            )
                        }
                    }
                )
            }
    ) {
        Surface(
            Modifier
                .fillMaxSize(0.985f)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = haloRotation }
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            accent.copy(alpha = 0.08f),
                            accent.copy(alpha = 0.58f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            accent.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(30.dp)
        ) {}

        AnimatedContent(
            targetState = state.current?.uri,
            transitionSpec = {
                if (motion) {
                    (
                        slideInHorizontally(
                            initialOffsetX = { it / 7 },
                            animationSpec = tween(230)
                        ) + fadeIn(tween(190))
                    ) togetherWith (
                        slideOutHorizontally(
                            targetOffsetX = { -it / 10 },
                            animationSpec = tween(170)
                        ) + fadeOut(tween(130))
                    )
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "artwork-change",
            modifier = Modifier.fillMaxSize().scale(artScale)
        ) {
            Surface(
                Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                ArtworkImage(
                    state.current,
                    app,
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .pointerInput(state.current?.uri) {
                            detectTapGestures(onDoubleTap = { onToggle() })
                        }
                ) { extracted ->
                    onAccent(extracted)
                }
            }
        }
    }
}

@Composable
private fun PlayerDetailsAndControls(
    state: PlaybackState,
    vm: MainViewModel,
    progress: Float,
    dragging: Boolean,
    dragProgress: Float,
    reducedMotion: Boolean,
    motion: Boolean,
    queueOpen: () -> Unit,
    artistOpen: () -> Unit,
    onDragging: (Boolean) -> Unit,
    onDragProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = state.current,
            transitionSpec = {
                if (motion) {
                    (
                        slideInHorizontally(
                            initialOffsetX = { it / 8 },
                            animationSpec = tween(210)
                        ) + fadeIn(tween(180))
                    ) togetherWith (
                        slideOutHorizontally(
                            targetOffsetX = { -it / 12 },
                            animationSpec = tween(150)
                        ) + fadeOut(tween(120))
                    )
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "track-info",
            modifier = Modifier.fillMaxWidth()
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
                    onClick = artistOpen,
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

        Spacer(Modifier.height(12.dp))

        Slider(
            value = if (dragging) dragProgress else progress,
            onValueChange = {
                onDragging(true)
                onDragProgress(it)
            },
            onValueChangeFinished = {
                onDragging(false)
                if (state.durationMs > 0L) {
                    vm.seekTo((dragProgress * state.durationMs).toLong())
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(state.positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "-\${formatTime(max(0L, state.durationMs - state.positionMs))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.status == PlaybackStatus.Playing && !reducedMotion) {
            Spacer(Modifier.height(5.dp))
            ImuxVisualizer(
                progress,
                true,
                MaterialTheme.colorScheme.primary,
                reducedMotion = reducedMotion
            )
        }

        Spacer(Modifier.height(8.dp))

        ImuxPlaybackControls(
            playing = state.status == PlaybackStatus.Playing,
            onPrevious = vm::previous,
            onPlayPause = vm::togglePlayback,
            onNext = vm::next,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImuxPressableIconButton(
                onClick = { state.current?.let(vm::favorite) },
                selected = state.current?.favorite == true,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    if (state.current?.favorite == true) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    "Favorite"
                )
            }

            ImuxPressableIconButton(
                onClick = { vm.shuffle(!state.shuffleEnabled) },
                selected = state.shuffleEnabled,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Shuffle, "Shuffle")
            }

            AssistChip(
                onClick = queueOpen,
                label = { Text("Queue \${state.queue.size}") },
                leadingIcon = { Icon(Icons.Default.QueueMusic, null) }
            )

            ImuxPressableIconButton(
                onClick = vm::cycleRepeat,
                selected = state.repeatMode != RepeatMode.Off,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    if (state.repeatMode == RepeatMode.One) Icons.Default.RepeatOne
                    else Icons.Default.Repeat,
                    "Repeat"
                )
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
            if (animationsEnabled) {
                fadeIn(tween(180)) togetherWith fadeOut(tween(120))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "mini-visibility"
    ) { visible ->
        if (!visible) return@AnimatedContent

        val progress = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else 0f

        Surface(
            onClick = onOpen,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
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
                if (showProgress && state.durationMs > 0L) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                    )
                }
                Row(
                    Modifier.padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(state.current, app, Modifier.size(52.dp))
                    Spacer(Modifier.width(10.dp))
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
                        modifier = Modifier.size(46.dp)
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
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
