package com.example.andplayer.data

import android.net.Uri
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class SongRepositoryImpl @Inject constructor() : SongRepository {
    override fun observeSongs(): Flow<List<Song>> = flow {
        delay(1000)
        emit(emptyList())

        delay(1000)
        emit(
            listOf(
                Song(
                    id = 1,
                    title = "Some Song",
                    artist = "Some Artist",
                    duration = 180000L,
                    uriString = Uri.EMPTY.toString(),
                ),
                Song(
                    id = 2,
                    title = "Some Very very very Long Song with comment and long text",
                    artist = "Some Long Artist with extra text",
                    duration = 65000L,
                    uriString = Uri.EMPTY.toString(),
                ),
            ),
        )

        delay(3000)
        throw Exception("Test error")
    }
}
