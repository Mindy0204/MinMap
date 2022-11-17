package com.mindyhsu.minmap.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.databinding.FragmentChatRoomBinding
import com.mindyhsu.minmap.ext.getVmFactory
import timber.log.Timber


class ChatRoomFragment : Fragment() {
    private lateinit var binding: FragmentChatRoomBinding
    private val viewModel by viewModels<ChatRoomViewModel> { getVmFactory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatRoomBinding.inflate(inflater, container, false)

        val adapter = ChatRoomAdapter(viewModel.uiState)
        binding.chatRoomRecyclerview.adapter = adapter

        viewModel.navigateToDialog.observe(viewLifecycleOwner) {
            viewModel.navigateToDialog.value?.let {
                findNavController().navigate(
                    ChatRoomFragmentDirections.actionChatRoomFragmentToDialogFragment(
                        it
                    )
                )
                viewModel.completeNavigateToDialog()
            }
        }

        binding.charRoomAddFriend.setOnClickListener {
            cameraCheckPermission()
        }

        viewModel.getLiveChatRoom.observe(viewLifecycleOwner) {
            viewModel.checkUsersExist(it)
        }

        viewModel.liveChatRoom.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    private fun cameraCheckPermission() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                findNavController().navigate(AddFriendFragmentDirections.navigateToAddFriendFragment())
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
                    findNavController().navigate(AddFriendFragmentDirections.navigateToAddFriendFragment())
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
}