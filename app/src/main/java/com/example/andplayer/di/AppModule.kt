package com.example.andplayer.di

import android.content.ContentResolver
import android.content.Context
import com.example.andplayer.data.SongRepositoryImpl
import com.example.andplayer.domain.SongRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppBindModule {

    @Binds
    fun bindSongRepository(impl: SongRepositoryImpl): SongRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppProvideModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
