package com.mindyhsu.minmap

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.Direction
import kotlinx.coroutines.*

class MapsViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val db = Firebase.firestore

    fun getRoute(map: GoogleMap) {
        coroutineScope.launch {
            // parameter: startLocation: LatLng, endLocation: LatLng
//            val start = "${startLocation.latitude},${startLocation.longitude}"
//            val end = "${endLocation.latitude},${endLocation.longitude}"

//            val directionResult = MinMapApi.retrofitService.getDirection(
//                startLocation = start,
//                endLocation = end,
//                apiKey = "***REMOVED***",
//                mode = "walking"
//            )

            val directionResult = MinMapApi.retrofitService.getDirection(
                startLocation = "25.042544669012685,121.5328892693387",
                endLocation = "25.054456266969456, 121.52410146733696",
                apiKey = "***REMOVED***",
                mode = "walking"
            )

            val polylineOptions = PolylineOptions()
            for (routeItem in directionResult.routes) {
                for (legItem in routeItem.legs) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                legItem.startLocation.lat,
                                legItem.startLocation.lng
                            ), 15F
                        )
                    )
                    for (stepItem in legItem.steps) {
                        polylineOptions.add(
                            LatLng(
                                stepItem.startLocation.lat,
                                stepItem.startLocation.lng
                            )
                        )
                    }
                }
            }
            map.addPolyline(polylineOptions)
        }
    }

    fun getMidPoint(locationList: MutableList<LatLng>): LatLng {
        var totalLat = 0.0
        var totalLon = 0.0
        val listSize = locationList.size
        for (location in locationList) {
            totalLat += location.latitude
            totalLon += location.longitude
        }
        return LatLng(totalLat / listSize, totalLon / listSize)
    }

    fun setEvent(latLng: LatLng) {
        val documentRef = db.collection("events").document()

        val event = hashMapOf(
            "id" to documentRef.id,
            "status" to "0", // ing
            "participants" to listOf("Mindy", "Wayne"),
            "geoHash" to GeoPoint(latLng.latitude, latLng.longitude)
        )

        db.collection("events").document(documentRef.id).set(event)
    }
}