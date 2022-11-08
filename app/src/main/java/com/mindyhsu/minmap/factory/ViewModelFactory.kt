package com.mindyhsu.minmap.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindyhsu.minmap.chat.ChatRoomViewModel
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.map.MapViewModel
import com.mindyhsu.minmap.map.NavigationSuccessViewModel

class ViewModelFactory constructor(
    private val repository: MinMapRepository
) : ViewModelProvider.NewInstanceFactory(){

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(MapViewModel::class.java) ->
                    MapViewModel(repository)

                isAssignableFrom(NavigationSuccessViewModel::class.java) ->
                    NavigationSuccessViewModel(repository)

                isAssignableFrom(ChatRoomViewModel::class.java) ->
                    ChatRoomViewModel(repository)

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}