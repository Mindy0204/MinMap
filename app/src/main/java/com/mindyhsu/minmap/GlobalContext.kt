package com.mindyhsu.minmap

import android.app.Application
import android.content.Context

class GlobalContext : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: GlobalContext? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = GlobalContext.applicationContext()
    }
}
