package com.example.andplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<SongItem>>>(UiState.PermissionRequired)
    val uiState = _uiState.asStateFlow()

    private var observeSongsJob: Job? = null

    fun onPermissionGranted() {
        if (observeSongsJob?.isActive == true) return

        observeSongsJob = viewModelScope.launch {
            songRepository.observeSongs()
                .onStart { _uiState.value = UiState.Loading }
                .catch { error -> _uiState.value = UiState.Error(error.message) }
                .collect { songs ->
                    if (songs.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(songs.map { it.toSongItem() })
                    }
                }
        }
    }
}
