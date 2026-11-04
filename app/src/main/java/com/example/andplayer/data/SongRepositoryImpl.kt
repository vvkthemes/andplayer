package com.example.andplayer.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media.ARTIST
import android.provider.MediaStore.Audio.Media.DURATION
import android.provider.MediaStore.Audio.Media.TITLE
import android.provider.MediaStore.Audio.Media._ID
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class SongRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : SongRepository {

    override fun observeSongs(): Flow<List<Song>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        contentResolver.registerContentObserver(AUDIO_URI, true, observer)
        trySend(Unit)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }
        .conflate()
        .map { querySongs() }

    private suspend fun querySongs(): List<Song> = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(AUDIO_URI, PROJECTION, SELECTION, null, SORT_ORDER)
            ?: throw IllegalStateException("MediaStore query returned null")

        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(_ID)
            val titleColumn = it.getColumnIndexOrThrow(TITLE)
            val artistColumn = it.getColumnIndexOrThrow(ARTIST)
            val durationColumn = it.getColumnIndexOrThrow(DURATION)

            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    add(
                        Song(
                            id = id,
                            title = it.getString(titleColumn),
                            artist = it.getString(artistColumn),
                            duration = it.getLong(durationColumn),
                            uriString = ContentUris.withAppendedId(AUDIO_URI, id).toString(),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        val AUDIO_URI: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val PROJECTION = arrayOf(_ID, TITLE, ARTIST, DURATION)
        const val SELECTION = "$DURATION > 0"
        const val SORT_ORDER = "$TITLE COLLATE NOCASE ASC"
    }
}
