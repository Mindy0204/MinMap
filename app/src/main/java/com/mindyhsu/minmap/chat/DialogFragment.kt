package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mindyhsu.minmap.databinding.FragmentDialogBinding

class DialogFragment : Fragment() {

    private lateinit var binding: FragmentDialogBinding
    private lateinit var viewModel: DialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val chatRoomDetail = DialogFragmentArgs.fromBundle(requireArguments()).chatRoomDetail
        viewModel = DialogViewModel(chatRoomDetail)
        Log.d("Minddddddy", "chatRoomDetail=${chatRoomDetail}")

        binding = FragmentDialogBinding.inflate(inflater, container, false)

        binding.dialogPageTitle.text = viewModel.roomTitle

        val adapter = DialogAdapter()
        binding.dialogRecyclerview.adapter = adapter

        return binding.root
    }
}