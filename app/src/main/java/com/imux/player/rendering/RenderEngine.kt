package com.imux.player.rendering

import androidx.compose.ui.graphics.Color

data class RenderState(
    val accent: Color = Color(0xFF6750A4),
    val progress: Float = 0f,
    val playing: Boolean = false,
    val reducedMotion: Boolean = false,
    val visible: Boolean = true
)

interface RenderSurface {
    fun update(state: RenderState)
    fun dispose()
}

class AnimationRenderer {
    fun transitionFraction(reducedMotion: Boolean): Float = if (reducedMotion) 1f else 0f
}

class ArtworkRenderer : RenderSurface {
    private var state = RenderState()
    override fun update(state: RenderState) { this.state = state }
    override fun dispose() { }
}

class VisualizerRenderer : RenderSurface {
    private var state = RenderState()
    override fun update(state: RenderState) { this.state = state }
    override fun dispose() { }
}

class RenderEngine {
    private val surfaces = mutableListOf<RenderSurface>()
    fun attach(surface: RenderSurface) { surfaces += surface }
    fun render(state: RenderState) { surfaces.forEach { it.update(state) } }
    fun dispose() { surfaces.forEach(RenderSurface::dispose); surfaces.clear() }
}
