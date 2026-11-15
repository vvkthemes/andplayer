package com.example.andplayer.presentation

import androidx.lifecycle.ViewModel
import com.example.andplayer.domain.SongRepository
import com.example.andplayer.domain.SongsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class ListViewModel @Inject constructor(
    private val songRepository: SongRepository,
) : ViewModel() {

    val songsState: StateFlow<SongsState> = songRepository.songsState

    fun onPermissionGranted() = songRepository.startObserving()
    fun onRetry() = songRepository.retryObserving()
}
