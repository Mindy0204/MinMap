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
import timber.log.Timber

private const val CHAT_ROOM_CHANNEL = "chatRoomChannel"
private const val CHAT_ROOM_CHANNEL_ID = 0x01
private const val MESSAGE_CHANNEL = "messageChannel"
private const val MESSAGE_CHANNEL_ID = 0x02

/** Timber message */
private const val BROADCAST_RECEIVER = "broadcast receiver notification: "

class BroadcastReceiverService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.extras?.let { bundle ->
            if (bundle.getString(KEY_CHAT_ROOM) != null) {
                bundle.getString(KEY_CHAT_ROOM)?.let {
                    chatRoomNotification(it)
                }
            } else if (bundle.getString(KEY_MESSAGE) != null) {
                bundle.getString(KEY_MESSAGE)?.let {
                    messageNotification(it)
                }
            } else {
                Timber.d(BROADCAST_RECEIVER + "unknown")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun chatRoomNotification(newChatRoomNum: String) {
        Timber.i(BROADCAST_RECEIVER + CHAT_ROOM_CHANNEL)
        val name = getString(R.string.app_name)
        var descriptionText = ""
        descriptionText = if (newChatRoomNum.toInt() > 1) {
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
        notificationManager.notify(CHAT_ROOM_CHANNEL_ID, notification)
    }

    private fun messageNotification(newMessageNum: String) {
        Timber.i(BROADCAST_RECEIVER + MESSAGE_CHANNEL)
        val name = getString(R.string.app_name)
        val descriptionText = if (newMessageNum.toInt() > 1) {
            getString(R.string.message_channel_description, newMessageNum, "s")
        } else {
            getString(R.string.message_channel_description, newMessageNum, "")
        }
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(MESSAGE_CHANNEL, name, importance).apply {
            description = descriptionText
        }

        val builder = NotificationCompat.Builder(this, MESSAGE_CHANNEL)
            .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
            .setContentTitle(descriptionText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notification = builder.build()

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(MESSAGE_CHANNEL_ID, notification)
    }
}
