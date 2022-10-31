package com.mindyhsu.minmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
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
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mindyhsu.minmap.databinding.FragmentMapsBinding


class MapsFragment : Fragment(),
    OnRequestPermissionsResultCallback {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var viewModel: MapsViewModel

    private var permissionDenied = false
    private lateinit var map: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->
        val taipei = LatLng(25.03850539224151, 121.53237404271704)
        googleMap.addMarker(MarkerOptions().position(taipei).title("Marker in AppWorks School"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(taipei))
        map = googleMap
    }

    var viewStatus = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        viewModel = MapsViewModel()

        binding.backToPosition.setOnClickListener {
            enableMyLocation()
            getMyLocation()
        }

        binding.functionMenu.setOnClickListener {
            advancedFunction()
        }

        binding.functionPlanning.setOnClickListener {
//            viewModel.getDirection(map)
            findNavController().navigate(MapSearchFragmentDirections.navigateToSearchMapFragment())
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        if (MapsFragmentArgs.fromBundle(requireArguments()).endLocation != null) {
            val selectedLocation = MapsFragmentArgs.fromBundle(requireArguments()).endLocation
            Log.d("Mindy", "$selectedLocation")

            selectedLocation?.latLng?.let {
                val maker = LatLng(it.latitude, it.longitude)
//                map.addMarker(MarkerOptions().position(maker))
//                map.moveCamera(CameraUpdateFactory.newLatLng(maker))
            }
        }
    }

    @SuppressLint("MissingPermission")
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

    private fun getMyLocation() {
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
                    return
                } else {
                    enableMyLocation()
                }
            }
            service.getLastKnownLocation(it)
        }

        val latLng = location?.let { LatLng(it.latitude, location.longitude) }
        val cameraUpdate = latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15F) }
        if (cameraUpdate != null) {
            map.animateCamera(cameraUpdate)
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun advancedFunction() {
        when (viewStatus) {
            -1 -> {
                viewStatus = 0
                binding.functionChat.visibility = View.VISIBLE
                binding.functionPlanning.visibility = View.VISIBLE
                binding.backToPosition.visibility = View.GONE
                map.uiSettings.setAllGesturesEnabled(false)
            }
            0 -> {
                viewStatus = -1
                binding.functionChat.visibility = View.GONE
                binding.functionPlanning.visibility = View.GONE
                binding.backToPosition.visibility = View.VISIBLE
                map.uiSettings.setAllGesturesEnabled(true)
            }
        }


    }
}



