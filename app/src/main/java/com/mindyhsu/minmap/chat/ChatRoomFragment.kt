package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mindyhsu.minmap.databinding.FragmentChatRoomBinding

class ChatRoomFragment : Fragment() {
    private lateinit var binding: FragmentChatRoomBinding
    private lateinit var viewModel: ChatRoomViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        viewModel = ChatRoomViewModel()

        val adapter = ChatRoomAdapter(
            viewModel.uiState
        )
        binding.chatRoomRecyclerview.adapter = adapter

        viewModel.chatRoom.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.navigateToDialog.observe(viewLifecycleOwner) {
            viewModel.navigateToDialog.value?.let {
                findNavController().navigate(ChatRoomFragmentDirections.actionChatRoomFragmentToDialogFragment(it))
            }

        }

        return binding.root
    }
}