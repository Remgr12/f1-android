package dev.remgr.f1.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.remgr.f1.core.database.dao.CircuitOutlineDao
import dev.remgr.f1.core.database.dao.DriverDao
import dev.remgr.f1.core.database.dao.LapDao
import dev.remgr.f1.core.database.dao.RaceDao
import dev.remgr.f1.core.database.dao.SessionDao
import dev.remgr.f1.core.database.dao.StandingsDao
import dev.remgr.f1.core.database.entity.CircuitOutlineEntity
import dev.remgr.f1.core.database.entity.ConstructorStandingCacheEntity
import dev.remgr.f1.core.database.entity.DriverCacheEntity
import dev.remgr.f1.core.database.entity.DriverStandingCacheEntity
import dev.remgr.f1.core.database.entity.LapCacheEntity
import dev.remgr.f1.core.database.entity.RaceCacheEntity
import dev.remgr.f1.core.database.entity.RaceResultCacheEntity
import dev.remgr.f1.core.database.entity.SessionCacheEntity

@Database(
    entities = [
        DriverCacheEntity::class,
        DriverStandingCacheEntity::class,
        ConstructorStandingCacheEntity::class,
        RaceCacheEntity::class,
        RaceResultCacheEntity::class,
        LapCacheEntity::class,
        CircuitOutlineEntity::class,
        SessionCacheEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driverDao(): DriverDao
    abstract fun standingsDao(): StandingsDao
    abstract fun raceDao(): RaceDao
    abstract fun lapDao(): LapDao
    abstract fun circuitOutlineDao(): CircuitOutlineDao
    abstract fun sessionDao(): SessionDao
}
