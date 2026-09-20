package com.imux.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun ImuxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
fun ImuxTonalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        content = content
    )
}

@Composable
fun ImuxPressableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon-press"
    )
    if (selected) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            interactionSource = source,
            content = content
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            interactionSource = source,
            content = content
        )
    }
}

@Composable
fun ImuxPlaybackControls(
    playing: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackControlSurface(onClick = onPrevious, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(30.dp))
        }
        val playShape by animateDpAsState(
            targetValue = if (playing) 34.dp else 24.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "play-shape"
        )
        val playScale by animateFloatAsState(
            targetValue = if (playing) 1.02f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "play-scale"
        )
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.weight(1.25f).size(76.dp).scale(playScale),
            shape = RoundedCornerShape(playShape),
            content = {
                Crossfade(
                    targetState = playing,
                    animationSpec = androidx.compose.animation.core.tween(180),
                    label = "play-icon"
                ) { isPlaying ->
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        )
        PlaybackControlSurface(onClick = onNext, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun PlaybackControlSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = CircleShape,
        content = content
    )
}
