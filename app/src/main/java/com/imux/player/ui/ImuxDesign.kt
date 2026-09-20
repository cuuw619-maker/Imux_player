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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
    content: @Composable () -> Unit
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
        FilledTonalIconToggleButton(
            checked = true,
            onCheckedChange = { onClick() },
            modifier = modifier.scale(scale),
            shapes = IconButtonDefaults.toggleableShapes(),
            interactionSource = source,
            content = content
        )
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            shapes = IconButtonDefaults.shapes(),
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
        modifier = modifier.fillMaxWidth().height(88.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImuxPressableIconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) {
            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(30.dp))
        }
        val playScale by animateFloatAsState(
            targetValue = if (playing) 1.04f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "primary-play-scale"
        )
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.weight(1f).height(76.dp).scale(playScale),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Crossfade(
                targetState = playing,
                animationSpec = androidx.compose.animation.core.tween(180),
                label = "play-icon"
            ) { isPlaying ->
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(38.dp)
                )
            }
        }
        ImuxPressableIconButton(onClick = onNext, modifier = Modifier.size(58.dp)) {
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
