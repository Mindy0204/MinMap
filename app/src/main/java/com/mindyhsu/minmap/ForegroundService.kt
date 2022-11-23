package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mindyhsu.minmap.main.MainActivity
import timber.log.Timber


private const val FOREGROUND_SERVICE_CHANNEL = "FOREGROUND SERVICE CHANNEL"
const val EXIT_NAVIGATION = "EXIT NAVIGATION"
const val EXIT_NAVIGATION_ACTION = "EXIT ACTION"

class ForegroundService : Service() {

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            val title = intent.getStringExtra("instructionTitle")
            val content = intent.getStringExtra("instructionContent")

            val bundle = Bundle()
            bundle.putString(EXIT_NAVIGATION, EXIT_NAVIGATION)

            // Exit navigation status and back to MainActivity
            // which will restart the entry flow
            // Need to add "action" that MainActivity can get string from intent
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    -2,
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
                        R.mipmap.icon_arrived,
                        getString(R.string.exit_navigation),
                        pendingIntent
                    )
                    .setSmallIcon(R.mipmap.icon_planning).build()

            if (it.getString("navigationComplete") != null) {
                stopForeground(true)
                Timber.d("status stopForeground")

            } else {
                createNotificationChannel()
                startForeground(-2, notification)
                Timber.d("status startForeground")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }
}