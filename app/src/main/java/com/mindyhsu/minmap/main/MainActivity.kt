package com.mindyhsu.minmap.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mindyhsu.minmap.*
import com.mindyhsu.minmap.databinding.ActivityMainBinding
import com.mindyhsu.minmap.ext.getVmFactory
import timber.log.Timber

const val CHAT_ROOM_INTENT_FILTER = "com.mindyhsu.minmap.DETECT_CHAT_ROOM"
const val MESSAGE_INTENT_FILTER = "com.mindyhsu.minmap.DETECT_MESSAGE"
const val KEY_CHAT_ROOM = "chatRoom"
const val KEY_MESSAGE = "message"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModels<MainViewModel> { getVmFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        if (BuildConfig.TIMBER_VISIABLE) {
            Timber.plant(Timber.DebugTree())
        }

        /** chatRoom and message notification */
        viewModel.getLiveChatRoom.observe(this) {
            viewModel.getChatRoomIds(it)
        }

        viewModel.getChatRoomIds.observe(this) {
            viewModel.getLiveMessage(it)
        }

        chatRoomReceiver()
        messageReceiver()

        /** Exit foreground service form pending intent */
        if (intent.extras?.get(EXIT_NAVIGATION) == EXIT_NAVIGATION) {
            exitNavigationForegroundService()
        }

        setContentView(binding.root)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.extras?.get(EXIT_NAVIGATION) == EXIT_NAVIGATION) {
            exitNavigationForegroundService()
            val bundle = Bundle()
            bundle.putString(EXIT_NAVIGATION, EXIT_NAVIGATION)
        }
    }

    private fun chatRoomReceiver() {
        val filter = IntentFilter(CHAT_ROOM_INTENT_FILTER)
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val chatRoom = intent?.getStringExtra(KEY_CHAT_ROOM).toString()
                    val i = Intent(context, BroadcastReceiverService::class.java)
                    startService(i.putExtra(KEY_CHAT_ROOM, chatRoom))
                }
            },
            filter
        )
    }

    private fun messageReceiver() {
        val filter = IntentFilter(MESSAGE_INTENT_FILTER)
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val message = intent?.getStringExtra(KEY_MESSAGE).toString()
                    val i = Intent(context, BroadcastReceiverService::class.java)
                    startService(i.putExtra(KEY_MESSAGE, message))
                }
            },
            filter
        )
    }

    private fun exitNavigationForegroundService() {

        viewModel.stopForegroundUpdate()

        val serviceIntent = Intent(MinMapApplication.instance, ForegroundService::class.java)
        serviceIntent.putExtra(NAVIGATION_COMPLETE, NAVIGATION_COMPLETE)
        ContextCompat.startForegroundService(MinMapApplication.instance, serviceIntent)
    }
}
