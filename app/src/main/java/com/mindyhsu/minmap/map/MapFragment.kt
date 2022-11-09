package com.mindyhsu.minmap.map

import android.Manifest
import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindyhsu.minmap.BuildConfig
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.chat.ChatRoomFragmentDirections
import com.mindyhsu.minmap.databinding.FragmentMapBinding
import com.mindyhsu.minmap.ext.getVmFactory
import timber.log.Timber


class MapFragment : Fragment(),
    OnRequestPermissionsResultCallback, OnMapClickListener {
    private lateinit var binding: FragmentMapBinding
    private val viewModel by viewModels<MapViewModel> { getVmFactory() }

    private lateinit var map: GoogleMap
    private val AUTOCOMPLETE_REQUEST_CODE = 1

    var marker = LatLng(0.0, 0.0)

    private var showFunctionButton = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        binding.backToPositionButton.setOnClickListener {
            enableMyLocation()
            getDeviceLocation()
        }

        // Main Entry Page Display
        viewModel.currentEventId.observe(viewLifecycleOwner) {
            if (it == "") {
                // UI
                binding.createEventButton.visibility = View.VISIBLE
                binding.createEventButton.text = context?.getString(R.string.create_new_event)
                binding.cardView.visibility = View.GONE
                binding.cardViewText.visibility = View.GONE
                binding.startNavigationButton.visibility = View.GONE

                // Function
                map.setOnMapClickListener(this)
                binding.createEventButton.setOnClickListener {
                    searchPlace()
                }
            } else {
                // UI
                map.setOnMapClickListener(null)
                binding.createEventButton.visibility = View.GONE

                if (!viewModel.isStartNavigation) {
                    // UI
                    binding.startNavigationButton.visibility = View.VISIBLE
                    viewModel.currentEventDisplay.observe(viewLifecycleOwner) {
                        binding.cardView.visibility = View.VISIBLE
                        binding.cardViewText.visibility = View.VISIBLE
                        binding.cardViewText.text = it
                    }

                    // Function
                    getDeviceLocation()?.let { myLocation ->
                        viewModel.getCurrentEventLocation(map, myLocation)
                    }
                }
            }
        }

        binding.startNavigationButton.setOnClickListener {
            binding.startNavigationButton.visibility = View.GONE
            binding.cardView.visibility = View.VISIBLE
            binding.cardViewText.text = getString(R.string.start_navigation)
            viewModel.startNavigation()
            viewModel.updateFriendsLocation()
        }

        val adapter = FriendLocationAdapter(viewModel.uiState)
        binding.friendsLocationRecyclerView.adapter = adapter
//        viewModel.userList.observe(viewLifecycleOwner) {
//            adapter.submitList(it)
//            viewModel.markFriendsLocation(map)
//        }

        viewModel.onFriendsLiveReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                viewModel.friends.observe(viewLifecycleOwner) {
//                    if (viewModel.markerList.size != 0) {
//                        for (i in 0 until viewModel.markerList.size) {
//                            viewModel.markerList.removeAt(i)
//                        }
//                    }
//                    map.clear()
                    viewModel.markFriendsLocation(map, it)
                    adapter.submitList(it)
                }
            }
        }

        viewModel.navigationInstruction.observe(viewLifecycleOwner) {
            binding.cardViewText.text = it
        }

        // Click friend's profile in card view
        viewModel.checkFriendLocation.observe(viewLifecycleOwner) {
            // Move smoothly
            val cameraPosition = CameraPosition.Builder()
                .target(it)
                .zoom(15F)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

        viewModel.isFinishNavigation.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.cardViewText.visibility = View.GONE
                binding.cardView.visibility = View.GONE
                binding.createEventButton.visibility = View.VISIBLE
                map.clear()
                viewModel.isStartNavigation = false
                findNavController().navigate(NavigationSuccessFragmentDirections.navigateToNavigationSuccessFragment())
            }
        }

        viewModel.isOnInvitation.observe(viewLifecycleOwner) {
            if (it) {
                binding.sendEventButton.visibility = View.VISIBLE
            } else {
                binding.sendEventButton.visibility = View.GONE
            }
        }

        binding.menuButton.setOnClickListener {
            showAdvancedFunction()
        }

        binding.planningButton.setOnClickListener {
            searchPlace()
        }

        binding.chatButton.setOnClickListener {
            /*  // Test function: catch mid-point
            // Mock Data
            // AppWorks School, Regent Taipei
            val friendLocation = LatLng(25.03850539224151, 121.53237404271704)
            val friend2Location = LatLng(25.05445587415607, 121.52420733306852)
            // Taipei Main Station
            val myLocation = LatLng(25.047605887381874, 121.51708580765687)

            val locationList: MutableList<LatLng> = mutableListOf()
            locationList.add(myLocation)
            locationList.add(friendLocation)
            locationList.add(friend2Location)

            val midPoint = viewModel.getMidPoint(locationList)
            Log.i("Mindy", "$midPoint")

            val markerFriend = LatLng(midPoint.latitude, midPoint.longitude)
            map.addMarker(MarkerOptions().position(markerFriend))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(markerFriend, 15F)) */

            findNavController().navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
        }

        binding.sendEventButton.setOnClickListener {
            viewModel.sendEvent(marker)
            binding.sendEventButton.visibility = View.GONE
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            map.uiSettings.setAllGesturesEnabled(true)
            showFunctionButton = -1

            enableMyLocation()
            getDeviceLocation()
        }
    }

    private fun enableMyLocation() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = false
                return
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getDeviceLocation()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun getDeviceLocation(): LatLng? {
        val service = context?.getSystemService(LOCATION_SERVICE) as LocationManager?
        val provider = service?.getBestProvider(Criteria(), false)
        val location = provider?.let {
            context?.let { context ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
//                    return
                } else {
                    enableMyLocation()
                }
            }
            service.getLastKnownLocation(it)
        }

        val latLng = location?.let { LatLng(it.latitude, it.longitude) }
        val cameraUpdate = latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15F) }
        if (cameraUpdate != null) {
            map.animateCamera(cameraUpdate)
        }
        return latLng
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun showAdvancedFunction() {
        when (showFunctionButton) {
            -1 -> {
                showFunctionButton = 0
                binding.chatButton.visibility = View.VISIBLE
                binding.planningButton.visibility = View.VISIBLE
                binding.backToPositionButton.visibility = View.GONE
                map.uiSettings.setAllGesturesEnabled(false)
            }
            0 -> {
                showFunctionButton = -1
                binding.chatButton.visibility = View.GONE
                binding.planningButton.visibility = View.GONE
                binding.backToPositionButton.visibility = View.VISIBLE
                map.uiSettings.setAllGesturesEnabled(true)
            }
        }
    }

    private fun searchPlace() {
        // Initialize Places SDK
        context?.let { Places.initialize(it, BuildConfig.MAPS_API_KEY) }

        // Set the fields to specify which types of place data to return after the user has made a selection
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        // Start the autocomplete intent
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(context)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        Log.d("Mindddddddy", "${place.name},${place.latLng}")

                        place.latLng?.let { latLng -> viewModel.onPlanningLocation(map, latLng) }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {}
                Activity.RESULT_CANCELED -> {}
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun markLocation(latLng: LatLng) {
        binding.sendEventButton.visibility = View.VISIBLE
        marker = latLng
        map.addMarker(MarkerOptions().position(marker))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15F))
    }

    override fun onMapClick(latlng: LatLng) {
        map.clear()
        markLocation(latlng)
    }
}



