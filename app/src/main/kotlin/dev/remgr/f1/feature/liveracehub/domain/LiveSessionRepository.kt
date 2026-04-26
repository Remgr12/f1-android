package dev.remgr.f1.feature.liveracehub.domain

import dev.remgr.f1.feature.liveracehub.domain.model.LiveDriver
import kotlinx.coroutines.flow.Flow

data class RaceControlMessage(
    val date: String,
    val message: String,
    val flag: String?,
    val category: String,
    val driverNumber: Int?,
)

interface LiveSessionRepository {
    fun getLiveStandings(): Flow<List<LiveDriver>>
    fun getRaceControlMessages(): Flow<List<RaceControlMessage>>
}
