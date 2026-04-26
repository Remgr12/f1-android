package dev.remgr.f1.feature.pastraces.domain

import dev.remgr.f1.feature.pastraces.domain.model.Race

interface RaceRepository {
    suspend fun getRaces(year: Int): List<Race>
}
