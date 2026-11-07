package com.example.andplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import com.example.andplayer.domain.SongsState.Empty
import com.example.andplayer.domain.SongsState.Error
import com.example.andplayer.domain.SongsState.Loading
import com.example.andplayer.domain.SongsState.NotStarted
import com.example.andplayer.domain.SongsState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlayerBarViewModel @Inject constructor(
    private val songRepository: SongRepository,
    exoPlayer: dagger.Lazy<ExoPlayer>,
) : ViewModel() {

    private val player: ExoPlayer by lazy { exoPlayer.get() }
    private var playerInitialized = false

    private val _playerBarState = MutableStateFlow<PlayerBarUiState?>(null)
    val playerBarState = _playerBarState.asStateFlow()

    private var songs: List<Song> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
            updatePlayPauseUi()

        override fun onIsPlayingChanged(isPlaying: Boolean) = updatePlayPauseUi()

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _playerBarState.value = null
            } else {
                updatePlayPauseUi()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = songs.getOrNull(player.currentMediaItemIndex) ?: return
            _playerBarState.value = _playerBarState.value?.copy(song = song)
            updatePlayPauseUi()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            _playerBarState.value = _playerBarState.value?.copy(
                position = newPosition.positionMs.coerceAtLeast(0L),
            )
        }
    }

    init {
        observeSongsState()
        observePlayerPosition()
    }

    private fun ensurePlayerInitialized() {
        if (!playerInitialized) {
            player.addListener(playerListener)
            playerInitialized = true
        }
    }

    private fun observeSongsState() {
        viewModelScope.launch {
            songRepository.songsState.collect { state ->
                when (state) {
                    is Success -> {
                        songs = state.songs
                        if (playerInitialized) syncPlayerPlaylist(state.songs)
                    }

                    Empty -> {
                        songs = emptyList()
                        if (playerInitialized) syncPlayerPlaylist(emptyList())
                    }

                    NotStarted, Loading, is Error -> Unit
                }
            }
        }
    }

    private fun observePlayerPosition() {
        viewModelScope.launch {
            while (isActive) {
                val state = _playerBarState.value
                if (state != null && player.isPlaying) {
                    _playerBarState.value = state.copy(
                        position = player.currentPosition.coerceAtLeast(0L),
                    )
                }
                delay(500L)
            }
        }
    }

    private fun syncPlayerPlaylist(newSongs: List<Song>) {
        if (newSongs.isEmpty()) {
            player.clearMediaItems()
            _playerBarState.value = null
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
        _playerBarState.value = _playerBarState.value?.copy(isPlaying = shouldShowPause)
    }

    fun onClickTrack(id: Long) {
        val startIndex = songs.indexOfFirst { it.id == id }
        if (startIndex == -1) return

        ensurePlayerInitialized()

        _playerBarState.value = PlayerBarUiState(
            song = songs[startIndex],
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
        _playerBarState.value = null
    }

    fun onPrev() = player.seekToPrevious()
    fun onNext() = player.seekToNext()

    fun onSeekTo(position: Long) {
        val safePosition = position.coerceAtLeast(0L)
        player.seekTo(safePosition)
        _playerBarState.value = _playerBarState.value?.copy(position = safePosition)
    }

    fun onRewind() = player.seekBack()
    fun onForward() = player.seekForward()

    override fun onCleared() {
        if (playerInitialized) player.removeListener(playerListener)
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
