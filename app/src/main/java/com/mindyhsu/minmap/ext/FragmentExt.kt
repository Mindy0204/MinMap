package com.mindyhsu.minmap.ext

import androidx.fragment.app.Fragment
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.factory.DialogViewModelFactory
import com.mindyhsu.minmap.factory.ViewModelFactory

/**
 * Extension functions for Fragment.
 */
fun Fragment.getVmFactory(): ViewModelFactory {
    val repository = (requireContext().applicationContext as MinMapApplication).repository
    return ViewModelFactory(repository)
}

fun Fragment.getVmFactory(chatRoomDetail: ChatRoom): DialogViewModelFactory {
    val repository = (requireContext().applicationContext as MinMapApplication).repository
    return DialogViewModelFactory(repository, chatRoomDetail)
}