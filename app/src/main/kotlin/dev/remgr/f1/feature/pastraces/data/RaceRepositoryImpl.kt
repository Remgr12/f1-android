package dev.remgr.f1.feature.pastraces.data

import dev.remgr.f1.core.database.dao.RaceDao
import dev.remgr.f1.core.database.entity.RaceCacheEntity
import dev.remgr.f1.core.database.entity.RaceResultCacheEntity
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.core.util.F1Points
import dev.remgr.f1.feature.pastraces.domain.RaceRepository
import dev.remgr.f1.feature.pastraces.domain.model.Race
import dev.remgr.f1.feature.pastraces.domain.model.RaceResult
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaceRepositoryImpl @Inject constructor(
    private val service: OpenF1Service,
    private val dao: RaceDao,
) : RaceRepository {

    override suspend fun getRaces(year: Int): List<Race> {
        val cachedRaces = dao.getRaces(year)
        val cachedAt = dao.cachedAt(year) ?: 0L
        val isCacheValid = (System.currentTimeMillis() - cachedAt) < 6 * 60 * 60 * 1000L

        if (cachedRaces.isNotEmpty() && isCacheValid) {
            return cachedRaces.map { raceEntity ->
                val results = dao.getResults(raceEntity.sessionKey).map { resEntity ->
                    RaceResult(
                        position = resEntity.position,
                        driverNumber = resEntity.driverNumber,
                        driverName = resEntity.driverName,
                        nameAcronym = resEntity.nameAcronym,
                        teamName = resEntity.teamName,
                        teamColour = resEntity.teamColour,
                        points = resEntity.points
                    )
                }
                Race(
                    meetingKey = raceEntity.meetingKey,
                    sessionKey = raceEntity.sessionKey,
                    raceName = raceEntity.raceName,
                    location = raceEntity.location,
                    countryName = raceEntity.countryName,
                    circuitName = raceEntity.circuitName,
                    dateStart = raceEntity.dateStart,
                    results = results
                )
            }
        }

        try {
            val meetingsByKey = service.getMeetings(mapOf("year" to year.toString()))
                .associateBy { it.meetingKey }

            val now = Instant.now()
            val raceSessions = listOf("Race", "Qualifying", "Sprint", "Sprint Qualifying")
                .flatMap { type -> service.getSessions(mapOf("year" to year.toString(), "session_type" to type)) }
                .distinctBy { it.sessionKey }
                .filter { !it.isCancelled }
                .filter { parseIsoInstantOrNull(it.dateStart)?.isBefore(now) == true }
                .sortedByDescending { it.dateStart }

            val resultSessionNames = setOf("Race", "Sprint")

            val fetched = raceSessions.mapNotNull { session ->
                val meeting = meetingsByKey[session.meetingKey]

                val results = if (session.sessionName in resultSessionNames) {
                    val positions = service.getPositions(mapOf("session_key" to session.sessionKey.toString()))
                    val drivers   = service.getDrivers(mapOf("session_key" to session.sessionKey.toString()))
                        .associateBy { it.driverNumber }
                    positions
                        .groupBy { it.driverNumber }
                        .mapValues { (_, hist) -> hist.maxByOrNull { it.date } }
                        .values
                        .filterNotNull()
                        .sortedBy { it.position }
                        .map { pos ->
                            val d = drivers[pos.driverNumber]
                            RaceResult(
                                position     = pos.position,
                                driverNumber = pos.driverNumber,
                                driverName   = d?.fullName    ?: "Driver #${pos.driverNumber}",
                                nameAcronym  = d?.nameAcronym ?: "???",
                                teamName     = d?.teamName    ?: "Unknown",
                                teamColour   = d?.teamColour  ?: "FFFFFF",
                                points       = when (session.sessionName) {
                                    "Race"   -> F1Points.forPosition(pos.position)
                                    "Sprint" -> F1Points.forSprintPosition(pos.position)
                                    else     -> 0
                                },
                            )
                        }
                } else {
                    emptyList()
                }

                val meetingName = meeting?.meetingName
                    ?: "${session.countryName ?: session.location ?: "Unknown"} Grand Prix"
                Race(
                    meetingKey  = session.meetingKey,
                    sessionKey  = session.sessionKey,
                    raceName    = "$meetingName — ${session.sessionName}",
                    location    = meeting?.location ?: session.location ?: "",
                    countryName = meeting?.countryName ?: session.countryName ?: "",
                    circuitName = meeting?.circuitShortName ?: session.circuitShortName ?: "",
                    dateStart   = session.dateStart,
                    results     = results,
                )
            }.sortedByDescending { it.dateStart }

            val raceEntities = fetched.map { r ->
                RaceCacheEntity(
                    sessionKey = r.sessionKey,
                    meetingKey = r.meetingKey,
                    raceName = r.raceName,
                    location = r.location,
                    countryName = r.countryName,
                    circuitName = r.circuitName,
                    circuitKey = null,
                    dateStart = r.dateStart,
                    year = year,
                    isSprint = r.raceName.endsWith("— Sprint")
                )
            }
            dao.upsertRaces(raceEntities)

            val resultEntities = fetched.flatMap { r ->
                r.results.map { res ->
                    RaceResultCacheEntity(
                        sessionKey = r.sessionKey,
                        meetingKey = r.meetingKey,
                        position = res.position,
                        driverNumber = res.driverNumber,
                        driverName = res.driverName,
                        nameAcronym = res.nameAcronym,
                        teamName = res.teamName,
                        teamColour = res.teamColour,
                        points = res.points
                    )
                }
            }
            dao.upsertResults(resultEntities)

            return fetched
        } catch (e: Exception) {
            if (cachedRaces.isNotEmpty()) {
                return cachedRaces.map { raceEntity ->
                    val results = dao.getResults(raceEntity.sessionKey).map { resEntity ->
                        RaceResult(
                            position = resEntity.position,
                            driverNumber = resEntity.driverNumber,
                            driverName = resEntity.driverName,
                            nameAcronym = resEntity.nameAcronym,
                            teamName = resEntity.teamName,
                            teamColour = resEntity.teamColour,
                            points = resEntity.points
                        )
                    }
                    Race(
                        meetingKey = raceEntity.meetingKey,
                        sessionKey = raceEntity.sessionKey,
                        raceName = raceEntity.raceName,
                        location = raceEntity.location,
                        countryName = raceEntity.countryName,
                        circuitName = raceEntity.circuitName,
                        dateStart = raceEntity.dateStart,
                        results = results
                    )
                }
            }
            throw e
        }
    }
}

private fun parseIsoInstantOrNull(value: String?): Instant? =
    value?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
