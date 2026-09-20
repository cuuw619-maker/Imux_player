package com.imux.player.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Imux UI Engine
 *
 * Owns the geometry and motion policy used by the player instead of scattering
 * device-specific magic numbers through individual screens. Compose/Material3
 * remains the rendering backend; this layer decides the stable visual geometry.
 */
@Immutable
data class ImuxViewport(
    val width: Dp,
    val height: Dp,
    val landscape: Boolean,
    val compactWidth: Boolean,
    val compactHeight: Boolean
) {
    val sidePadding: Dp
        get() = when {
            width < 360.dp -> 14.dp
            width < 600.dp -> 20.dp
            else -> 32.dp
        }

    val contentWidth: Dp
        get() = min(width.value, 720f).dp

    val artworkSize: Dp
        get() {
            val availableWidth = (contentWidth - sidePadding * 2).coerceAtLeast(220.dp)
            val availableHeight = if (landscape) {
                (height - 118.dp).coerceAtLeast(220.dp)
            } else {
                (height * 0.40f).coerceAtLeast(240.dp)
            }
            return min(min(availableWidth.value, availableHeight.value), 430f).dp
        }

    val controlGap: Dp
        get() = if (compactWidth) 10.dp else 16.dp

    val sectionGap: Dp
        get() = if (compactHeight) 8.dp else 14.dp
}

object ImuxMotionEngine {
    fun <T> springSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> responsiveSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun fadeInSpec() = tween<Float>(190)
    fun fadeOutSpec() = tween<Float>(130)
    fun contentSpec() = tween<Float>(220)
}

fun imuxViewport(width: Dp, height: Dp): ImuxViewport {
    return ImuxViewport(
        width = width,
        height = height,
        landscape = width > height,
        compactWidth = width < 360.dp,
        compactHeight = height < 640.dp
    )
}
