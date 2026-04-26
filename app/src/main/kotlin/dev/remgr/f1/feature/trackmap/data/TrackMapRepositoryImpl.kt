package dev.remgr.f1.feature.trackmap.data

import androidx.compose.ui.geometry.Offset
import dev.remgr.f1.core.circuits.CircuitPaths
import dev.remgr.f1.core.database.dao.CircuitOutlineDao
import dev.remgr.f1.core.database.entity.CircuitOutlineEntity
import dev.remgr.f1.core.network.OpenF1Service
import dev.remgr.f1.feature.trackmap.domain.TrackMapRepository
import dev.remgr.f1.feature.trackmap.domain.TrackMapState
import dev.remgr.f1.feature.trackmap.domain.TrackSessionOption
import dev.remgr.f1.feature.trackmap.domain.model.TrackPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class TrackMapRepositoryImpl @Inject constructor(
    private val service: OpenF1Service,
    private val outlineDao: CircuitOutlineDao,
) : TrackMapRepository {

    // Cap history to avoid OOM — 60 s × 20 cars × ~4 Hz ≈ ~5 k points max in practice.
    private val maxTrackPoints = 20_000

    override suspend fun getTrackOptions(): List<TrackSessionOption> {
        val currentYear = Year.now().value
        val candidateYears = listOf(currentYear, currentYear - 1)

        val meetingsByKey = mutableMapOf<Int, String>()
        val sessions = mutableListOf<Pair<Int, String>>()

        candidateYears.forEach { year ->
            service.getMeetings(mapOf("year" to year.toString()))
                .forEach { meeting ->
                    meetingsByKey[meeting.meetingKey] = meeting.meetingName
                }

            service.getSessions(mapOf("year" to year.toString(), "session_type" to "Race"))
                .sortedByDescending { it.dateStart }
                .forEach { session ->
                    val dateLabel = session.dateStart.take(10)
                    val meetingName = meetingsByKey[session.meetingKey] ?: (session.location ?: "Race")
                    val circuit = session.circuitShortName ?: "Unknown circuit"
                    val label = "$dateLabel • $meetingName • $circuit"
                    sessions += session.sessionKey to label
                }
        }

        val historical = sessions
            .distinctBy { it.first }
            .take(20)
            .map { (sessionKey, label) -> TrackSessionOption(sessionKey = sessionKey, label = label) }

        return listOf(TrackSessionOption(sessionKey = null, label = "Latest session (Live)")) + historical
    }

    override fun getTrackMapState(sessionKey: Int?): Flow<TrackMapState> = flow {
        val sessionFilter = sessionKey?.toString() ?: "latest"

        val drivers = service.getDrivers(mapOf("session_key" to sessionFilter))
            .associateBy { it.driverNumber }

        val sessions = service.getSessions(mapOf("session_key" to sessionFilter))
        val session = sessions.firstOrNull()
        val circuitKey = session?.circuitKey
        val shortName = session?.circuitShortName

        val trackPointSet = LinkedHashSet<Offset>()
        var outlineSaved = false

        // Load pre-baked outline from cache if available
        if (circuitKey != null) {
            val cachedOutline = outlineDao.getOutline(circuitKey)
            if (cachedOutline != null) {
                try {
                    val points = Json.decodeFromString<List<List<Float>>>(cachedOutline.pointsJson)
                    points.forEach { pt ->
                        trackPointSet.add(Offset(pt[0], pt[1]))
                    }
                    outlineSaved = true // Already have a good outline saved
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }

        // If no cache, emit pre-baked SVG path from CircuitPaths as an instant backdrop
        if (trackPointSet.isEmpty() && shortName != null) {
            val prebaked = CircuitPaths.forCircuit(shortName)
            if (prebaked != null) {
                emit(TrackMapState(prebaked, emptyList()))
            }
        } else if (trackPointSet.isNotEmpty()) {
            emit(TrackMapState(trackPointSet.toList(), emptyList()))
        }

        if (sessionKey != null) {
            val outlineDriverNumber = drivers.keys.minOrNull()
            if (outlineDriverNumber == null) {
                emit(TrackMapState(trackPointSet.toList(), emptyList()))
                return@flow
            }

            // Historical full-session location for all drivers can exceed API payload limits.
            // Build the outline from one stable driver stream to stay within endpoint constraints.
            val outlineLocations = service.getLocations(
                mapOf(
                    "session_key" to sessionFilter,
                    "driver_number" to outlineDriverNumber.toString(),
                ),
            ).sortedBy { it.date }
            if (outlineLocations.isEmpty()) {
                emit(TrackMapState(trackPointSet.toList(), emptyList()))
                return@flow
            }

            if (!outlineSaved) {
                trackPointSet.clear()
            }
            val samplingStep = (outlineLocations.size / maxTrackPoints).coerceAtLeast(1)
            outlineLocations.forEachIndexed { index, loc ->
                if (index % samplingStep == 0) {
                    trackPointSet.add(Offset(loc.x.toFloat(), loc.y.toFloat()))
                }
            }

            if (circuitKey != null && !outlineSaved && trackPointSet.isNotEmpty()) {
                val pointsJson = Json.encodeToString(trackPointSet.map { listOf(it.x, it.y) })
                outlineDao.upsert(CircuitOutlineEntity(circuitKey, pointsJson))
                outlineSaved = true
            }

            emit(TrackMapState(trackPointSet.toList(), emptyList()))
            return@flow
        }

        var cursor = Instant.now().minusSeconds(10).toString().substringBefore(".")
        var telemetryPointsAdded = 0
        var outlineDriverNumber: Int? = null

        while (true) {
            val newLocations = service.getLocations(
                mapOf("session_key" to sessionFilter, "date>" to cursor),
            )

            if (newLocations.isNotEmpty()) {
                val latest = newLocations.maxByOrNull { it.date }?.date
                if (latest != null) cursor = latest

                if (outlineDriverNumber == null) {
                    outlineDriverNumber = newLocations
                        .groupingBy { it.driverNumber }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key
                }

                val outlineLocations = outlineDriverNumber
                    ?.let { preferred ->
                        newLocations
                            .asSequence()
                            .filter { it.driverNumber == preferred }
                            .sortedBy { it.date }
                            .toList()
                    }
                    ?.takeIf { it.isNotEmpty() }
                    ?: newLocations.sortedBy { it.date }

                // If this is the first telemetry arriving, clear the pre-baked SVG path
                // to avoid mixing 0-1000 coordinate space with absolute telemetry space
                if (telemetryPointsAdded == 0 && !outlineSaved) {
                    trackPointSet.clear()
                }

                // Accumulate track outline from a stable telemetry stream to keep path order coherent.
                outlineLocations.forEach { loc ->
                    trackPointSet.add(Offset(loc.x.toFloat(), loc.y.toFloat()))
                    telemetryPointsAdded++
                }

                // Evict oldest entries once we exceed the cap.
                while (trackPointSet.size > maxTrackPoints) {
                    trackPointSet.remove(trackPointSet.iterator().next())
                }

                // Save to cache after enough telemetry points accumulate (e.g. 1000 points)
                if (circuitKey != null && !outlineSaved && telemetryPointsAdded > 1000) {
                    val thinnedPoints = trackPointSet.filterIndexed { index, _ -> index % 20 == 0 }
                        .map { listOf(it.x, it.y) }
                    
                    if (thinnedPoints.isNotEmpty()) {
                        val pointsJson = Json.encodeToString(thinnedPoints)
                        outlineDao.upsert(CircuitOutlineEntity(circuitKey, pointsJson))
                        outlineSaved = true
                    }
                }

                // Latest position per car.
                val carPositions = newLocations
                    .groupBy { it.driverNumber }
                    .mapValues { (_, locs) -> locs.maxByOrNull { it.date } }
                    .mapNotNull { (num, loc) ->
                        loc ?: return@mapNotNull null
                        val d = drivers[num] ?: return@mapNotNull null
                        TrackPosition(
                            driverNumber = num,
                            nameAcronym  = d.nameAcronym,
                            teamColour   = d.teamColour ?: "FFFFFF",
                            x            = loc.x,
                            y            = loc.y,
                            z            = loc.z,
                        )
                    }

                emit(TrackMapState(trackPointSet.toList(), carPositions))
            } else if (trackPointSet.isNotEmpty() && telemetryPointsAdded > 0) {
                // Keep emitting the track even if cars haven't moved (only if we have real telemetry or cached outline)
                emit(TrackMapState(trackPointSet.toList(), emptyList()))
            }

            delay(1200.milliseconds)
        }
    }.flowOn(Dispatchers.IO)
}
