package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
            findNavController().navigate(AddFriendFragmentDirections.navigateToAddFriendFragment())
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
}