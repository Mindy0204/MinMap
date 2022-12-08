package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mindyhsu.minmap.main.MainActivity
import timber.log.Timber

private const val FOREGROUND_SERVICE_CHANNEL = "foregroundServiceChannel"
private const val FOREGROUND_SERVICE_CHANNEL_ID = -2 // Only decimal id can be used
const val INSTRUCTION_TITLE = "instructionTitle"
const val INSTRUCTION_CONTENT = "instructionContent"
const val NAVIGATION_COMPLETE = "navigationComplete"
const val EXIT_NAVIGATION = "exitNavigation"
const val EXIT_NAVIGATION_ACTION = "exitNavigationAction"
private const val PENDING_INTENT_REQUEST_CODE = 0

/** Timber message */
private const val FOREGROUND_STATUS = "foreground status: "

class ForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.extras?.let {
            val title = intent.getStringExtra(INSTRUCTION_TITLE)
            val content = intent.getStringExtra(INSTRUCTION_CONTENT)

            val bundle = Bundle()
            bundle.putString(EXIT_NAVIGATION, EXIT_NAVIGATION)

            /**
             * Exit navigation status and back to MainActivity
             * which will restart the entry flow
             * Need to add "action" that MainActivity can get string from intent
             * */
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    PENDING_INTENT_REQUEST_CODE,
                    Intent(this, MainActivity::class.java).apply {
                        putExtras(bundle)
                        action = EXIT_NAVIGATION_ACTION
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )

            val notification =
                NotificationCompat.Builder(this, FOREGROUND_SERVICE_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(content)
                    .addAction(
                        R.mipmap.icon_planning,
                        getString(R.string.exit_navigation),
                        pendingIntent
                    )
                    .setSmallIcon(R.mipmap.icon_planning)
                    .setSilent(true)
                    .build()

            if (it.getString(NAVIGATION_COMPLETE) != null) {
                stopForeground(true)
                Timber.d(FOREGROUND_STATUS + "stop")
            } else {
                createNotificationChannel()
                startForeground(FOREGROUND_SERVICE_CHANNEL_ID, notification)
                Timber.d(FOREGROUND_STATUS + "start")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            FOREGROUND_SERVICE_CHANNEL,
            FOREGROUND_SERVICE_CHANNEL,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(serviceChannel)
    }
}
