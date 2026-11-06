package com.example.andplayer.domain

internal sealed interface SongsState {
    data object NotStarted : SongsState
    data object Loading : SongsState
    data object Empty : SongsState
    data class Success(val songs: List<Song>) : SongsState
    data class Error(val message: String) : SongsState
}
