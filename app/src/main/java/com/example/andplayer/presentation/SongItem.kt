package com.example.andplayer.presentation

import com.example.andplayer.domain.Song

internal data class SongItem(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val durationString: String,
)

internal fun Song.toSongItem(): SongItem = SongItem(
    id = id,
    title = title,
    artist = artist?.takeIf { it.isNotBlank() } ?: "No artist",
    duration = duration,
    durationString = getDurationString(duration),
)
