package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mindyhsu.minmap.databinding.FragmentDialogBinding
import com.mindyhsu.minmap.ext.getVmFactory

class DialogFragment : Fragment() {

    private lateinit var binding: FragmentDialogBinding
    private val viewModel by viewModels<DialogViewModel> {
        getVmFactory(
            DialogFragmentArgs.fromBundle(
                requireArguments()
            ).chatRoomDetail
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        val chatRoomDetail = DialogFragmentArgs.fromBundle(requireArguments()).chatRoomDetail

        binding = FragmentDialogBinding.inflate(inflater, container, false)

        binding.dialogPageTitle.text = viewModel.roomTitle

        val adapter = DialogAdapter()
        binding.dialogRecyclerView.adapter = adapter

        viewModel.dialogs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        // Chips
        binding.donNotMove.setOnClickListener { }
        binding.illBeThereIn.setOnClickListener { }
        binding.imAt.setOnClickListener { }
        binding.whereAreYou.setOnClickListener { }
        binding.imHere.setOnClickListener { }

        return binding.root
    }
}