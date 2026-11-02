package com.example.andplayer.domain

import kotlinx.coroutines.flow.Flow

internal interface SongRepository {
    fun observeSongs(): Flow<List<Song>>
}
