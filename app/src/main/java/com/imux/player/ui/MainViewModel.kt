package com.imux.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.imux.player.ImuxApplication
import com.imux.player.data.*
import com.imux.player.playback.PlaybackController
import com.imux.player.playback.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyList
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(private val app: ImuxApplication) : ViewModel() {
    private val playback = PlaybackController(app)

    val tracks = app.library.tracks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders = app.library.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val onboarding = app.settings.onboarding.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerSettings())
    val playbackState = playback.state

    init {
        viewModelScope.launch {
            playback.applySettings(app.settings.settings.first())
        }
    }

    fun addFolder(uri: String, name: String) = viewModelScope.launch {
        app.library.addFolder(uri, name)
        app.settings.done()
    }

    fun scan() = viewModelScope.launch { app.library.scanAll() }

    fun favorite(track: Track) = viewModelScope.launch {
        app.library.favorite(track.uri, !track.favorite)
    }

    fun play(track: Track) = playback.play(track, tracks.value).also {
        viewModelScope.launch { app.library.played(track.uri) }
    }

    fun togglePlayback() = playback.toggle()
    fun next() = playback.next()
    fun previous() = playback.previous()
    fun seekTo(position: Long) = playback.seekTo(position)
    fun shuffle(enabled: Boolean) = playback.setShuffle(enabled)
    fun cycleRepeat() = playback.cycleRepeat()
    fun speed(value: Float) = viewModelScope.launch {
        app.settings.setSpeed(value)
        playback.setSpeed(value)
    }

    fun updateSettings(action: suspend SettingsRepository.() -> Unit) =
        viewModelScope.launch { app.settings.action() }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }

    companion object {
        fun factory(app: ImuxApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(app) as T
        }
    }
}
