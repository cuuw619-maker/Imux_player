package com.imux.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imux.player.ui.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ImuxApplication
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.factory(app))
            val onboardingDone by vm.onboarding.collectAsState()
            val settings by vm.settings.collectAsState()
            val playback by vm.playbackState.collectAsState()
            val operationError by vm.operationError.collectAsState()
            var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
            var nowPlaying by rememberSaveable { mutableStateOf(false) }

            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
                    val root = DocumentFile.fromTreeUri(this, uri)
                        ?: error("The selected folder is no longer available.")
                    root.listFiles()
                    val name = root.name?.takeIf { it.isNotBlank() } ?: "Music"
                    vm.addFolder(uri.toString(), name)
                }.onFailure {
                    vm.reportOperationError(
                        it.message?.takeIf(String::isNotBlank)
                            ?: "Unable to access the selected music folder."
                    )
                }
            }

            ImuxTheme(settings.appearance, settings.dynamicColor) {
                if (!onboardingDone) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.fillMaxSize().padding(28.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Imux", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "A local-first music player built around your library.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(28.dp))
                            Button(onClick = { picker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Choose music folder")
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = nowPlaying, label = "player-surface") { fullPlayer ->
                        if (fullPlayer) {
                            NowPlayingScreen(
                                vm,
                                app,
                                playback,
                                reducedMotion = settings.animation != com.imux.player.data.AnimationMode.Full,
                                artworkAnimations = settings.artworkAnimations,
                                onBack = { nowPlaying = false }
                            )
                        } else {
                            Scaffold(
                                snackbarHost = {
                                    SnackbarHost(
                                        hostState = remember { SnackbarHostState() }
                                    )
                                },
                                bottomBar = {
                                    Column {
                                        if (settings.showMiniPlayer) MiniPlayer(
                                            playback,
                                            app,
                                            settings.showPlaybackProgress,
                                            { nowPlaying = true },
                                            vm
                                        )
                                        NavigationBar {
                                            NavigationBarItem(
                                                destination == AppDestination.Home,
                                                { destination = AppDestination.Home },
                                                icon = { Icon(Icons.Default.Home, null) },
                                                label = { Text("Home") }
                                            )
                                            NavigationBarItem(
                                                destination == AppDestination.Library,
                                                { destination = AppDestination.Library },
                                                icon = { Icon(Icons.Default.LibraryMusic, null) },
                                                label = { Text("Library") }
                                            )
                                            NavigationBarItem(
                                                destination == AppDestination.Settings,
                                                { destination = AppDestination.Settings },
                                                icon = { Icon(Icons.Default.Settings, null) },
                                                label = { Text("Settings") }
                                            )
                                        }
                                    }
                                }
                            ) { padding ->
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(padding)
                                        .pointerInput(destination) {
                                            var totalDrag = 0f
                                            detectHorizontalDragGestures(
                                                onHorizontalDrag = { change, amount ->
                                                    change.consume()
                                                    totalDrag += amount
                                                },
                                                onDragEnd = {
                                                    if (kotlin.math.abs(totalDrag) < 120f) return@detectHorizontalDragGestures
                                                    destination = when {
                                                        totalDrag < 0f && destination == AppDestination.Home -> AppDestination.Library
                                                        totalDrag < 0f && destination == AppDestination.Library -> AppDestination.Settings
                                                        totalDrag > 0f && destination == AppDestination.Settings -> AppDestination.Library
                                                        totalDrag > 0f && destination == AppDestination.Library -> AppDestination.Home
                                                        else -> destination
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    when (destination) {
                                        AppDestination.Home -> HomeScreen(vm, app) { nowPlaying = true }
                                        AppDestination.Library -> LibraryScreen(vm, app) { nowPlaying = true }
                                        AppDestination.Settings -> SettingsScreen(vm)
                                    }
                                }
                            }
                        }
                    }
                }

                if (operationError != null) {
                    AlertDialog(
                        onDismissRequest = vm::clearOperationError,
                        title = { Text("Cannot access folder") },
                        text = { Text(operationError.orEmpty()) },
                        confirmButton = {
                            TextButton(onClick = vm::clearOperationError) { Text("OK") }
                        }
                    )
                }
            }
        }
    }
}
