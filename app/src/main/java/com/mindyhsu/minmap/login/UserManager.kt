package com.mindyhsu.minmap.login

import android.content.Context
import com.mindyhsu.minmap.MinMapApplication


object UserManager {
    private val sharedPreferencesId =
        MinMapApplication.instance.getSharedPreferences("id", Context.MODE_PRIVATE)
    private val sharedPreferencesImage =
        MinMapApplication.instance.getSharedPreferences("image", Context.MODE_PRIVATE)
    private val sharedPreferencesName =
        MinMapApplication.instance.getSharedPreferences("name", Context.MODE_PRIVATE)

    var id: String?
        set(value) {
            sharedPreferencesId.edit().putString("id", value).apply()
        }
        get() {
            return sharedPreferencesId.getString("id", null)
        }

    var image: String?
        set(value) {
            sharedPreferencesImage.edit().putString("image", value).apply()
        }
        get() {
            return sharedPreferencesImage.getString("image", null)
        }

    var name: String?
        set(value) {
            sharedPreferencesName.edit().putString("name", value).apply()
        }
        get() {
            return sharedPreferencesName.getString("name", null)
        }

//    fun isLogin(): Boolean {
//        return id != null
//    }
}