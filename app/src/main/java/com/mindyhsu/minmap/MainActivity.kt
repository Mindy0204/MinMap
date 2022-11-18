package com.mindyhsu.minmap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.app.NotificationCompat
import com.mindyhsu.minmap.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        registerReceiver()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        messageReceiver()
    }

    override fun onPause() {
        super.onPause()
        messageReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter(Intent.ACTION_VIEW)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {}
        }, filter)
    }

    private fun messageReceiver() {
        val filter = IntentFilter("com.mindyhsu.minmap.DETECT_MESSAGE")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                var message = intent?.getStringExtra("message").toString()
                val i = Intent(context, BroadcastReceiverService::class.java)
                startService(i.putExtra("message", message))
            }
        }, filter)
    }
}
