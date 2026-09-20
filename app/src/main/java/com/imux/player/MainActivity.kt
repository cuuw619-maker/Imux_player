package com.imux.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imux.player.data.AnimationMode
import com.imux.player.ui.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val app = application as ImuxApplication

        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.factory(app))
            val onboardingDone by vm.onboarding.collectAsState()
            val settings by vm.settings.collectAsState()
            val playback by vm.playbackState.collectAsState()
            val operationError by vm.operationError.collectAsState()

            var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
            var nowPlaying by rememberSaveable { mutableStateOf(false) }
            var folderPickerActive by rememberSaveable { mutableStateOf(false) }

            val picker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                folderPickerActive = false
                if (uri == null) {
                    AppLogger.info("PICKER", "Folder picker cancelled")
                    return@rememberLauncherForActivityResult
                }
                AppLogger.info("PICKER", "Folder picker returned URI: $uri")
                vm.addFolder(uri.toString())
            }

            fun openFolderPicker() {
                if (folderPickerActive) return
                folderPickerActive = true
                AppLogger.info("PICKER", "Opening Android OpenDocumentTree")
                runCatching {
                    picker.launch(null)
                }.onFailure {
                    folderPickerActive = false
                    AppLogger.error("PICKER", "OpenDocumentTree launch failed", it)
                    vm.reportOperationError(
                        it.message?.takeIf(String::isNotBlank)
                            ?: "Android could not open the folder picker."
                    )
                }
            }

            ImuxTheme(
                settings.appearance,
                settings.dynamicColor,
                settings.expressive
            ) {
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
                            Button(
                                onClick = ::openFolderPicker,
                                enabled = !folderPickerActive,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choose music folder")
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = nowPlaying,
                        transitionSpec = {
                            if (
                                settings.animation == AnimationMode.Off ||
                                !settings.expressive
                            ) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(190)
                                ) togetherWith fadeOut(
                                    animationSpec = androidx.compose.animation.core.tween(130)
                                )
                            }
                        },
                        label = "player-surface"
                    ) { fullPlayer ->
                        if (fullPlayer) {
                            NowPlayingScreen(
                                vm = vm,
                                app = app,
                                state = playback,
                                reducedMotion =
                                    settings.animation == AnimationMode.Reduced ||
                                    settings.animation == AnimationMode.Off ||
                                    !settings.expressive,
                                artworkAnimations =
                                    settings.artworkAnimations &&
                                    settings.animation != AnimationMode.Off &&
                                    settings.expressive,
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
                                        if (settings.showMiniPlayer) {
                                            MiniPlayer(
                                                state = playback,
                                                app = app,
                                                showProgress = settings.showPlaybackProgress,
                                                onOpen = { nowPlaying = true },
                                                vm = vm,
                                                animationsEnabled =
                                                    settings.animation != AnimationMode.Off &&
                                                    settings.expressive
                                            )
                                        }

                                        NavigationBar {
                                            NavigationBarItem(
                                                selected = destination == AppDestination.Home,
                                                onClick = { destination = AppDestination.Home },
                                                icon = { Icon(Icons.Default.Home, null) },
                                                label = { Text("Home") }
                                            )
                                            NavigationBarItem(
                                                selected = destination == AppDestination.Library,
                                                onClick = { destination = AppDestination.Library },
                                                icon = { Icon(Icons.Default.LibraryMusic, null) },
                                                label = { Text("Library") }
                                            )
                                            NavigationBarItem(
                                                selected = destination == AppDestination.Settings,
                                                onClick = { destination = AppDestination.Settings },
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
                                                    if (kotlin.math.abs(totalDrag) < 120f) {
                                                        return@detectHorizontalDragGestures
                                                    }
                                                    destination = when {
                                                        totalDrag < 0f &&
                                                            destination == AppDestination.Home ->
                                                            AppDestination.Library

                                                        totalDrag < 0f &&
                                                            destination == AppDestination.Library ->
                                                            AppDestination.Settings

                                                        totalDrag > 0f &&
                                                            destination == AppDestination.Settings ->
                                                            AppDestination.Library

                                                        totalDrag > 0f &&
                                                            destination == AppDestination.Library ->
                                                            AppDestination.Home

                                                        else -> destination
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    when (destination) {
                                        AppDestination.Home ->
                                            HomeScreen(vm, app) { nowPlaying = true }

                                        AppDestination.Library ->
                                            LibraryScreen(vm, app) { nowPlaying = true }

                                        AppDestination.Settings ->
                                            SettingsScreen(vm)
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
                            TextButton(onClick = vm::clearOperationError) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}
