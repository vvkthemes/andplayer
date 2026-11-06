package com.example.andplayer.presentation

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
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
                    if (playbackState == Player.STATE_ENDED) {
                        _miniPlayerState.value = null
                    } else {
                        updatePlayPauseUi()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val song = songs.getOrNull(player.currentMediaItemIndex) ?: return
                    _miniPlayerState.value = _miniPlayerState.value?.copy(song = song.toSongItem())
                    updatePlayPauseUi()
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    _miniPlayerState.value = _miniPlayerState.value?.copy(
                        position = newPosition.positionMs.coerceAtLeast(0L),
                    )
                }
            },
        )

        viewModelScope.launch {
            while (isActive) {
                val state = _miniPlayerState.value
                if (state != null && player.isPlaying) {
                    _miniPlayerState.value = state.copy(
                        position = player.currentPosition.coerceAtLeast(0L),
                    )
                }
                delay(500L)
            }
        }
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

                    syncPlayerPlaylist(result)
                }
        }
    }

    private fun syncPlayerPlaylist(newSongs: List<Song>) {
        if (newSongs.isEmpty()) {
            player.clearMediaItems()
            _miniPlayerState.value = null
            return
        }

        val mediaItems = newSongs.map { it.toMediaItem() }
        val currentMediaId = player.currentMediaItem?.mediaId
        val currentIndex = player.currentMediaItemIndex
        val currentPosition = player.currentPosition

        val newIndex = currentMediaId
            ?.let { id -> mediaItems.indexOfFirst { it.mediaId == id } }
            ?.takeIf { it != -1 }
            ?: currentIndex.coerceIn(0, mediaItems.size - 1)

        val position =
            if (mediaItems[newIndex].mediaId == currentMediaId) currentPosition else C.TIME_UNSET

        player.setMediaItems(mediaItems, newIndex, position)
    }

    private fun updatePlayPauseUi() {
        val shouldShowPause = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        _miniPlayerState.value = _miniPlayerState.value?.copy(isPlaying = shouldShowPause)
    }

    fun onClickTrack(id: Long) {
        val startIndex = songs.indexOfFirst { it.id == id }
        if (startIndex == -1) return

        _miniPlayerState.value = MiniPlayerUiState(
            song = songs[startIndex].toSongItem(),
            isPlaying = false,
        )

        val mediaItems = songs.map { it.toMediaItem() }
        player.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()
    }

    fun onTogglePlayback() = if (player.playWhenReady) player.pause() else player.play()

    fun onStop() {
        player.stop()
        player.clearMediaItems()
        _miniPlayerState.value = null
    }

    fun onPrev() = player.seekToPrevious()
    fun onNext() = player.seekToNext()

    fun onSeek(position: Long) {
        player.seekTo(position)
        _miniPlayerState.value = _miniPlayerState.value?.copy(position = position)
    }

    fun onRewind() = player.seekBack()
    fun onForward() = player.seekForward()
}

internal data class MiniPlayerUiState(
    val song: SongItem,
    val isPlaying: Boolean,
    val position: Long = 0L,
)

private fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uriString.toUri())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build(),
        )
        .build()
}
