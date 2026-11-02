package com.example.andplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PlayerViewModel @Inject constructor(
    private val songRepository: SongRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Song>>>(UiState.Empty)
    val uiState = _uiState.asStateFlow()

    init {
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            songRepository.observeSongs()
                .onStart { _uiState.value = UiState.Loading }
                .catch { error -> _uiState.value = UiState.Error(error.message) }
                .collect { songs ->
                    _uiState.value = if (songs.isEmpty()) UiState.Empty else UiState.Success(songs)
                }
        }
    }
}
