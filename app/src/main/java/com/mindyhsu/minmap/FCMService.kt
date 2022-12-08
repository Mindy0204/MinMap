package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

private const val FCM_SERVICE_CHANNEL = "fcmServiceChannel"
private const val FCM_SERVICE_CHANNEL_ID = 0x04

/** Timber message */
private const val FCM_MESSAGE = "FCM: received message"

class FCMService : FirebaseMessagingService() {

    /** This callback fires whenever a new token is generated */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /** Handle FCM messages */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.i(FCM_MESSAGE)

        // key-value
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d(FCM_MESSAGE + " - ${remoteMessage.data}")
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Timber.d(FCM_MESSAGE + " - title=${it.title}")
            Timber.d(FCM_MESSAGE + " - body=${it.body}")

            firebaseCloudMessageNotification(it.title ?: "Empty title", it.body ?: "Empty body")
        }
    }

    private fun firebaseCloudMessageNotification(title: String, content: String) {
        val name = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(FCM_SERVICE_CHANNEL, name, importance)
        val builder = NotificationCompat.Builder(this, FCM_SERVICE_CHANNEL)
            .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notification = builder.build()

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(FCM_SERVICE_CHANNEL_ID, notification)
    }
}
