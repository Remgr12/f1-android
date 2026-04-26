package dev.remgr.f1.feature.leaderboard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.remgr.f1.feature.leaderboard.data.StandingsRepositoryImpl
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LeaderboardModule {

    @Binds
    @Singleton
    abstract fun bindStandingsRepository(impl: StandingsRepositoryImpl): StandingsRepository
}
