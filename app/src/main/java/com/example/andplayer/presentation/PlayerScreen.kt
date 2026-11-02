package com.example.andplayer.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val preferences = remember { context.getSharedPreferences("permissions", Context.MODE_PRIVATE) }

    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    var shouldOpenSettings by remember { mutableStateOf(false) }
    val hasPermission = remember {
        {
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldShowRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    fun updateSettingsState() {
        shouldOpenSettings =
            preferences.getBoolean(KEY_DENIED_WITH_RATIONALE, false) && !shouldShowRationale()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            preferences.edit { remove(KEY_DENIED_WITH_RATIONALE) }
            shouldOpenSettings = false
            viewModel.onPermissionGranted()
        } else {
            if (shouldShowRationale()) {
                preferences.edit { putBoolean(KEY_DENIED_WITH_RATIONALE, true) }
            }
            updateSettingsState()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasPermission()) {
                    shouldOpenSettings = false
                    viewModel.onPermissionGranted()
                } else {
                    updateSettingsState()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val state = uiState) {
        UiState.PermissionRequired -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Permission required to display audio files.")
                if (shouldOpenSettings) Text("You can grant it in App Settings.")
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (shouldOpenSettings) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                },
                            )
                        } else {
                            shouldOpenSettings = false
                            permissionLauncher.launch(permission)
                        }
                    },
                ) {
                    Text(if (shouldOpenSettings) "Open app settings" else "Grant permission")
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

private const val KEY_DENIED_WITH_RATIONALE = "audio_permission_denied_with_rationale"
