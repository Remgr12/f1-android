package dev.remgr.f1.feature.leaderboard.domain

import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding

interface StandingsRepository {
    suspend fun getDriverStandings(year: Int): List<DriverStanding>
    suspend fun getConstructorStandings(year: Int): List<ConstructorStanding>
}
