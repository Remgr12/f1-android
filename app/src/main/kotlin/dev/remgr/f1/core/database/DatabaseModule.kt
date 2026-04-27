package dev.remgr.f1.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM driver_standings")
        db.execSQL("DELETE FROM constructor_standings")
        db.execSQL("DELETE FROM race_results")
        db.execSQL("DELETE FROM races")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "f1.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun driverDao(db: AppDatabase)         = db.driverDao()
    @Provides fun standingsDao(db: AppDatabase)       = db.standingsDao()
    @Provides fun raceDao(db: AppDatabase)            = db.raceDao()
    @Provides fun lapDao(db: AppDatabase)             = db.lapDao()
    @Provides fun circuitOutlineDao(db: AppDatabase)  = db.circuitOutlineDao()
    @Provides fun sessionDao(db: AppDatabase)         = db.sessionDao()
}
