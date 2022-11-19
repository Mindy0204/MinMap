package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mindyhsu.minmap.main.KEY_CHAT_ROOM
import com.mindyhsu.minmap.main.KEY_MESSAGE
import com.mindyhsu.minmap.main.UNREAD_MESSAGE

private const val CHAT_ROOM_CHANNEL = "chat room channel"
private const val MESSAGE_CHANNEL = "message channel"

class BroadcastReceiverService : Service() {
    private var num = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            chatRoomNotification(it.getString(KEY_CHAT_ROOM))

            when (it.getString(KEY_MESSAGE, "")) {
                UNREAD_MESSAGE -> {
                    messageNotification()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun chatRoomNotification(newChatRoomNum: String?) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            var descriptionText = ""
            descriptionText = if (newChatRoomNum!!.toInt() > 1) {
                getString(R.string.chat_room_channel_description, newChatRoomNum, "s")
            } else {
                getString(R.string.chat_room_channel_description, newChatRoomNum, "")
            }
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHAT_ROOM_CHANNEL, name, importance).apply {
                description = descriptionText
            }

            var builder = NotificationCompat.Builder(this, CHAT_ROOM_CHANNEL)
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
            notificationManager.notify(-1, notification)
        }
    }

    private fun messageNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.message_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MESSAGE_CHANNEL, name, importance).apply {
                description = descriptionText
            }

            var builder = NotificationCompat.Builder(this, MESSAGE_CHANNEL)
                .setSmallIcon(R.mipmap.icon_planning)
                .setContentTitle(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notification = builder.build()

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            notificationManager.notify(num, notification)
            num += 1
        }
    }
}