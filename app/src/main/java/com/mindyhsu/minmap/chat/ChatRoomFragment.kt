package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.mindyhsu.minmap.R
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.databinding.FragmentChatRoomBinding

class ChatRoomFragment : Fragment() {
    private lateinit var binding: FragmentChatRoomBinding
    private lateinit var viewModel: ChatRoomViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatRoomBinding.inflate(inflater, container, false)

        val adapter = ChatRoomAdapter()
        binding.chatRoomRecyclerview.adapter = adapter
        // Allows Data Binding to Observe LiveData with the lifecycle of this Fragment
        binding.lifecycleOwner = this

        viewModel = ChatRoomViewModel()

//        viewModel.friendList.observe(viewLifecycleOwner) {
//
//        }
        val friendList = mutableListOf<ChatRoom>()
        val chatRoom1 = ChatRoom(
            eventId = "bg2XkutGcicdPElJ3vTQ",
            id = "tOQfOQ5Fc3TRqHBOE9od",
            participants = listOf("Mindy, Wayne"),
            messages = listOf(Message(id = "GnGMAzxQq3xLrBpYA6rP", senderId = "Wayne", text = "Hi, how are you"))
        )
        val chatRoom2 = ChatRoom(
            eventId = "",
            id = "tOQfOQ5Fc3TRqHBOE9od",
            participants = listOf("Mindy, Beva"),
            messages = listOf(Message(id = "GnGMAzxQq3xLrBpYA6rP", senderId = "Beva", text = "Hi, how are you"))
        )
        friendList.add(chatRoom1)
        friendList.add(chatRoom2)
        adapter.submitList(friendList)

        return binding.root
    }
}