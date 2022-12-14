package com.mindyhsu.minmap.login

import android.content.Context
import com.mindyhsu.minmap.MinMapApplication

object UserManager {
    private const val ID = "id"
    private const val IMAGE = "image"
    private const val NAME = "name"

    private val sharedPreferencesId =
        MinMapApplication.instance.getSharedPreferences(ID, Context.MODE_PRIVATE)
    private val sharedPreferencesImage =
        MinMapApplication.instance.getSharedPreferences(IMAGE, Context.MODE_PRIVATE)
    private val sharedPreferencesName =
        MinMapApplication.instance.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var id: String?
        set(value) {
            sharedPreferencesId.edit().putString(ID, value).apply()
        }
        get() {
            return sharedPreferencesId.getString(ID, null)
        }

    var image: String?
        set(value) {
            sharedPreferencesImage.edit().putString(IMAGE, value).apply()
        }
        get() {
            return sharedPreferencesImage.getString(IMAGE, null)
        }

    var name: String?
        set(value) {
            sharedPreferencesName.edit().putString(NAME, value).apply()
        }
        get() {
            return sharedPreferencesName.getString(NAME, null)
        }
}
