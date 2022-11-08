package com.mindyhsu.minmap.map

import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.source.MinMapRepository
import com.mindyhsu.minmap.login.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NavigationSuccessViewModel(private val repository: MinMapRepository) : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun finishEvent() {
        coroutineScope.launch {
            repository.finishEvent(UserManager.id)
        }
    }
}