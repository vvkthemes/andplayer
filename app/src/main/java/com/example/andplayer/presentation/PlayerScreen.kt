package com.example.andplayer.presentation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionState = rememberAudioPermissionState(
        onPermissionGranted = viewModel::onPermissionGranted,
    )

    when (val state = uiState) {
        UiState.PermissionRequired -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permission required to display audio files.")
                if (permissionState.shouldOpenSettings) Text("You can grant it in App Settings.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = permissionState.requestPermission) {
                    Text(
                        if (permissionState.shouldOpenSettings) {
                            "Open app settings"
                        } else {
                            "Grant permission"
                        },
                    )
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
