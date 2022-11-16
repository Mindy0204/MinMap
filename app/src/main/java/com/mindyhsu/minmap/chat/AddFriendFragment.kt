package com.mindyhsu.minmap.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.databinding.FragmentAddFriendBinding
import timber.log.Timber


class AddFriendFragment : DialogFragment() {
    private lateinit var binding: FragmentAddFriendBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddFriendBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        cameraCheckPermission()
        buildCodeScanner()

        return binding.root
    }

    private fun cameraCheckPermission() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                return
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.CAMERA
                    ), CAMERA_PERMISSION_REQUEST_CODE
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
            CAMERA_PERMISSION_REQUEST_CODE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    buildCodeScanner()
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.camera_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }

    private fun buildCodeScanner() {
        val barcodeDetector =
            BarcodeDetector.Builder(MinMapApplication.instance).setBarcodeFormats(Barcode.QR_CODE)
                .build()
        val cameraSource = CameraSource.Builder(MinMapApplication.instance, barcodeDetector)
            .setAutoFocusEnabled(true).build()

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int,
                width: Int, height: Int
            ) {
                Timber.d("surfaceChanged")

            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.d("surfaceCreated")

                if (ContextCompat.checkSelfPermission(MinMapApplication.instance, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraSource.start(holder)

                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Close camera
                cameraSource.stop()
                Timber.d("surfaceDestroyed")

            }
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {  }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val qrCode: SparseArray<Barcode> = detections.detectedItems
                if (qrCode.size() != 0) {
                    binding.addFriendText.post {
                        binding.addFriendText.text = qrCode.valueAt(0).displayValue
                    }
                }
            }
        })
    }
}