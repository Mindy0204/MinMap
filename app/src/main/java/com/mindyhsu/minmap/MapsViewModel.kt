package com.mindyhsu.minmap

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.mindyhsu.minmap.data.Direction
import kotlinx.coroutines.*

class MapsViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun getDirection(map: GoogleMap, startLocation: LatLng, endLocation: LatLng) {
        coroutineScope.launch {
            val start = "${startLocation.latitude},${startLocation.longitude}"
            val end = "${endLocation.latitude},${endLocation.longitude}"

//            val directionResult = MinMapApi.retrofitService.getDirection(
//                startLocation = start,
//                endLocation = end,
//                apiKey = "***REMOVED***"
//            )

            val directionResult = MinMapApi.retrofitService.getDirection(
                startLocation = "25.042544669012685,121.5328892693387",
                endLocation = "25.03850539224151,121.53237404271704",
                apiKey = "***REMOVED***"
            )

            val polylineOptions = PolylineOptions()
            for (routeItem in directionResult.routes) {
                for (legItem in routeItem.legs) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(legItem.startLocation.lat, legItem.startLocation.lng), 15F))
                    for (stepItem in legItem.steps) {
                        polylineOptions.add(LatLng(stepItem.startLocation.lat, stepItem.startLocation.lng))
                    }
                }
            }
            map.addPolyline(polylineOptions)
        }
    }
}