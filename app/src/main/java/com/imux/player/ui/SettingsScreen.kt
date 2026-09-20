package com.imux.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imux.player.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text("Settings") }
            )
        }
        item {
            SettingsSectionCard("Playback") {
                SwitchRow("Shuffle by default", settings.shuffleDefault) {
                    vm.updateSettings { setShuffleDefault(it) }
                }
                SwitchRow("Repeat all by default", settings.repeatDefault == "ALL") {
                    vm.updateSettings { setRepeatDefault(if (it) "ALL" else "OFF") }
                }
                SwitchRow("Resume playback", settings.resumePlayback) {
                    vm.updateSettings { setResume(it) }
                }
                SwitchRow("Autoplay", settings.autoplay) {
                    vm.updateSettings { setAutoplay(it) }
                }
                SwitchRow("Skip silence", settings.skipSilence) {
                    vm.updateSettings { setSkipSilence(it) }
                }
                Text(
                    "Playback speed · %.2f×".format(settings.playbackSpeed),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                Slider(
                    value = settings.playbackSpeed,
                    onValueChange = { vm.speed((it * 20).toInt() / 20f) },
                    valueRange = 0.5f..2f,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        item {
            SettingsSectionCard("Appearance") {
                ChoiceLabel("Theme")
                ChoiceChips(
                    values = AppearanceMode.values().toList(),
                    selected = settings.appearance,
                    onSelected = { vm.updateSettings { setAppearance(it) } }
                )
                SwitchRow("Dynamic color", settings.dynamicColor) {
                    vm.updateSettings { setDynamicColor(it) }
                }
                SwitchRow("Expressive Material motion", settings.expressive) {
                    vm.updateSettings { setExpressive(it) }
                }
                ChoiceLabel("Motion")
                ChoiceChips(
                    values = AnimationMode.values().toList(),
                    selected = settings.animation,
                    onSelected = { vm.updateSettings { setAnimation(it) } }
                )
                SwitchRow("Artwork animations", settings.artworkAnimations) {
                    vm.updateSettings { setArtworkAnimations(it) }
                }
                ChoiceLabel("Density")
                ChoiceChips(
                    values = DensityMode.values().toList(),
                    selected = settings.density,
                    onSelected = { vm.updateSettings { setDensity(it) } }
                )
            }
        }
        item {
            SettingsSectionCard("Interface") {
                SwitchRow("Show mini player", settings.showMiniPlayer) {
                    vm.updateSettings { setMiniPlayer(it) }
                }
                SwitchRow("Show playback progress", settings.showPlaybackProgress) {
                    vm.updateSettings { setPlaybackProgress(it) }
                }
            }
        }
        item {
            SettingsSectionCard("Library") {
                SwitchRow("Automatic scanning", settings.automaticScanning) {
                    vm.updateSettings { setAutomaticScanning(it) }
                }
                InfoRow("Supported formats are controlled by the centralized format registry.")
                FilledTonalButton(
                    onClick = vm::scan,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rescan folders")
                }
            }
        }
        item {
            SettingsSectionCard("Playback service") {
                InfoRow("MediaSession provides background playback, lock-screen controls and the system media notification.")
            }
        }
        item {
            SettingsSectionCard("Storage") {
                InfoRow("Music folders use Android Storage Access Framework and remain available after restarting the app.")
            }
        }
        item {
            SettingsSectionCard("Rendering") {
                InfoRow("Artwork, visualizer and UI animation are isolated from database and playback work.")
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).animateContentSize()
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable
private fun ChoiceLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun <T> ChoiceChips(
    values: List<T>,
    selected: T,
    onSelected: (T) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(values) { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(value.toString()) }
            )
        }
    }
}

@Composable
private fun InfoRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    )
}
