package com.mindyhsu.minmap.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.mindyhsu.minmap.data.source.DefaultMinMapRepository
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.data.source.remote.MinMapRemoteDataSource

object ServiceLocator {

    @Volatile
    var minMapRepository: MinMapRepository? = null
        @VisibleForTesting set

    fun provideTasksRepository(context: Context): MinMapRepository {
        synchronized(this) {
            return minMapRepository
                ?: minMapRepository
                ?: createMinMapRepository(context)
        }
    }

    private fun createMinMapRepository(context: Context): MinMapRepository {
        return DefaultMinMapRepository(
            MinMapRemoteDataSource
        )
    }
}