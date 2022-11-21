package com.mindyhsu.minmap.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mindyhsu.minmap.chat.AddFriendViewModel
import com.mindyhsu.minmap.chat.ChatRoomViewModel
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.LoginViewModel
import com.mindyhsu.minmap.main.MainViewModel
import com.mindyhsu.minmap.map.MapViewModel
import com.mindyhsu.minmap.map.NavigationSuccessViewModel
import com.mindyhsu.minmap.map.SendInvitationViewModel

class ViewModelFactory constructor(
    private val repository: MinMapRepository
) : ViewModelProvider.NewInstanceFactory(){

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(MainViewModel::class.java) ->
                    MainViewModel(repository)

                isAssignableFrom(LoginViewModel::class.java) ->
                    LoginViewModel(repository)

                isAssignableFrom(MapViewModel::class.java) ->
                    MapViewModel(repository)

                isAssignableFrom(NavigationSuccessViewModel::class.java) ->
                    NavigationSuccessViewModel(repository)

                isAssignableFrom(ChatRoomViewModel::class.java) ->
                    ChatRoomViewModel(repository)

                isAssignableFrom(AddFriendViewModel::class.java) ->
                    AddFriendViewModel(repository)

                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}