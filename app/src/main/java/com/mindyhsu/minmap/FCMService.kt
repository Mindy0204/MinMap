package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

private const val FCM_SERVICE_CHANNEL = "FCM SERVICE CHANNEL"

class FCMService : FirebaseMessagingService() {

    // This callback fires whenever a new token is generated
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    // Handle FCM messages
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // key-value
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("minddddy, FCM message=${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.d("minddddy, FCM message title=${it.title}")
            Timber.d("minddddy, FCM message body=${it.body}")

            firebaseCloudMessageNotification(it.title ?: "Empty title", it.body ?: "Empty body")
        }
    }

    private fun firebaseCloudMessageNotification(title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

            notificationManager.notify(0x04, notification)
        }
    }
}