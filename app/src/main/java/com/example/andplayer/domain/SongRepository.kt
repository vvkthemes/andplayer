package com.example.andplayer.domain

import kotlinx.coroutines.flow.StateFlow

internal interface SongRepository {
    val songsState: StateFlow<SongsState>
    fun startObserving()
    fun retryObserving()
}
