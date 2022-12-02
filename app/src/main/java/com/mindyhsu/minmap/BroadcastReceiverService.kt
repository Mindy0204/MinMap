package com.mindyhsu.minmap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mindyhsu.minmap.main.KEY_CHAT_ROOM
import com.mindyhsu.minmap.main.KEY_EVENT
import com.mindyhsu.minmap.main.KEY_MESSAGE
import timber.log.Timber

private const val CHAT_ROOM_CHANNEL = "chat room channel"
private const val MESSAGE_CHANNEL = "message channel"
//private const val EVENT_CHANNEL = "event channel"

class BroadcastReceiverService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let {
            if (it.getString(KEY_CHAT_ROOM) != null) {
                chatRoomNotification(it.getString(KEY_CHAT_ROOM))
            }

            if (it.getString(KEY_MESSAGE) != null) {
                messageNotification(it.getString(KEY_MESSAGE))
            }

//            if (it.getString(KEY_EVENT) != null) {
//                eventNotification()
//            }
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

            val builder = NotificationCompat.Builder(this, CHAT_ROOM_CHANNEL)
                .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
                .setContentTitle(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notification = builder.build()

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // let old notification will not be cover by new notification
            notificationManager.notify(0x01, notification)
        }
    }

    private fun messageNotification(newMessageNum: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = if (newMessageNum!!.toInt() > 1) {
                getString(R.string.message_channel_description, newMessageNum, "s")
            } else {
                getString(R.string.message_channel_description, newMessageNum, "")
            }
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MESSAGE_CHANNEL, name, importance).apply {
                description = descriptionText
            }

            var builder = NotificationCompat.Builder(this, MESSAGE_CHANNEL)
                .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
                .setContentTitle(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val notification = builder.build()

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            notificationManager.notify(0x02, notification)
        }
    }

//    private fun eventNotification() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.app_name)
//            val descriptionText = getString(R.string.event_channel_description)
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel(EVENT_CHANNEL, name, importance).apply {
//                description = descriptionText
//            }
//
//            var builder = NotificationCompat.Builder(this, EVENT_CHANNEL)
//                .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
//                .setContentTitle(descriptionText)
//                .setDefaults(Notification.DEFAULT_ALL)
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//            val notification = builder.build()
//
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//
//            notificationManager.notify(0x03, notification)
//        }
//    }
}