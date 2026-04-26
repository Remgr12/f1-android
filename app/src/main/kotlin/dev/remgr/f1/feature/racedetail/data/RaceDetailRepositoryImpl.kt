package dev.remgr.f1.feature.racedetail.data

import dev.remgr.f1.core.database.dao.LapDao
import dev.remgr.f1.core.database.dao.RaceDao
import dev.remgr.f1.core.database.entity.LapCacheEntity
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.core.network.dto.LapDto
import dev.remgr.f1.feature.pastraces.domain.model.RaceResult
import dev.remgr.f1.feature.racedetail.domain.RaceDetailRepository
import dev.remgr.f1.feature.racedetail.domain.model.LapData
import dev.remgr.f1.feature.racedetail.domain.model.PitStop
import dev.remgr.f1.feature.racedetail.domain.model.RaceDetailModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaceDetailRepositoryImpl @Inject constructor(
    private val service: OpenF1Service,
    private val lapDao: LapDao,
    private val raceDao: RaceDao,
) : RaceDetailRepository {

    override suspend fun getRaceDetail(meetingKey: Int, sessionKey: Int): RaceDetailModel {
        val drivers = service.getDrivers(mapOf("session_key" to sessionKey.toString()))
            .associateBy { it.driverNumber }

        // Laps — cache-first; persist on network fetch.
        val lapEntities = lapDao.getLaps(sessionKey)
        val laps: List<LapDto> = if (lapEntities.isNotEmpty()) {
            lapEntities.map { it.toDto() }
        } else {
            service.getLaps(mapOf("session_key" to sessionKey.toString())).also { fetched ->
                lapDao.upsertLaps(fetched.map { it.toEntity(sessionKey, meetingKey) })
            }
        }

        val pitStops = service.getPitStops(mapOf("session_key" to sessionKey.toString()))
            .map { dto ->
                val d = drivers[dto.driverNumber]
                PitStop(dto.driverNumber, d?.nameAcronym ?: "???", dto.lapNumber, dto.pitDuration)
            }

        val results = raceDao.getResults(sessionKey).map { e ->
            RaceResult(e.position, e.driverNumber, e.driverName, e.nameAcronym, e.teamName, e.teamColour, e.points)
        }

        val lapData = laps.map { lap ->
            val d = drivers[lap.driverNumber]
            LapData(
                driverNumber = lap.driverNumber,
                nameAcronym  = d?.nameAcronym ?: "???",
                teamColour   = d?.teamColour  ?: "FFFFFF",
                lapNumber    = lap.lapNumber,
                lapDuration  = lap.lapDuration,
                isPitOutLap  = lap.isPitOutLap,
                sector1      = lap.sector1,
                sector2      = lap.sector2,
                sector3      = lap.sector3,
            )
        }

        return RaceDetailModel(
            meetingKey  = meetingKey,
            sessionKey  = sessionKey,
            raceName    = results.firstOrNull()?.let { "Race" } ?: "Race Detail",
            location    = "",
            circuitName = "",
            results     = results,
            laps        = lapData,
            pitStops    = pitStops,
            totalLaps   = laps.maxByOrNull { it.lapNumber }?.lapNumber ?: 0,
        )
    }
}

// ── Entity / DTO mappers ──────────────────────────────────────────────────────

private fun LapCacheEntity.toDto() = LapDto(
    sessionKey   = sessionKey,
    meetingKey   = meetingKey,
    driverNumber = driverNumber,
    lapNumber    = lapNumber,
    lapDuration  = lapDuration,
    isPitOutLap  = isPitOutLap,
    sector1      = sector1,
    sector2      = sector2,
    sector3      = sector3,
)

private fun LapDto.toEntity(sessionKey: Int, meetingKey: Int) = LapCacheEntity(
    sessionKey   = sessionKey,
    meetingKey   = meetingKey,
    driverNumber = driverNumber,
    lapNumber    = lapNumber,
    lapDuration  = lapDuration,
    isPitOutLap  = isPitOutLap,
    sector1      = sector1,
    sector2      = sector2,
    sector3      = sector3,
)
