package com.mindyhsu.minmap.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mindyhsu.minmap.data.Message
import com.mindyhsu.minmap.databinding.FragmentDialogBinding

class DialogFragment : Fragment() {

    private lateinit var binding: FragmentDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        val chatRoomId = DialogFragmentArgs.fromBundle(requireArguments()).chatRoomId

        binding = FragmentDialogBinding.inflate(inflater, container, false)

        val adapter = DialogAdapter()
        binding.dialogRecyclerview.adapter = adapter

        val chatList = mutableListOf<Message>()
        val message1 = Message(id = "GnGMAzxQq3xLrBpYA6rP", senderId = "Wayne", text = "Hi, how are you")
        val message2 = Message(id = "JvotWfs0w81xQyBUNC1s", senderId = "Mindy", text = "i'm fine, thank you, and you")
        chatList.add(message1)
        chatList.add(message2)
        adapter.submitList(chatList)

        return binding.root
    }
}