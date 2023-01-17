package com.mindyhsu.minmap.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.mindyhsu.minmap.*
import com.mindyhsu.minmap.chat.ChatRoomFragmentDirections
import com.mindyhsu.minmap.databinding.ActivityMainBinding
import com.mindyhsu.minmap.ext.getVmFactory
import timber.log.Timber

const val MESSAGE_INTENT_FILTER = "com.mindyhsu.minmap.DETECT_MESSAGE"
const val KEY_MESSAGE = "message"
private const val MESSAGE_CHANNEL = "messageChannel"
private const val MESSAGE_CHANNEL_ID = 0x02
const val ENTER_CHATROOM = "enterChatRoom"
const val ENTER_CHATROOM_ACTION = "enterChatRoomAction"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel> { getVmFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        if (BuildConfig.TIMBER_VISIABLE) {
            Timber.plant(Timber.DebugTree())
        }

        messageReceiver()

        /** Message notification */
        viewModel.getLiveChatRoom.observe(this) {
            viewModel.getChatRoomIds(it)
        }
        viewModel.getChatRoomIds.observe(this) {
            viewModel.getLiveMessage(it)
        }

        /** Exit foreground service form pending intent */
        if (intent.extras?.get(EXIT_NAVIGATION) == EXIT_NAVIGATION) {
            exitNavigationForegroundService()
        }

        setContentView(binding.root)
    }

    private fun messageReceiver() {
        val filter = IntentFilter(MESSAGE_INTENT_FILTER)
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.extras?.let { bundle ->
                        bundle.getString(KEY_MESSAGE)?.let {
                            messageNotification(it)
                        }
                    }
                }
            },
            filter
        )
    }

    private fun messageNotification(newMessageNum: String) {
        Timber.i("Broadcast receiver notification: $MESSAGE_CHANNEL")
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

        val bundle = Bundle()
        bundle.putString(ENTER_CHATROOM, ENTER_CHATROOM)

        /**
         * Click new message notification for going to chatRoom
         * Need to add "action" that MainActivity can get string from intent
         * */
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                PENDING_INTENT_REQUEST_CODE,
                Intent(this, MainActivity::class.java).apply {
                    putExtras(bundle)
                    action = ENTER_CHATROOM_ACTION
                },
                PendingIntent.FLAG_IMMUTABLE
            )

        val builder = NotificationCompat.Builder(this, MESSAGE_CHANNEL)
            .setSmallIcon(R.mipmap.icon_launcher_minmap_dark)
            .setContentTitle(descriptionText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val notification = builder.build()

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(MESSAGE_CHANNEL_ID, notification)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.extras?.get(EXIT_NAVIGATION) == EXIT_NAVIGATION) {
            exitNavigationForegroundService()
            val bundle = Bundle()
            bundle.putString(EXIT_NAVIGATION, EXIT_NAVIGATION)
        }

        if (intent?.extras?.get(ENTER_CHATROOM) == ENTER_CHATROOM) {
            findNavController(R.id.fragmentContainerView).navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
        }
    }

    private fun exitNavigationForegroundService() {

        viewModel.stopForegroundUpdate()

        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra(NAVIGATION_COMPLETE, NAVIGATION_COMPLETE)
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }
}
