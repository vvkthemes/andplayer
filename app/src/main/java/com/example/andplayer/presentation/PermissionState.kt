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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Stable
internal class PermissionState(
    val shouldOpenSettings: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
internal fun rememberAudioPermissionState(
    onPermissionGranted: () -> Unit,
): PermissionState {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember { context.getSharedPreferences("permissions", Context.MODE_PRIVATE) }
    val onGranted by rememberUpdatedState(onPermissionGranted)
    var shouldOpenSettings by remember { mutableStateOf(false) }

    val permission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun hasPermission() =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

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
            onGranted()
        } else {
            if (shouldShowRationale()) {
                preferences.edit { putBoolean(KEY_DENIED_WITH_RATIONALE, true) }
            }
            updateSettingsState()
        }
    }

    val requestPermission = {
        if (shouldOpenSettings) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        } else {
            shouldOpenSettings = false
            permissionLauncher.launch(permission)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasPermission()) {
                    shouldOpenSettings = false
                    onGranted()
                } else {
                    updateSettingsState()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return PermissionState(
        shouldOpenSettings = shouldOpenSettings,
        requestPermission = requestPermission,
    )
}

private const val KEY_DENIED_WITH_RATIONALE = "audio_permission_denied_with_rationale"
