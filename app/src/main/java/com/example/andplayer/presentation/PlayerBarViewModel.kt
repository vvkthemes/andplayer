package com.example.andplayer.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import com.example.andplayer.domain.SongsState.Empty
import com.example.andplayer.domain.SongsState.Error
import com.example.andplayer.domain.SongsState.Loading
import com.example.andplayer.domain.SongsState.NotStarted
import com.example.andplayer.domain.SongsState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
internal class PlayerBarViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackConnection: PlaybackConnection,
) : ViewModel() {

    // use setPlayerBarState for mutation
    private val _playerBarState = MutableStateFlow<PlayerBarUiState?>(null)
    val playerBarState = _playerBarState.asStateFlow()

    private var songs: List<Song> = emptyList()
    private var mediaController: MediaController? = null
    private var playbackJob: Job? = null
    private var positionTickerJob: Job? = null

    private val playerListener = object : Player.Listener {

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
            updatePlayPauseUi()

        override fun onIsPlayingChanged(isPlaying: Boolean) =
            updatePlayPauseUi()

        override fun onPlaybackStateChanged(playbackState: Int) =
            updatePlayPauseUi()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
            restorePlayerBarState()

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            setPlayerBarState(
                _playerBarState.value?.copy(position = newPosition.positionMs.coerceAtLeast(0L)),
            )
        }
    }

    init {
        observeConnection()
        observeSongsState()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            playbackConnection.controller.collect { controller ->
                if (controller === mediaController) return@collect

                mediaController?.removeListener(playerListener)
                mediaController = controller
                controller?.addListener(playerListener)
                controller?.let { restorePlayerBarState() }
            }
        }
    }

    private fun observeSongsState() {
        viewModelScope.launch {
            songRepository.songsState.collect { state ->
                when (state) {
                    is Success -> {
                        songs = state.songs
                        mediaController?.let {
                            syncPlayerPlaylist(songs)
                            restorePlayerBarState()
                        }
                    }

                    Empty -> {
                        songs = emptyList()
                        mediaController?.let { syncPlayerPlaylist(songs) }
                    }

                    NotStarted, Loading, is Error -> Unit
                }
            }
        }
    }

    private fun setPlayerBarState(newState: PlayerBarUiState?) {
        val oldState = _playerBarState.value
        if (oldState == newState) return

        _playerBarState.value = newState

        when {
            newState?.isPlaying == true && oldState?.isPlaying != true -> startPositionTicker()
            newState?.isPlaying != true && oldState?.isPlaying == true -> stopPositionTicker()
        }
    }


    private fun startPositionTicker() {
        positionTickerJob?.cancel()

        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val state = _playerBarState.value ?: break
                val controller = mediaController

                if (controller?.isPlaying == true) {
                    _playerBarState.value =
                        state.copy(position = controller.currentPosition.coerceAtLeast(0L))
                }

                delay(500L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }


    private fun syncPlayerPlaylist(newSongs: List<Song>) {
        val controller = mediaController ?: return

        if (newSongs.isEmpty()) {
            controller.clearMediaItems()
            setPlayerBarState(null)
            return
        }

        val mediaItems = newSongs.map { it.toMediaItem() }
        val currentMediaId = controller.currentMediaItem?.mediaId
        val currentIndex = controller.currentMediaItemIndex
        val currentPosition = controller.currentPosition

        val newIndex = currentMediaId
            ?.let { id -> mediaItems.indexOfFirst { it.mediaId == id } }
            ?.takeIf { it != -1 }
            ?: currentIndex.coerceIn(0, mediaItems.size - 1)

        val position =
            if (mediaItems[newIndex].mediaId == currentMediaId) currentPosition
            else C.TIME_UNSET

        controller.setMediaItems(mediaItems, newIndex, position)
    }

    private fun restorePlayerBarState() {
        if (songs.isEmpty()) return

        val controller = mediaController ?: return
        val mediaItem = controller.currentMediaItem ?: return

        val song = songs.firstOrNull { it.id.toString() == mediaItem.mediaId }
            ?: run {
                setPlayerBarState(null)
                return
            }

        setPlayerBarState(
            PlayerBarUiState(
                song = song,
                isPlaying = controller.playWhenReady && controller.playbackState != Player.STATE_ENDED,
                position = controller.currentPosition.coerceAtLeast(0L),
            ),
        )
    }

    private fun updatePlayPauseUi() {
        val controller = mediaController ?: return

        val shouldShowPause =
            controller.playWhenReady && controller.playbackState != Player.STATE_ENDED

        setPlayerBarState(_playerBarState.value?.copy(isPlaying = shouldShowPause))
    }

    fun onClickTrack(id: Long) {
        val playlist = songs
        val startIndex = playlist.indexOfFirst { it.id == id }
        if (startIndex == -1) return

        setPlayerBarState(PlayerBarUiState(song = playlist[startIndex], isPlaying = false))

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            try {
                val controller = playbackConnection.await()
                ensureActive()

                val mediaItems = playlist.map { it.toMediaItem() }
                controller.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                controller.prepare()
                controller.play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PlayerBarViewModel", "Failed to start playback", e)
                setPlayerBarState(null)
            }
        }
    }

    fun onTogglePlayback() = mediaController?.let { controller ->
        if (controller.playWhenReady) controller.pause() else controller.play()
    }

    fun onStop() {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        setPlayerBarState(null)
    }

    fun onPrev() = mediaController?.seekToPrevious()
    fun onNext() = mediaController?.seekToNext()

    fun onSeekTo(position: Long) {
        val controller = mediaController ?: return
        val safePosition = position.coerceAtLeast(0L)
        controller.seekTo(safePosition)
        setPlayerBarState(_playerBarState.value?.copy(position = safePosition))
    }

    fun onRewind() = mediaController?.seekBack()
    fun onForward() = mediaController?.seekForward()

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController = null
        playbackConnection.release()
    }
}

internal data class PlayerBarUiState(
    val song: Song,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
)

private fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uriString)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build(),
        )
        .build()
}
