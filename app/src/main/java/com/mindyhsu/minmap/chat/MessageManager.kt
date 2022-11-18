package com.mindyhsu.minmap.chat

import android.content.Context
import com.mindyhsu.minmap.MinMapApplication

object MessageManager {
    private val sharedPreferences =
        MinMapApplication.instance.getSharedPreferences("message", Context.MODE_PRIVATE)

//    var message: List<Map<String, Int>>
//        set(value) {
//            sharedPreferences.edit().putStringSet("message", value).apply()
//        }
//        get() {
//            return sharedPreferences.getString("message", null)
//        }
}