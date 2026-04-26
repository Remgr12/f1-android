package dev.remgr.f1.feature.liveracehub.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.feature.liveracehub.data.LiveSessionRepositoryImpl
import dev.remgr.f1.feature.liveracehub.domain.LiveSessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveRaceHubModule {

    @Binds
    @Singleton
    abstract fun bindLiveSessionRepository(impl: LiveSessionRepositoryImpl): LiveSessionRepository
}
