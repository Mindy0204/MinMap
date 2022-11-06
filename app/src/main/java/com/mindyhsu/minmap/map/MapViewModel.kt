package com.mindyhsu.minmap.map

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mindyhsu.minmap.BuildConfig
import com.mindyhsu.minmap.GlobalContext
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.network.MinMapApi
import com.mindyhsu.minmap.data.Event
import com.mindyhsu.minmap.data.Step
import com.mindyhsu.minmap.login.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val db = Firebase.firestore

    private val _currentEventDetail = MutableLiveData<Event>()
    val currentEventDetail: LiveData<Event>
        get() = _currentEventDetail

    private var _currentEventId = MutableLiveData<String?>()
    val currentEventId: LiveData<String?>
        get() = _currentEventId

    var isStartNavigation: Boolean = false
    private var routeSteps = listOf<Step>()
    var step = 0

    private var _navigationInstruction = MutableLiveData<String>()
    val navigationInstruction: LiveData<String>
        get() = _navigationInstruction

    private var _isFinishNavigation = MutableLiveData<Boolean>()
    val isFinishNavigation: LiveData<Boolean>
        get() = _isFinishNavigation

    private var _isOnInvitation = MutableLiveData<Boolean>()
    val isOnInvitation: LiveData<Boolean>
        get() = _isOnInvitation

    init {
        getUserEvent()
        updateUser()
    }

    private fun getUserEvent() {
        db.collection("users").document(UserManager.id).get().addOnSuccessListener { user ->
            val eventList = user.data?.get("currentEvent") as List<String>
            if (eventList.isNotEmpty()) {
                _currentEventId.value = eventList[0]
            } else {
                _currentEventId.value = ""
            }
        }
    }

    private fun updateUser() {
        db.collection("users").document(UserManager.id).addSnapshotListener { _, _ ->
            getUserEvent()
        }
    }

    fun getCurrentEventLocation(map: GoogleMap) {
        lateinit var currentEventGeoPoint: GeoPoint

        db.collection("events").document(_currentEventId.value.toString())
            .get().addOnSuccessListener { event ->
                event.toObject(Event::class.java)?.let { currentEvent ->
                    _currentEventDetail.value = currentEvent
                    currentEvent.geoHash?.let { geo ->
                        currentEventGeoPoint = geo
                    }
                }

                val location = LatLng(currentEventGeoPoint.latitude, currentEventGeoPoint.longitude)
                map.addMarker(MarkerOptions().position(location))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
            }
    }

    fun onPlanningLocation(map: GoogleMap, latLng: LatLng) {
        val location = LatLng(latLng.latitude, latLng.longitude)
        map.addMarker(MarkerOptions().position(location))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15F))
        _isOnInvitation.value = true
    }

    fun getRoute(map: GoogleMap, myLocation: LatLng) {
        // TODO: why currentEventId can access but currentEventDetail can't ???

        var _currentEventDetail = Event()
        db.collection("events").document(_currentEventId.value.toString())
            .get().addOnSuccessListener { event ->
                event.toObject(Event::class.java)?.let { currentEvent ->
                    _currentEventDetail = currentEvent
                }

                coroutineScope.launch {
                    val directionResult = MinMapApi.retrofitService.getDirection(
                        startLocation = "${myLocation.latitude}, ${myLocation.longitude}",
                        endLocation = "${_currentEventDetail?.geoHash?.latitude}, " +
                                "${_currentEventDetail?.geoHash?.longitude}",
                        apiKey = BuildConfig.MAPS_API_KEY,
                        mode = "walking"
                    )

                    _currentEventDetail.geoHash?.let {
                        map.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)))
                    }

                    // draw the route
                    val polylineOptions = PolylineOptions()
                    directionResult?.routes?.let {
                        for (routeItem in it) {
                            for (legItem in routeItem.legs) {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            legItem.startLocation.lat,
                                            legItem.startLocation.lng
                                        ), 15F
                                    )
                                )
                                routeSteps = legItem.steps

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
                    }
                    map.addPolyline(polylineOptions)
                }
            }
        isStartNavigation = false
    }

    fun startNavigation() {
//        _isFinishNavigation.value = false
        val locationManager = GlobalContext.applicationContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = LocationListener {
            showRouteGuide(it)
            updateMyLocation(GeoPoint(it.latitude, it.longitude))
        }
//        if (_isFinishNavigation.value != true) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0F,
                locationListener
            )
//        } else {
//            locationManager.removeUpdates(locationListener)
//        }
    }

    private fun updateMyLocation(userGeo: GeoPoint) {
        db.collection("users").document(UserManager.id).update("geoHash", userGeo)
    }

    private fun showRouteGuide(myLocation: Location) {
        val stepEndLocation = Location("stepEndLocation")
        stepEndLocation.latitude = routeSteps[step].endLocation.lat
        stepEndLocation.longitude = routeSteps[step].endLocation.lng

        if (myLocation.distanceTo(stepEndLocation) <= 20) { // 20 meter
            if (step != routeSteps.size - 1) {
                step += 1
            } else {
//                _isFinishNavigation.value = true
            }

            var direction = ""
            routeSteps[step].maneuver?.let {
                direction = "Direction: $it"
            }
            _navigationInstruction.value =
                direction + "\nDuration: " + routeSteps[step].duration.text
        }
    }

    fun sendInvitation(latLng: LatLng) {
        val documentRef = db.collection("events").document()

        val event = hashMapOf(
            "id" to documentRef.id,
            "status" to "0", // ing
            "participants" to listOf("Mindy", "Wayne"),
            "geoHash" to GeoPoint(latLng.latitude, latLng.longitude)
        )

        db.collection("events").document(documentRef.id).set(event)

        _isOnInvitation.value = false
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
}