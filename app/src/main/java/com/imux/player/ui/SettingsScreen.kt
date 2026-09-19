package com.imux.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imux.player.data.*

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(20.dp)) }
        item {
            SettingsGroup("Playback") {
                SwitchRow("Shuffle by default", settings.shuffleDefault) { vm.updateSettings { setShuffleDefault(it) } }
                SwitchRow("Repeat all by default", settings.repeatDefault == "ALL") { vm.updateSettings { setRepeatDefault(if (it) "ALL" else "OFF") } }
                SwitchRow("Resume playback", settings.resumePlayback) { vm.updateSettings { setResume(it) } }
                SwitchRow("Autoplay", settings.autoplay) { vm.updateSettings { setAutoplay(it) } }
                SwitchRow("Skip silence", settings.skipSilence) { vm.updateSettings { setSkipSilence(it) } }
                Text("Playback speed " + "%.2f".format(settings.playbackSpeed) + "×", modifier = Modifier.padding(horizontal = 20.dp))
                Slider(value = settings.playbackSpeed, onValueChange = { vm.speed((it * 20).toInt() / 20f) }, valueRange = 0.5f..2f, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        item {
            SettingsGroup("Audio") {
                Text(
                    "Media3 audio output is active. Audio effects stay isolated from playback and UI for the dedicated effects layer.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsGroup("Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp))
                SingleChoiceRow(AppearanceMode.values().toList(), settings.appearance) { vm.updateSettings { setAppearance(it) } }
                SwitchRow("Dynamic Color", settings.dynamicColor) { vm.updateSettings { setDynamicColor(it) } }
                SwitchRow("Material expressive motion", settings.expressive) { vm.updateSettings { setExpressive(it) } }
                SingleChoiceRow(AnimationMode.values().toList(), settings.animation) { vm.updateSettings { setAnimation(it) } }
                SwitchRow("Artwork animations", settings.artworkAnimations) { vm.updateSettings { setArtworkAnimations(it) } }
                SingleChoiceRow(DensityMode.values().toList(), settings.density) { vm.updateSettings { setDensity(it) } }
            }
        }
        item {
            SettingsGroup("Interface") {
                SwitchRow("Show Mini Player", settings.showMiniPlayer) { vm.updateSettings { setMiniPlayer(it) } }
                SwitchRow("Show playback progress", settings.showPlaybackProgress) { vm.updateSettings { setPlaybackProgress(it) } }
            }
        }
        item {
            SettingsGroup("Library") {
                SwitchRow("Automatic scanning", settings.automaticScanning) { vm.updateSettings { setAutomaticScanning(it) } }
                Text("Supported formats remain controlled by the centralized format registry.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = vm::scan, modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rescan folders")
                }
            }
        }
        item { SettingsGroup("Notifications") { Text("MediaSession controls are active for background playback and the system media notification.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item { SettingsGroup("Storage") { Text("Folders are selected through Android Storage Access Framework and persist across restarts.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item { SettingsGroup("Advanced") { Text("The custom rendering layer is isolated from database and playback logic.", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) { Column(content = content) }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, trailingContent = { Switch(checked, onChange) })
}

@Composable
private fun <T> SingleChoiceRow(values: List<T>, selected: T, onSelected: (T) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value ->
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(value.toString()) })
        }
    }
}
