package com.mindyhsu.minmap.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.mindyhsu.minmap.MinMapApplication

object Util {
    fun isInternetConnected(): Boolean {
        val connectivityManager = MinMapApplication.instance
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    fun getString(resourceId: Int): String {
        return MinMapApplication.instance.getString(resourceId)
    }
}
