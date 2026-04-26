package dev.remgr.f1.core.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SessionReminderWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val name     = inputData.getString(KEY_SESSION_NAME) ?: return Result.failure()
        val location = inputData.getString(KEY_LOCATION)     ?: return Result.failure()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return Result.success()

        val notification = NotificationHelper.build(
            context = applicationContext,
            title   = "$name — starting soon",
            body    = "$location · 30 minutes to go",
        )
        NotificationManagerCompat.from(applicationContext)
            .notify(name.hashCode(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_SESSION_NAME = "session_name"
        const val KEY_LOCATION     = "location"
    }
}
