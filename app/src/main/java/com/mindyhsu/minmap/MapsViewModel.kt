package com.mindyhsu.minmap

import androidx.lifecycle.ViewModel
import com.mindyhsu.minmap.data.Direction
import kotlinx.coroutines.*

class MapsViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun getDirection() {
        coroutineScope.launch {
            MinMapApi.retrofitService.getDirection(
                startLocation = "25.042544669012685,121.5328892693387",
                endLocation = "25.03850539224151,121.53237404271704",
                apiKey = "***REMOVED***"
            )
        }
    }
}