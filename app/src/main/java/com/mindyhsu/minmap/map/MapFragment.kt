package com.mindyhsu.minmap.map

import android.Manifest
import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindyhsu.minmap.BuildConfig
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.chat.ChatRoomFragmentDirections
import com.mindyhsu.minmap.databinding.FragmentMapBinding


class MapFragment : Fragment(),
    OnRequestPermissionsResultCallback, OnMapClickListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var viewModel: MapViewModel

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
        viewModel = MapViewModel()

        binding.backToPosition.setOnClickListener {
            enableMyLocation()
            getMyLocation()
        }

        viewModel.currentEventId.observe(viewLifecycleOwner) {
            if (viewModel.currentEventId.value == "") {
                map.clear()
                binding.homeNotice.text = context?.getString(R.string.create_new_event)
                binding.homeNotice.setOnClickListener { searchPlace() }
            } else {
                binding.homeNotice.text = context?.getString(R.string.show_event)
                binding.homeNotice.setOnClickListener { viewModel.getCurrentEventLocation(map) }
            }
        }

        viewModel.currentEventDetail.observe(viewLifecycleOwner) {
            viewModel.currentEventDetail.value?.let {
                findNavController().navigate(
                    CheckEventFragmentDirections.navigateToCheckEventFragment(
                        it
                    )
                )
            }
        }

        viewModel.isStartNavigation = MapFragmentArgs.fromBundle(requireArguments()).startNavigation

        binding.startNavigation.setOnClickListener {
            binding.startNavigation.visibility = View.GONE
            binding.routeGuideView.visibility = View.VISIBLE
            binding.guideText.text = getString(R.string.start_navigation)
            viewModel.startNavigation()
        }

        viewModel.navigationInstruction.observe(viewLifecycleOwner) {
            binding.guideText.visibility = View.VISIBLE
            binding.guideText.text = viewModel.navigationInstruction.value
        }

        viewModel.isFinishNavigation.observe(viewLifecycleOwner) {
            if (viewModel.isFinishNavigation.value == true) {
                binding.guideText.visibility = View.GONE
                binding.routeGuideView.visibility = View.GONE
                binding.homeNotice.visibility = View.VISIBLE
                map.clear()
                findNavController().navigate(NavigationSuccessFragmentDirections.navigateToNavigationSuccessFragment())
            }
        }

        viewModel.isOnInvitation.observe(viewLifecycleOwner) {
            if (it) {
                binding.sendInvitation.visibility = View.VISIBLE
            } else {
                binding.sendInvitation.visibility = View.GONE
            }
        }

        binding.functionMenu.setOnClickListener {
            showAdvancedFunction()
        }

        binding.functionPlanning.setOnClickListener {
            searchPlace()
        }

        binding.functionChat.setOnClickListener {
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
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(markerFriend, 15F))*/

            findNavController().navigate(ChatRoomFragmentDirections.navigateToChatRoomFragment())
        }

        binding.sendInvitation.setOnClickListener {
            viewModel.sendInvitation(marker)
            binding.sendInvitation.visibility = View.GONE
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
            map.setOnMapClickListener(this)

            enableMyLocation()

            if (viewModel.isStartNavigation) {
                getMyLocation()?.let { myLocation ->
                    viewModel.getRoute(map, myLocation)
                }
                binding.homeNotice.visibility = View.GONE
                binding.startNavigation.visibility = View.VISIBLE
            }
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
//                viewModel.startLocationUpdates()
                map.uiSettings.isMyLocationButtonEnabled = false
                return
            } else {
                activity?.let { fragmentActivity ->
                    ActivityCompat.requestPermissions(
                        fragmentActivity, arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ), LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun getMyLocation(): LatLng? {
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
                binding.functionChat.visibility = View.VISIBLE
                binding.functionPlanning.visibility = View.VISIBLE
                binding.backToPosition.visibility = View.GONE
                map.uiSettings.setAllGesturesEnabled(false)
            }
            0 -> {
                showFunctionButton = -1
                binding.functionChat.visibility = View.GONE
                binding.functionPlanning.visibility = View.GONE
                binding.backToPosition.visibility = View.VISIBLE
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
        binding.sendInvitation.visibility = View.VISIBLE
        marker = latLng
        map.addMarker(MarkerOptions().position(marker))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15F))
    }

    override fun onMapClick(latlng: LatLng) {
        map.clear()
        map.addMarker(MarkerOptions().position(latlng))
    }
}



