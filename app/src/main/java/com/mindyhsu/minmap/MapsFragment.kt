package com.mindyhsu.minmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mindyhsu.minmap.databinding.FragmentMapsBinding


class MapsFragment : Fragment(),
    OnMyLocationButtonClickListener,
    OnRequestPermissionsResultCallback {

    private lateinit var binding: FragmentMapsBinding
    private lateinit var viewModel: MapsViewModel

    private var permissionDenied = false
    private lateinit var map: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->
        val taipei = LatLng(25.0330, 121.5654)
        googleMap.addMarker(MarkerOptions().position(taipei).title("Marker in Taipei"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(taipei))
        map = googleMap
    }

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
            viewModel.getDirection()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
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


    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

