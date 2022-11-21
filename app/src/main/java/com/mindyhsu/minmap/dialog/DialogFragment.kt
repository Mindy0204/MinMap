package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.databinding.FragmentDialogBinding
import com.mindyhsu.minmap.ext.getVmFactory
import com.mindyhsu.minmap.map.MapFragmentDirections
import java.util.*

class DialogFragment : Fragment() {

    private lateinit var binding: FragmentDialogBinding
    private val viewModel by viewModels<DialogViewModel> {
        getVmFactory(
            DialogFragmentArgs.fromBundle(
                requireArguments()
            ).chatRoomDetail
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogBinding.inflate(inflater, container, false)

        binding.dialogTitleText.text = viewModel.roomTitle

        val adapter = DialogAdapter(viewModel.uiState)
        binding.dialogRecyclerview.adapter = adapter

        viewModel.messages?.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.dialogRecyclerview.postDelayed({
                binding.dialogRecyclerview.smoothScrollToPosition(adapter.itemCount - 1)
            }, 100)
        }

        binding.sendMessage.setOnClickListener {
            viewModel.sendMessage(binding.myMessageEditText.text.toString())
            binding.myMessageEditText.text.clear()
        }

        // Find Mid-Point
        binding.shareLocation.setOnClickListener {
            viewModel.getMidPoint()
            viewModel.midPoint.observe(viewLifecycleOwner) {
                setFragmentResult(
                    "midPoint",
                    bundleOf("latLng" to it, "participants" to viewModel.participants)
                )
                findNavController().navigate(MapFragmentDirections.navigateToMapFragment())
            }
        }

        // Chips
        binding.donNotMoveChip.setOnClickListener {
            viewModel.sendMessage(getString(R.string.don_not_move_chip))
        }
        binding.whereAreYouChip.setOnClickListener {
            viewModel.sendMessage(getString(R.string.where_are_you_chip))
        }
        binding.imHereChip.setOnClickListener {
            viewModel.sendMessage(getString(R.string.im_here_chip))
        }

        binding.myMessageEditText.doOnTextChanged { text, start, before, count ->
            if (text?.trim().toString() == "") {
                binding.sendMessage.visibility = View.GONE
            } else {
                binding.sendMessage.visibility = View.VISIBLE
            }
        }

        return binding.root
    }
}