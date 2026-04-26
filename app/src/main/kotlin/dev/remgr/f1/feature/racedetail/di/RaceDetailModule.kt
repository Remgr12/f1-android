package dev.remgr.f1.feature.racedetail.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.feature.racedetail.data.RaceDetailRepositoryImpl
import dev.remgr.f1.feature.racedetail.domain.RaceDetailRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RaceDetailModule {

    @Binds
    @Singleton
    abstract fun bindRaceDetailRepository(impl: RaceDetailRepositoryImpl): RaceDetailRepository
}
