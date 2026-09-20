package com.imux.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ImuxAnimatedPresence(
    visible: Boolean,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = if (enabled) fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.96f) else fadeIn(tween(1)),
        exit = if (enabled) fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.98f) else fadeOut(tween(1))
    ) { content() }
}

/**
 * Lightweight equalizer indicator inspired by modern music-player UIs.
 * Only three tiny bars animate, keeping the work localized to the icon.
 */
@Composable
fun ImuxPlayingEqIcon(
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    if (!playing) {
        Canvas(modifier.size(22.dp)) {
            drawEqBars(listOf(0.45f, 0.45f, 0.45f), MaterialTheme.colorScheme.primary)
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "imux-eq")
    val a by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq-a"
    )
    val b by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(680, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq-b"
    )
    val c by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(430, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq-c"
    )

    Canvas(modifier.size(22.dp)) {
        drawEqBars(listOf(a, b, c), MaterialTheme.colorScheme.primary)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEqBars(
    heights: List<Float>,
    color: androidx.compose.ui.graphics.Color
) {
    val barWidth = size.width / 7f
    heights.forEachIndexed { index, value ->
        val x = size.width * (0.18f + index * 0.30f)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x, size.height * (1f - value)),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height * value),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth, barWidth)
        )
    }
}

@Composable
fun rememberImuxRotation(enabled: Boolean): Float {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(9000)
            )
        }
    }
    return rotation.value % 360f
}
