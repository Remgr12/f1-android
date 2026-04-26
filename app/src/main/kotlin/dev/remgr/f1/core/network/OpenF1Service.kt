package dev.remgr.f1.core.network

import dev.remgr.f1.core.network.dto.DriverDto
import dev.remgr.f1.core.network.dto.IntervalDto
import dev.remgr.f1.core.network.dto.LapDto
import dev.remgr.f1.core.network.dto.LocationDto
import dev.remgr.f1.core.network.dto.MeetingDto
import dev.remgr.f1.core.network.dto.PitDto
import dev.remgr.f1.core.network.dto.PositionDto
import dev.remgr.f1.core.network.dto.RaceControlDto
import dev.remgr.f1.core.network.dto.SessionDto
import retrofit2.http.GET
import retrofit2.http.QueryMap

// OpenF1-compatible REST API — https://f1api.remgr.dev/v1/
//
// Filter operators (date>, date<) are non-standard query param names.
// We pass them via @QueryMap; the OpenF1FilterInterceptor decodes the
// URL-encoded ">" character before the request is dispatched.
interface OpenF1Service {

    @GET("drivers")
    suspend fun getDrivers(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<DriverDto>

    @GET("sessions")
    suspend fun getSessions(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<SessionDto>

    @GET("meetings")
    suspend fun getMeetings(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<MeetingDto>

    // Race standing position (position 1–20).
    @GET("position")
    suspend fun getPositions(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<PositionDto>

    // Track XYZ telemetry — used for the live track map.
    @GET("location")
    suspend fun getLocations(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<LocationDto>

    @GET("intervals")
    suspend fun getIntervals(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<IntervalDto>

    @GET("laps")
    suspend fun getLaps(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<LapDto>

    @GET("pit")
    suspend fun getPitStops(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<PitDto>

    @GET("race_control")
    suspend fun getRaceControlMessages(
        @QueryMap filters: Map<String, String> = emptyMap(),
    ): List<RaceControlDto>
}

// Convenience builders so call-sites stay readable.
fun sessionFilter(sessionKey: String = "latest") =
    mapOf("session_key" to sessionKey)

fun sessionFilterWithDate(sessionKey: String = "latest", dateAfter: String) =
    mapOf("session_key" to sessionKey, "date>" to dateAfter)
