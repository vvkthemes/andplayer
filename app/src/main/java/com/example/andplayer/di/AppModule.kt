package com.example.andplayer.di

import com.example.andplayer.data.SongRepositoryImpl
import com.example.andplayer.domain.SongRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppBindModule {

    @Binds
    fun bindSongRepository(impl: SongRepositoryImpl): SongRepository
}
