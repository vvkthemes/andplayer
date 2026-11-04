package com.example.andplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val songRepository: SongRepository,
    exoPlayer: dagger.Lazy<ExoPlayer>,
) : ViewModel() {

    private val player: ExoPlayer by lazy { exoPlayer.get() }

    private val _uiState = MutableStateFlow<UiState<List<SongItem>>>(UiState.PermissionRequired)
    val uiState = _uiState.asStateFlow()

    private val _miniPlayerState = MutableStateFlow<MiniPlayerUiState?>(null)
    val miniPlayerState = _miniPlayerState.asStateFlow()

    private var songs: List<Song> = emptyList()
    private var observeSongsJob: Job? = null


    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    updatePlayPauseUi()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                        _miniPlayerState.value = null
                    } else {
                        updatePlayPauseUi()
                    }
                }
            },
        )
    }

    fun onPermissionGranted() {
        if (observeSongsJob?.isActive == true) return

        observeSongsJob = viewModelScope.launch {
            songRepository.observeSongs()
                .onStart { _uiState.value = UiState.Loading }
                .catch { error -> _uiState.value = UiState.Error(error.message) }
                .collect { result ->
                    songs = result

                    if (result.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(result.map { it.toSongItem() })
                    }
                }
        }
    }

    private fun updatePlayPauseUi() {
        val shouldShowPause = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        _miniPlayerState.value = _miniPlayerState.value?.copy(isPlaying = shouldShowPause)
    }

    fun onClickTrack(id: Long) {
        val song = songs.firstOrNull { it.id == id } ?: return

        _miniPlayerState.value = MiniPlayerUiState(
            song = song.toSongItem(),
            isPlaying = false,
        )

        player.setMediaItem(MediaItem.fromUri(song.uriString))
        player.prepare()
        player.play()
    }

    fun onTogglePlayback() = if (player.playWhenReady) player.pause() else player.play()

    fun onStop() {
        player.stop()
        player.clearMediaItems()
        _miniPlayerState.value = null
    }
}

internal data class MiniPlayerUiState(
    val song: SongItem,
    val isPlaying: Boolean,
)
