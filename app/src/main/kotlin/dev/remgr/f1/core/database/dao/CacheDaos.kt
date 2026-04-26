package dev.remgr.f1.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.remgr.f1.core.database.entity.CircuitOutlineEntity
import dev.remgr.f1.core.database.entity.ConstructorStandingCacheEntity
import dev.remgr.f1.core.database.entity.DriverCacheEntity
import dev.remgr.f1.core.database.entity.DriverStandingCacheEntity
import dev.remgr.f1.core.database.entity.LapCacheEntity
import dev.remgr.f1.core.database.entity.RaceCacheEntity
import dev.remgr.f1.core.database.entity.RaceResultCacheEntity
import dev.remgr.f1.core.database.entity.SessionCacheEntity

@Dao
interface DriverDao {
    @Upsert
    suspend fun upsertAll(drivers: List<DriverCacheEntity>)

    @Query("SELECT * FROM drivers")
    suspend fun getAll(): List<DriverCacheEntity>
}

@Dao
interface StandingsDao {
    @Query("SELECT * FROM driver_standings WHERE year = :year ORDER BY position ASC")
    suspend fun getDriverStandings(year: Int): List<DriverStandingCacheEntity>

    @Query("SELECT cachedAt FROM driver_standings WHERE year = :year LIMIT 1")
    suspend fun driverCachedAt(year: Int): Long?

    @Transaction
    suspend fun replaceDriverStandings(year: Int, rows: List<DriverStandingCacheEntity>) {
        clearDriverStandings(year)
        insertDriverStandings(rows)
    }

    @Query("DELETE FROM driver_standings WHERE year = :year")
    suspend fun clearDriverStandings(year: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriverStandings(rows: List<DriverStandingCacheEntity>)

    @Query("SELECT * FROM constructor_standings WHERE year = :year ORDER BY position ASC")
    suspend fun getConstructorStandings(year: Int): List<ConstructorStandingCacheEntity>

    @Transaction
    suspend fun replaceConstructorStandings(year: Int, rows: List<ConstructorStandingCacheEntity>) {
        clearConstructorStandings(year)
        insertConstructorStandings(rows)
    }

    @Query("DELETE FROM constructor_standings WHERE year = :year")
    suspend fun clearConstructorStandings(year: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConstructorStandings(rows: List<ConstructorStandingCacheEntity>)
}

@Dao
interface RaceDao {
    @Query("SELECT * FROM races WHERE year = :year ORDER BY dateStart DESC")
    suspend fun getRaces(year: Int): List<RaceCacheEntity>

    @Query("SELECT cachedAt FROM races WHERE year = :year LIMIT 1")
    suspend fun cachedAt(year: Int): Long?

    @Upsert
    suspend fun upsertRaces(races: List<RaceCacheEntity>)

    @Query("SELECT * FROM race_results WHERE sessionKey = :sessionKey ORDER BY position ASC")
    suspend fun getResults(sessionKey: Int): List<RaceResultCacheEntity>

    @Upsert
    suspend fun upsertResults(results: List<RaceResultCacheEntity>)
}

@Dao
interface LapDao {
    @Query("SELECT * FROM laps WHERE sessionKey = :sessionKey ORDER BY driverNumber, lapNumber")
    suspend fun getLaps(sessionKey: Int): List<LapCacheEntity>

    @Upsert
    suspend fun upsertLaps(laps: List<LapCacheEntity>)
}

@Dao
interface CircuitOutlineDao {
    @Query("SELECT * FROM circuit_outlines WHERE circuitKey = :key")
    suspend fun getOutline(key: Int): CircuitOutlineEntity?

    @Upsert
    suspend fun upsert(outline: CircuitOutlineEntity)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE year = :year ORDER BY dateStart ASC")
    suspend fun getSessions(year: Int): List<SessionCacheEntity>

    // Sessions that start after `now` (ISO string comparison — works for RFC-3339 dates).
    @Query("SELECT * FROM sessions WHERE dateStart > :now ORDER BY dateStart ASC LIMIT 10")
    suspend fun getUpcoming(now: String): List<SessionCacheEntity>

    @Upsert
    suspend fun upsertAll(sessions: List<SessionCacheEntity>)
}
