package com.openbiblescholar.services.sword

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openbiblescholar.OpenBibleApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModuleDownloadService : Service() {

    @Inject
    lateinit var moduleManager: SwordModuleManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val moduleName = intent?.getStringExtra(EXTRA_MODULE_NAME) ?: return START_NOT_STICKY
        startForeground(NOTIFICATION_ID, buildNotification(moduleName, 0))

        // Download happens in coroutine via SwordModuleManager
        stopForeground(STOP_FOREGROUND_DETACH)
        return START_NOT_STICKY
    }

    private fun buildNotification(moduleName: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, OpenBibleApp.CHANNEL_DOWNLOADS)
            .setContentTitle("Downloading $moduleName")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val EXTRA_MODULE_NAME = "module_name"
    }
}
