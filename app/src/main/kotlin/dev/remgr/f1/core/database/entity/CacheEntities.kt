package dev.remgr.f1.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverCacheEntity(
    @PrimaryKey val driverNumber: Int,
    val sessionKey: Int,
    val fullName: String,
    val nameAcronym: String,
    val teamName: String?,
    val teamColour: String?,
    val headshotUrl: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "driver_standings", primaryKeys = ["year", "driverNumber"])
data class DriverStandingCacheEntity(
    val year: Int,
    val position: Int,
    val driverNumber: Int,
    val fullName: String,
    val nameAcronym: String,
    val teamName: String,
    val teamColour: String,
    val headshotUrl: String?,
    val points: Int,
    val wins: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "constructor_standings", primaryKeys = ["year", "teamName"])
data class ConstructorStandingCacheEntity(
    val year: Int,
    val position: Int,
    val teamName: String,
    val teamColour: String,
    val points: Int,
    val wins: Int,
    val driverAcronyms: List<String>, // JSON via TypeConverter
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "races")
data class RaceCacheEntity(
    @PrimaryKey val sessionKey: Int,
    val meetingKey: Int,
    val raceName: String,
    val location: String,
    val countryName: String,
    val circuitName: String,
    val circuitKey: Int?,
    val dateStart: String,
    val year: Int,
    val isSprint: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "race_results", primaryKeys = ["sessionKey", "driverNumber"])
data class RaceResultCacheEntity(
    val sessionKey: Int,
    val meetingKey: Int,
    val position: Int,
    val driverNumber: Int,
    val driverName: String,
    val nameAcronym: String,
    val teamName: String,
    val teamColour: String,
    val points: Int,
)

@Entity(tableName = "laps", primaryKeys = ["sessionKey", "driverNumber", "lapNumber"])
data class LapCacheEntity(
    val sessionKey: Int,
    val meetingKey: Int,
    val driverNumber: Int,
    val lapNumber: Int,
    val lapDuration: Double?,
    val sector1: Double?,
    val sector2: Double?,
    val sector3: Double?,
    val isPitOutLap: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)

// Thinned circuit outline from accumulated telemetry, keyed by OpenF1 circuit_key.
@Entity(tableName = "circuit_outlines")
data class CircuitOutlineEntity(
    @PrimaryKey val circuitKey: Int,
    // JSON-encoded list of [x, y] pairs (every 20th telemetry point).
    val pointsJson: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sessions")
data class SessionCacheEntity(
    @PrimaryKey val sessionKey: Int,
    val sessionName: String,
    val sessionType: String,
    val dateStart: String,
    val dateEnd: String?,
    val meetingKey: Int,
    val year: Int,
    val location: String?,
    val countryName: String?,
    val circuitKey: Int?,
    val circuitShortName: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)
