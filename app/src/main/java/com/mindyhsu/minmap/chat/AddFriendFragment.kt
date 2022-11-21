package com.mindyhsu.minmap.chat

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.bindImage
import com.mindyhsu.minmap.databinding.FragmentAddFriendBinding
import com.mindyhsu.minmap.ext.getVmFactory
import com.mindyhsu.minmap.login.UserManager
import kotlinx.coroutines.Runnable


class AddFriendFragment : DialogFragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var binding: FragmentAddFriendBinding
    private val viewModel by viewModels<AddFriendViewModel> { getVmFactory() }

    private var qrDisplay = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddFriendBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        buildCodeScanner()

        binding.addFriendText.setOnClickListener {
            when (qrDisplay) {
                -1 -> {
                    showMyQrCode()
                    qrDisplay = 0
                }
                0 -> {
                    showQrCodeScanner()
                    qrDisplay = -1
                }
            }
        }

        binding.addFriendButton.setOnClickListener {
            if (viewModel.friend.value != null) {
                viewModel.setFriend()
                findNavController().navigateUp()
            } else {
                Toast.makeText(context, R.string.no_qrcode_result, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.hasThisFriend.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigateUp()
                Toast.makeText(context, R.string.add_friend_exist, Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
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
                cameraSource.start(holder)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Close camera
                cameraSource.stop()
            }
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val qrCode: SparseArray<Barcode> = detections.detectedItems
                if (qrCode.size() != 0) {
                    binding.addFriendText.post(Runnable {
                        viewModel.getUserById(qrCode.valueAt(0).displayValue)
                        cameraSource.stop()
                        showScanResult()
                    })
                }
            }
        })
    }

    private fun qrCodeGenerate() {
        val multiFormatWriter = MultiFormatWriter()
        val barcodeEncoder = BarcodeEncoder()
        val bitMatrix: BitMatrix =
            multiFormatWriter.encode(UserManager.id, BarcodeFormat.QR_CODE, 500, 500)
        val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)
        binding.addFriendQrcode.setImageBitmap(bitmap)
    }

    private fun showMyQrCode() {
        qrCodeGenerate()
        binding.surfaceView.visibility = View.GONE
        binding.addFriendQrcode.visibility = View.VISIBLE
        binding.addFriendText.text = getString(R.string.show_qrcode_scanner)
    }

    private fun showQrCodeScanner() {
        buildCodeScanner()
        binding.surfaceView.visibility = View.VISIBLE
        binding.addFriendQrcode.visibility = View.GONE
        binding.addFriendText.text = getString(R.string.show_qrcode)
    }

    private fun showScanResult() {
        viewModel.friend.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.addFriendText.text = it.name
                binding.addFriendImage.visibility = View.VISIBLE
                bindImage(binding.addFriendImage, it.image)
                binding.surfaceView.visibility = View.GONE
                binding.addFriendQrcode.visibility = View.GONE
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.no_this_user),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}