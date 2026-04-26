package dev.remgr.f1.feature.trackmap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.feature.trackmap.data.TrackMapRepositoryImpl
import dev.remgr.f1.feature.trackmap.domain.TrackMapRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackMapModule {

    @Binds
    @Singleton
    abstract fun bindTrackMapRepository(impl: TrackMapRepositoryImpl): TrackMapRepository
}
