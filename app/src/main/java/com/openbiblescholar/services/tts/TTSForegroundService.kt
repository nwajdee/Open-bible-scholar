package com.openbiblescholar.services.tts

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openbiblescholar.MainActivity
import com.openbiblescholar.OpenBibleApp
import com.openbiblescholar.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TTSForegroundService : Service() {

    @Inject
    lateinit var ttsService: BibleTtsService

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> { /* resume TTS */ }
            ACTION_PAUSE -> ttsService.pause()
            ACTION_STOP -> {
                ttsService.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TTSForegroundService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TTSForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, OpenBibleApp.CHANNEL_TTS)
            .setContentTitle("OpenBible Scholar")
            .setContentText("Reading the Bible aloud")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.openbiblescholar.tts.PLAY"
        const val ACTION_PAUSE = "com.openbiblescholar.tts.PAUSE"
        const val ACTION_STOP = "com.openbiblescholar.tts.STOP"
    }
}
