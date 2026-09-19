package com.imux.player
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imux.player.ui.LibraryScreen
import com.imux.player.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ImuxApplication

        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.factory(app))
            val onboardingDone by vm.onboarding.collectAsState()

            val picker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    vm.addFolder(
                        uri.toString(),
                        DocumentFile.fromTreeUri(this, uri)?.name ?: "Music"
                    )
                }
            }

            MaterialTheme {
                if (!onboardingDone) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Welcome to Imux Player",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { picker.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose music folder")
                        }
                    }
                } else {
                    LibraryScreen(vm)
                }
            }
        }
    }
}
