package com.example.andplayer.presentation

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller = _controller.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (_controller.value === controller) {
                _controller.value = null
                controllerFuture = null
            }
        }
    }

    suspend fun await(): MediaController {
        _controller.value?.let { return it }

        controllerFuture?.let { future ->
            return suspendCancellableCoroutine { continuation ->
                future.addListener(
                    {
                        try {
                            if (continuation.isActive) continuation.resume(future.get())
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.cancel(e)
                        }
                    },
                    { command -> command.run() },
                )
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val token = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java),
            )
            val future = MediaController.Builder(context, token)
                .setListener(controllerListener)
                .buildAsync()
            controllerFuture = future

            future.addListener(
                {
                    try {
                        val controller = future.get()
                        _controller.value = controller
                        if (continuation.isActive) continuation.resume(controller)
                    } catch (e: Exception) {
                        controllerFuture = null
                        if (continuation.isActive) continuation.cancel(e)
                    }
                },
                { command -> command.run() },
            )
        }
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _controller.value = null
    }
}
