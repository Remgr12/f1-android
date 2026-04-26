package dev.remgr.f1.feature.liveracehub.data

import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.core.util.pollFlow
import dev.remgr.f1.feature.liveracehub.domain.LiveSessionRepository
import dev.remgr.f1.feature.liveracehub.domain.RaceControlMessage
import dev.remgr.f1.feature.liveracehub.domain.model.LiveDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class LiveSessionRepositoryImpl @Inject constructor(
    private val service: OpenF1Service,
) : LiveSessionRepository {

    // Poll OpenF1 at 1 s intervals.  We advance the cursor timestamp so each
    // request only fetches new data, keeping bandwidth low.
    override fun getLiveStandings(): Flow<List<LiveDriver>> = flow {
        val drivers = service.getDrivers(mapOf("session_key" to "latest"))
            .associateBy { it.driverNumber }

        // Start cursor 5 s before "now" to catch any already-in-flight data.
        var cursor = Instant.now().minusSeconds(5).toString().substringBefore(".")
        var lapCursor = Instant.now().minusSeconds(30).toString().substringBefore(".")
        var tick = 0

        // Mutable snapshot — holds the last known state per driver between polls.
        val positionSnapshot  = mutableMapOf<Int, Int>()     // driverNumber → position
        val gapSnapshot       = mutableMapOf<Int, Double?>()
        val intervalSnapshot  = mutableMapOf<Int, Double?>()
        val lapSnapshot       = mutableMapOf<Int, Int>()

        while (true) {
            val newPositions  = service.getPositions(mapOf("session_key" to "latest", "date>" to cursor))
            val newIntervals  = service.getIntervals(mapOf("session_key" to "latest", "date>" to cursor))
            val shouldRefreshLaps = tick % 15 == 0
            val newLaps = if (shouldRefreshLaps) {
                service.getLaps(mapOf("session_key" to "latest", "date_start>" to lapCursor))
            } else {
                emptyList()
            }

            newPositions.forEach { p -> positionSnapshot[p.driverNumber] = p.position }
            newIntervals.forEach { i ->
                gapSnapshot[i.driverNumber]      = i.gapToLeader
                intervalSnapshot[i.driverNumber] = i.interval
            }
            newLaps
                .groupBy { it.driverNumber }
                .mapValues { (_, laps) -> laps.maxByOrNull { it.lapNumber }?.lapNumber ?: 0 }
                .forEach { (num, lap) -> lapSnapshot[num] = lap }

            val latest = (newPositions.maxByOrNull { it.date }?.date)
            if (latest != null) cursor = latest
            val latestLapDate = newLaps
                .mapNotNull { it.dateStart?.substringBefore(".") }
                .maxOrNull()
            if (latestLapDate != null) lapCursor = latestLapDate

            val liveDrivers = positionSnapshot.entries
                .sortedBy { it.value }
                .mapNotNull { (num, pos) ->
                    val d = drivers[num] ?: return@mapNotNull null
                    LiveDriver(
                        position     = pos,
                        driverNumber = num,
                        nameAcronym  = d.nameAcronym,
                        teamName     = d.teamName  ?: "Unknown",
                        teamColour   = d.teamColour ?: "FFFFFF",
                        gapToLeader  = gapSnapshot[num],
                        interval     = intervalSnapshot[num],
                        currentLap   = lapSnapshot[num] ?: 0,
                        inPit        = false,
                    )
                }

            if (liveDrivers.isNotEmpty()) emit(liveDrivers)
            tick++
            kotlinx.coroutines.delay(1.seconds)
        }
    }.flowOn(Dispatchers.IO)

    override fun getRaceControlMessages(): Flow<List<RaceControlMessage>> = pollFlow(3.seconds) {
        service.getRaceControlMessages(mapOf("session_key" to "latest"))
            .map { dto ->
                RaceControlMessage(
                    date         = dto.date,
                    message      = dto.message,
                    flag         = dto.flag,
                    category     = dto.category,
                    driverNumber = dto.driverNumber,
                )
            }
    }
}
