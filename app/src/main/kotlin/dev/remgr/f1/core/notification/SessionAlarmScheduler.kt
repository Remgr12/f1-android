package dev.remgr.f1.core.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.remgr.f1.core.database.dao.SessionDao
import dev.remgr.f1.core.database.entity.SessionCacheEntity
import dev.remgr.f1.core.network.OpenF1Service
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val service: OpenF1Service,
) {
    suspend fun scheduleUpcomingReminders() {
        val now = Instant.now().toString().substringBefore(".")

        // Sync upcoming sessions from network
        try {
            val currentYear = Instant.now().toString().substringBefore("-")
            val meetings = service.getMeetings(mapOf("year" to currentYear))
            val sessions = service.getSessions(mapOf("year" to currentYear))
            
            val meetingMap = meetings.associateBy { it.meetingKey }
            val sessionEntities = sessions.map { s ->
                val m = meetingMap[s.meetingKey]
                SessionCacheEntity(
                    sessionKey = s.sessionKey,
                    sessionName = s.sessionName,
                    sessionType = s.sessionType,
                    dateStart = s.dateStart,
                    dateEnd = s.dateEnd,
                    meetingKey = s.meetingKey,
                    year = s.year,
                    location = m?.location ?: s.location,
                    countryName = m?.countryName ?: s.countryName,
                    circuitKey = m?.circuitKey ?: s.circuitKey,
                    circuitShortName = m?.circuitShortName ?: s.circuitShortName
                )
            }
            sessionDao.upsertAll(sessionEntities)
        } catch (e: Exception) {
            // Ignore network errors, fall back to cached sessions
        }

        val upcoming = sessionDao.getUpcoming(now)

        upcoming.forEach { session ->
            val startInstant   = runCatching { Instant.parse("${session.dateStart}Z") }.getOrNull() ?: return@forEach
            val reminderInstant = startInstant.minusSeconds(30 * 60)
            val delayMs        = reminderInstant.toEpochMilli() - System.currentTimeMillis()

            if (delayMs <= 0) return@forEach

            val data = workDataOf(
                SessionReminderWorker.KEY_SESSION_NAME to "${session.sessionName} — ${session.circuitShortName ?: session.countryName}",
                SessionReminderWorker.KEY_LOCATION     to (session.location ?: session.countryName ?: ""),
            )

            val request = OneTimeWorkRequestBuilder<SessionReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("reminder_${session.sessionKey}")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_${session.sessionKey}",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
