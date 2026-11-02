package com.example.andplayer.presentation

import com.example.andplayer.domain.Song

internal data class SongItem(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: String,
)

internal fun Song.toSongItem(): SongItem = SongItem(
    id = id,
    title = title,
    artist = artist?.takeIf { it.isNotBlank() } ?: "No artist",
    duration = formatDuration(duration),
)

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
