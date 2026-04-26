package dev.remgr.f1.feature.pastraces.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.feature.pastraces.data.RaceRepositoryImpl
import dev.remgr.f1.feature.pastraces.domain.RaceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PastRacesModule {

    @Binds
    @Singleton
    abstract fun bindRaceRepository(impl: RaceRepositoryImpl): RaceRepository
}
