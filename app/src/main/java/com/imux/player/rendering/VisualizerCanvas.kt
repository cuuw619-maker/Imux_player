package com.imux.player.rendering

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun ImuxVisualizer(
    progress: Float,
    playing: Boolean,
    accent: Color,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val energy by animateFloatAsState(
        targetValue = if (playing) 1f else 0.22f,
        animationSpec = spring(stiffness = if (reducedMotion) 900f else 260f),
        label = "visualizer-energy"
    )
    Canvas(modifier.fillMaxWidth().height(56.dp)) {
        val bars = 32
        val gap = size.width / bars
        for (i in 0 until bars) {
            val wave = (sin((i * 0.72) + progress * 6.28) * 0.5 + 0.5).toFloat()
            val h = size.height * (0.12f + wave * 0.72f * energy)
            drawRoundRect(
                color = accent.copy(alpha = 0.28f + 0.45f * energy),
                topLeft = Offset(i * gap + gap * 0.25f, (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(gap * 0.5f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(gap * 0.25f)
            )
        }
    }
}
