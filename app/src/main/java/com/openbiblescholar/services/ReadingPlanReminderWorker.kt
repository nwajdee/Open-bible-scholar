package com.openbiblescholar.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.openbiblescholar.MainActivity
import com.openbiblescholar.OpenBibleApp
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReadingPlanReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        showReminderNotification()
        return Result.success()
    }

    private fun showReminderNotification() {
        val contentIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, OpenBibleApp.CHANNEL_REMINDERS)
            .setContentTitle("📖 Time for your Bible reading!")
            .setContentText("Keep up with your reading plan. You've got this!")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "reading_plan_reminder"
        const val NOTIFICATION_ID = 1003

        fun schedule(context: Context, hourOfDay: Int = 8, minute: Int = 0) {
            val now = System.currentTimeMillis()
            val targetCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= now) add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = targetCalendar.timeInMillis - now

            val request = PeriodicWorkRequestBuilder<ReadingPlanReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
