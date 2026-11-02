package com.example.andplayer.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted -> if (isGranted) viewModel.onPermissionGranted() }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) viewModel.onPermissionGranted()
    }

    when (val state = uiState) {
        UiState.PermissionRequired -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permission required to display audio files.")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(permission) },
                ) {
                    Text("Grant permission")
                }
            }
        }

        UiState.Empty -> Text("List is empty")
        is UiState.Error -> Text(state.message ?: "Unknown error")
        UiState.Loading -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("List is loading")
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }

        is UiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.data, key = { it.id }) { song ->
                    ElevatedClickableCard(
                        onClick = { /* TODO */ },
                    ) {
                        ListItem(
                            headlineContent = { OneLineText(song.title) },
                            supportingContent = { OneLineText(song.artist) },
                            trailingContent = { OneLineText(song.duration) },
                        )
                    }

                }
            }
        }
    }
}
