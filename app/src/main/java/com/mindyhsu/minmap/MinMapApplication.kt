package com.mindyhsu.minmap

import android.app.Application
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.util.ServiceLocator
import kotlin.properties.Delegates

class MinMapApplication : Application() {

    val repository: MinMapRepository
        get() = ServiceLocator.provideTasksRepository(this)

    companion object {
        var instance: MinMapApplication by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
