package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.MinMapApplication
import com.mindyhsu.minmap.databinding.FragmentDialogBinding
import com.mindyhsu.minmap.ext.getVmFactory
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
        }

        binding.sendMessage.setOnClickListener {
            val time = Timestamp(Calendar.getInstance().time)
            viewModel.sendMessage(binding.myMessageEditText.text.toString(), time)
            binding.myMessageEditText.text.clear()
        }

        // Chips
        binding.donNotMoveChip.setOnClickListener { }
        binding.illBeThereChip.setOnClickListener { }
        binding.imAtChip.setOnClickListener { }
        binding.whereAreYouChip.setOnClickListener { }
        binding.imHereChip.setOnClickListener { }

        return binding.root
    }
}