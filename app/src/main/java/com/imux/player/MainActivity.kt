package com.imux.player
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imux.player.ui.LibraryScreen
import com.imux.player.ui.MainViewModel
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val a=application as ImuxApplication;setContent{val vm:MainViewModel=viewModel(factory=MainViewModel.factory(a));val done by vm.onboarding.collectAsState();val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){u->if(u!=null){runCatching{contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};vm.addFolder(u.toString(),DocumentFile.fromTreeUri(this,u)?.name?:"Music")}};MaterialTheme{if(!done)Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center){Text("Welcome to Imux Player",style=MaterialTheme.typography.displaySmall);Spacer(Modifier.height(16.dp));Button({picker.launch(null)},Modifier.fillMaxWidth()){Text("Choose music folder")}}else LibraryScreen(vm)}}}}