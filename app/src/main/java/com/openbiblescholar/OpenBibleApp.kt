package com.openbiblescholar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OpenBibleApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // TTS playback channel
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TTS,
                    "Bible Reading",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Controls for text-to-speech Bible reading"
                    setShowBadge(false)
                }
            )

            // Module downloads
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DOWNLOADS,
                    "Module Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Bible module download progress"
                }
            )

            // Reading plan reminders
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Reading Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily Bible reading plan reminders"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_TTS = "tts_playback"
        const val CHANNEL_DOWNLOADS = "module_downloads"
        const val CHANNEL_REMINDERS = "reading_reminders"
    }
}
