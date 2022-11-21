package com.mindyhsu.minmap.ext

import android.app.Activity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.factory.ViewModelFactory

/**
 * Extension functions for Activity.
 */
fun Activity.getVmFactory(): ViewModelFactory {
    val repository = (applicationContext as MinMapApplication).repository
    return ViewModelFactory(repository)
}