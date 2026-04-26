package dev.remgr.f1.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverDto(
    @SerialName("driver_number")  val driverNumber: Int    = 0,
    @SerialName("broadcast_name") val broadcastName: String = "",
    @SerialName("full_name")      val fullName: String      = "",
    @SerialName("name_acronym")   val nameAcronym: String   = "",
    @SerialName("team_name")      val teamName: String?     = null,
    @SerialName("team_colour")    val teamColour: String?   = null,
    @SerialName("headshot_url")   val headshotUrl: String?  = null,
    @SerialName("session_key")    val sessionKey: Int       = 0,
    @SerialName("meeting_key")    val meetingKey: Int       = 0,
)

@Serializable
data class SessionDto(
    @SerialName("session_key")         val sessionKey: Int      = 0,
    @SerialName("session_name")        val sessionName: String  = "",
    @SerialName("session_type")        val sessionType: String  = "",
    @SerialName("date_start")          val dateStart: String    = "",
    @SerialName("date_end")            val dateEnd: String?     = null,
    @SerialName("meeting_key")         val meetingKey: Int      = 0,
    @SerialName("year")                val year: Int            = 0,
    @SerialName("location")            val location: String?    = null,
    @SerialName("country_name")        val countryName: String? = null,
    @SerialName("circuit_key")         val circuitKey: Int?     = null,
    @SerialName("circuit_short_name")  val circuitShortName: String? = null,
    @SerialName("is_cancelled")        val isCancelled: Boolean      = false,
)

@Serializable
data class MeetingDto(
    @SerialName("meeting_key")           val meetingKey: Int    = 0,
    @SerialName("meeting_name")          val meetingName: String = "",
    @SerialName("meeting_official_name") val officialName: String? = null,
    @SerialName("location")              val location: String   = "",
    @SerialName("country_code")          val countryCode: String = "",
    @SerialName("country_name")          val countryName: String = "",
    @SerialName("circuit_key")           val circuitKey: Int    = 0,
    @SerialName("circuit_short_name")    val circuitShortName: String = "",
    @SerialName("date_start")            val dateStart: String  = "",
    @SerialName("year")                  val year: Int          = 0,
)

// Race standing position (not track XYZ position — see LocationDto).
@Serializable
data class PositionDto(
    @SerialName("meeting_key")   val meetingKey: Int   = 0,
    @SerialName("session_key")   val sessionKey: Int   = 0,
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("date")          val date: String      = "",
    @SerialName("position")      val position: Int     = 0,
)

// Real-time XYZ telemetry coordinates for the track map.
@Serializable
data class LocationDto(
    @SerialName("session_key")   val sessionKey: Int   = 0,
    @SerialName("meeting_key")   val meetingKey: Int   = 0,
    @SerialName("driver_number") val driverNumber: Int = 0,
    @SerialName("date")          val date: String      = "",
    @SerialName("x")             val x: Int            = 0,
    @SerialName("y")             val y: Int            = 0,
    @SerialName("z")             val z: Int            = 0,
)

@Serializable
data class IntervalDto(
    @SerialName("session_key")   val sessionKey: Int    = 0,
    @SerialName("meeting_key")   val meetingKey: Int    = 0,
    @SerialName("driver_number") val driverNumber: Int  = 0,
    @SerialName("date")          val date: String       = "",
    @SerialName("gap_to_leader") val gapToLeader: Double? = null,
    @SerialName("interval")      val interval: Double?  = null,
)

@Serializable
data class LapDto(
    @SerialName("session_key")       val sessionKey: Int      = 0,
    @SerialName("meeting_key")       val meetingKey: Int      = 0,
    @SerialName("driver_number")     val driverNumber: Int    = 0,
    @SerialName("lap_number")        val lapNumber: Int       = 0,
    @SerialName("lap_duration")      val lapDuration: Double? = null,
    @SerialName("is_pit_out_lap")    val isPitOutLap: Boolean = false,
    @SerialName("date_start")        val dateStart: String?   = null,
    @SerialName("duration_sector_1") val sector1: Double?     = null,
    @SerialName("duration_sector_2") val sector2: Double?     = null,
    @SerialName("duration_sector_3") val sector3: Double?     = null,
    @SerialName("i1_speed")          val i1Speed: Int?        = null,
    @SerialName("i2_speed")          val i2Speed: Int?        = null,
    @SerialName("st_speed")          val stSpeed: Int?        = null,
)

@Serializable
data class PitDto(
    @SerialName("session_key")   val sessionKey: Int     = 0,
    @SerialName("meeting_key")   val meetingKey: Int     = 0,
    @SerialName("driver_number") val driverNumber: Int   = 0,
    @SerialName("lap_number")    val lapNumber: Int      = 0,
    @SerialName("date")          val date: String        = "",
    @SerialName("pit_duration")  val pitDuration: Double? = null,
)

@Serializable
data class RaceControlDto(
    @SerialName("session_key")   val sessionKey: Int     = 0,
    @SerialName("meeting_key")   val meetingKey: Int     = 0,
    @SerialName("date")          val date: String        = "",
    @SerialName("driver_number") val driverNumber: Int?  = null,
    @SerialName("lap_number")    val lapNumber: Int?     = null,
    @SerialName("category")      val category: String    = "",
    @SerialName("flag")          val flag: String?       = null,
    @SerialName("message")       val message: String     = "",
    @SerialName("scope")         val scope: String?      = null,
    @SerialName("sector")        val sector: Int?        = null,
)
