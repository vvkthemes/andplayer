package com.example.andplayer.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media.ARTIST
import android.provider.MediaStore.Audio.Media.DISPLAY_NAME
import android.provider.MediaStore.Audio.Media.DURATION
import android.provider.MediaStore.Audio.Media.TITLE
import android.provider.MediaStore.Audio.Media._ID
import com.example.andplayer.di.AppScope
import com.example.andplayer.domain.Song
import com.example.andplayer.domain.SongRepository
import com.example.andplayer.domain.SongsState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class SongRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @AppScope private val appScope: CoroutineScope,
) : SongRepository {

    private val _songsState = MutableStateFlow<SongsState>(SongsState.NotStarted)
    override val songsState = _songsState.asStateFlow()

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private var observationJob: Job? = null

    override fun startObserving() {
        if (observationJob?.isActive == true) return

        observationJob = appScope.launch {
            refreshRequests
                .receiveAsFlow()
                .onStart { emit(Unit) }
                .collectLatest {
                    _songsState.value = SongsState.Loading

                    observeSongsFromMediaStore()
                        .map { songs ->
                            if (songs.isEmpty()) SongsState.Empty else SongsState.Success(songs)
                        }
                        .catch { e ->
                            if (e is CancellationException) throw e
                            _songsState.value =
                                SongsState.Error(e.message ?: "Failed to load media")
                        }
                        .collect { state -> _songsState.value = state }
                }
        }
    }

    override fun retryObserving() {
        if (observationJob?.isActive == true) refreshRequests.trySend(Unit) else startObserving()
    }

    private fun observeSongsFromMediaStore() = callbackFlow {
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
            val displayNameColumn = it.getColumnIndexOrThrow(DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(TITLE)
            val artistColumn = it.getColumnIndexOrThrow(ARTIST)
            val durationColumn = it.getColumnIndexOrThrow(DURATION)

            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)

                    add(
                        Song(
                            id = id,
                            title = it.getString(titleColumn)
                                ?.takeIf { t -> t.isNotBlank() }
                                ?: it.getString(displayNameColumn),
                            artist = it.getString(artistColumn).orEmpty(),
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
        val PROJECTION = arrayOf(_ID, DISPLAY_NAME, TITLE, ARTIST, DURATION)
        const val SELECTION = "$DURATION > 0"
        const val SORT_ORDER = "$TITLE COLLATE NOCASE ASC"
    }
}
