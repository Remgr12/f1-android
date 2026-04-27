package dev.remgr.f1.feature.leaderboard.data

import dev.remgr.f1.core.database.dao.StandingsDao
import dev.remgr.f1.core.database.entity.ConstructorStandingCacheEntity
import dev.remgr.f1.core.database.entity.DriverStandingCacheEntity
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.core.util.F1Points
import dev.remgr.f1.feature.leaderboard.domain.StandingsRepository
import java.time.Instant
import java.time.OffsetDateTime
import dev.remgr.f1.feature.leaderboard.domain.model.ConstructorStanding
import dev.remgr.f1.feature.leaderboard.domain.model.DriverStanding
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandingsRepositoryImpl @Inject constructor(
    private val service: OpenF1Service,
    private val dao: StandingsDao,
) : StandingsRepository {

    override suspend fun getDriverStandings(year: Int): List<DriverStanding> {
        val cached = dao.getDriverStandings(year)
        val cachedAt = dao.driverCachedAt(year) ?: 0L
        val isCacheValid = (System.currentTimeMillis() - cachedAt) < 6 * 60 * 60 * 1000L

        if (cached.isNotEmpty() && isCacheValid) {
            return cached.map {
                DriverStanding(
                    position = it.position,
                    driverNumber = it.driverNumber,
                    fullName = it.fullName,
                    nameAcronym = it.nameAcronym,
                    teamName = it.teamName,
                    teamColour = it.teamColour,
                    headshotUrl = it.headshotUrl,
                    points = it.points,
                    wins = it.wins
                )
            }
        }

        try {
            val now = Instant.now()
            val sessions = service.getSessions(mapOf("year" to year.toString(), "session_type" to "Race"))
                .filter { !it.isCancelled }
                .filter { runCatching { OffsetDateTime.parse(it.dateStart).toInstant().isBefore(now) }.getOrDefault(false) }
            val driverInfo = service.getDrivers(mapOf("session_key" to "latest")).associateBy { it.driverNumber }

            val pointsMap = mutableMapOf<Int, Int>()
            val winsMap   = mutableMapOf<Int, Int>()

            for (session in sessions) {
                val positions = service.getPositions(mapOf("session_key" to session.sessionKey.toString()))

                positions
                    .groupBy { it.driverNumber }
                    .mapValues { (_, hist) -> hist.maxByOrNull { it.date } }
                    .values
                    .filterNotNull()
                    .forEach { pos ->
                        val pts = if (session.sessionName == "Sprint") F1Points.forSprintPosition(pos.position) else F1Points.forPosition(pos.position)
                        pointsMap.merge(pos.driverNumber, pts, Int::plus)
                        if (session.sessionName == "Race" && pos.position == 1) winsMap.merge(pos.driverNumber, 1, Int::plus)
                    }
            }

            val result = pointsMap.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }
                    .thenByDescending { winsMap[it.key] ?: 0 })
                .mapIndexed { idx, (num, pts) ->
                    val d = driverInfo[num]
                    DriverStanding(
                        position     = idx + 1,
                        driverNumber = num,
                        fullName     = d?.fullName     ?: "Driver #$num",
                        nameAcronym  = d?.nameAcronym  ?: "???",
                        teamName     = d?.teamName     ?: "Unknown",
                        teamColour   = d?.teamColour   ?: "FFFFFF",
                        headshotUrl  = d?.headshotUrl,
                        points       = pts,
                        wins         = winsMap[num]    ?: 0,
                    )
                }

            val cacheEntities = result.map {
                DriverStandingCacheEntity(
                    year = year,
                    position = it.position,
                    driverNumber = it.driverNumber,
                    fullName = it.fullName,
                    nameAcronym = it.nameAcronym,
                    teamName = it.teamName,
                    teamColour = it.teamColour,
                    headshotUrl = it.headshotUrl,
                    points = it.points,
                    wins = it.wins
                )
            }
            dao.replaceDriverStandings(year, cacheEntities)
            return result
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                return cached.map {
                    DriverStanding(
                        position = it.position,
                        driverNumber = it.driverNumber,
                        fullName = it.fullName,
                        nameAcronym = it.nameAcronym,
                        teamName = it.teamName,
                        teamColour = it.teamColour,
                        headshotUrl = it.headshotUrl,
                        points = it.points,
                        wins = it.wins
                    )
                }
            }
            throw e
        }
    }

    override suspend fun getConstructorStandings(year: Int): List<ConstructorStanding> {
        val cached = dao.getConstructorStandings(year)
        val isCacheValid = cached.isNotEmpty() && (System.currentTimeMillis() - cached.first().cachedAt) < 6 * 60 * 60 * 1000L

        if (cached.isNotEmpty() && isCacheValid) {
            return cached.map {
                ConstructorStanding(
                    position = it.position,
                    teamName = it.teamName,
                    teamColour = it.teamColour,
                    points = it.points,
                    wins = it.wins,
                    driverAcronyms = it.driverAcronyms
                )
            }
        }

        try {
            val drivers = getDriverStandings(year)

            val result = drivers
                .groupBy { it.teamName }
                .entries
                .map { (team, members) ->
                    ConstructorStanding(
                        position       = 0,
                        teamName       = team,
                        teamColour     = members.first().teamColour,
                        points         = members.sumOf { it.points },
                        wins           = members.sumOf { it.wins },
                        driverAcronyms = members.map { it.nameAcronym },
                    )
                }
                .sortedByDescending { it.points }
                .mapIndexed { idx, s ->
                    ConstructorStanding(
                        position       = idx + 1,
                        teamName       = s.teamName,
                        teamColour     = s.teamColour,
                        points         = s.points,
                        wins           = s.wins,
                        driverAcronyms = s.driverAcronyms,
                    )
                }

            val cacheEntities = result.map {
                ConstructorStandingCacheEntity(
                    year = year,
                    position = it.position,
                    teamName = it.teamName,
                    teamColour = it.teamColour,
                    points = it.points,
                    wins = it.wins,
                    driverAcronyms = it.driverAcronyms
                )
            }
            dao.replaceConstructorStandings(year, cacheEntities)
            return result
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                return cached.map {
                    ConstructorStanding(
                        position = it.position,
                        teamName = it.teamName,
                        teamColour = it.teamColour,
                        points = it.points,
                        wins = it.wins,
                        driverAcronyms = it.driverAcronyms
                    )
                }
            }
            throw e
        }
    }

    override suspend fun getCachedDriverStandings(year: Int): List<DriverStanding> =
        dao.getDriverStandings(year).map {
            DriverStanding(it.position, it.driverNumber, it.fullName, it.nameAcronym,
                it.teamName, it.teamColour, it.headshotUrl, it.points, it.wins)
        }

    override suspend fun getCachedConstructorStandings(year: Int): List<ConstructorStanding> =
        dao.getConstructorStandings(year).map {
            ConstructorStanding(it.position, it.teamName, it.teamColour,
                it.points, it.wins, it.driverAcronyms)
        }
}
