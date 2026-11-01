package com.example.andplayer.domain

internal data class Song(
    val id: Long,
    val title: String,
    val artist: String?,
    val duration: Long,
    val uriString: String,
)
