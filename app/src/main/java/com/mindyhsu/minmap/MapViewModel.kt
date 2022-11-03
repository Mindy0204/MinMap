package com.mindyhsu.minmap

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.data.Event
import com.mindyhsu.minmap.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val db = Firebase.firestore

    lateinit var userData: User

    private var _hasCurrentEvent = MutableLiveData<Boolean>()
    val hasCurrentEvent: LiveData<Boolean>
        get() = _hasCurrentEvent

    private val _hasEventDetail = MutableLiveData<Event>()
    val hasEventDetail: LiveData<Event>
        get() = _hasEventDetail

    var currentEventId = ""
    var currentEventWith = ""
    lateinit var eventGeoPoint: GeoPoint

    var isStartNavigation: Boolean = false

    private var _isInviting = MutableLiveData<Boolean>()
    val isInviting: LiveData<Boolean>
        get() = _isInviting

    init {
        getUser()
        userChange()
    }

    private fun getUser() {
        // user id: D7uCAaCvEsUSM5hl5yeK
        db.collection("users").document("D7uCAaCvEsUSM5hl5yeK")
            .get().addOnSuccessListener { user ->
                user.data?.let {
                    userData = User(
                        id = it["id"] as String,
                        image = it["image"] as String,
                        name = it["name"] as String,
                        geoHash = it["geoHash"] as GeoPoint,
                        currentEvent = it["currentEvent"] as List<String>,
                        friends = it["friends"] as List<String>
                    )
                    _hasCurrentEvent.value = userData.currentEvent.isNotEmpty()
                    if (userData.currentEvent.isNotEmpty()) {
                        currentEventId = userData.currentEvent[0]
                    }

                }
            }
    }

    private fun userChange() {
        db.collection("users").document("D7uCAaCvEsUSM5hl5yeK")
            .addSnapshotListener { _, _ ->
                getUser()
            }
    }

    fun getLocation(map: GoogleMap, latLng: LatLng?) {
        db.collection("events").document(currentEventId)
            .get().addOnSuccessListener { event ->
                event.data?.let {
                    _hasEventDetail.value = Event(
                        id = it["id"] as String,
                        status = it["status"] as String,
                        participants = it["participants"] as List<String>,
                        place = it["place"] as String,
                        geoHash = it["geoHash"] as GeoPoint,
                        time = it["time"] as Timestamp
                    )
                }

                eventGeoPoint = event.data?.get("geoHash") as GeoPoint

                // Source 1: click map location -> parameter: latLng
                // Source 2: click planning button and search location -> parameter: latLng
                // Source 3: click event notice button -> parameter: eventGeoPoint
                val marker = latLng ?: LatLng(eventGeoPoint.latitude, eventGeoPoint.longitude)

                map.addMarker(MarkerOptions().position(marker))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15F))

                // Source 1 & 2 situation
                _isInviting.value = latLng != null
            }
    }

    fun getRoute(map: GoogleMap, myLocation: LatLng) {
        var eventDetail: Event? = null

        db.collection("events").document(currentEventId)
            .get().addOnSuccessListener { event ->
                event.data?.let {
                    eventDetail = Event(
                        id = it["id"] as String,
                        status = it["status"] as String,
                        participants = it["participants"] as List<String>,
                        place = it["place"] as String,
                        geoHash = it["geoHash"] as GeoPoint,
                        time = it["time"] as Timestamp
                    )
                }


                coroutineScope.launch {
                    val directionResult = MinMapApi.retrofitService.getDirection(
                        startLocation = "${myLocation.latitude}, ${myLocation.longitude}",
                        endLocation = "${eventDetail?.geoHash?.latitude}, ${eventDetail?.geoHash?.longitude}",
                        apiKey = BuildConfig.MAPS_API_KEY,
                        mode = "walking"
                    )

                    // mock data
//                    val directionResult = MinMapApi.retrofitService.getDirection(
//                        startLocation = "25.042544669012685,121.5328892693387",
//                        endLocation = "25.054456266969456, 121.52410146733696",
//                        apiKey = BuildConfig.MAPS_API_KEY,
//                        mode = "walking"
//                    )

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
        isStartNavigation = false
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

        _isInviting.value = false
    }
}