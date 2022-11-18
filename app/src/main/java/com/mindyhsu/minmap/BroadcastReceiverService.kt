package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BroadcastReceiverService : Service() {
    private var num = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras.let {
            when (it?.getString("message", "")) {
                "unread message" -> {
                    messageNotification()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun messageNotification() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("message channel", name, importance).apply {
                description = descriptionText
            }

            var builder = NotificationCompat.Builder(this, "message channel")
                .setSmallIcon(R.mipmap.icon_planning)
                .setContentTitle(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notification = builder.build()

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // set currentSecond as id
            // let old notification will not be cover by new notification
            notificationManager.notify(num, notification)
            num += 1
        }
    }
}