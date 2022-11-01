package com.mindyhsu.minmap.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindyhsu.minmap.data.ChatRoom
import com.mindyhsu.minmap.databinding.ItemChatRoomBinding

class ChatRoomAdapter(private val onClickListener: OnClickListener) :
    ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    class ChatRoomViewHolder(private var binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatRoom) {
            var chatRoomName = ""
            for (participant in item.participants) {
                chatRoomName = "$chatRoomName $participant"
            }
            binding.friendName.text = chatRoomName

            if (item.eventId != "") {
                binding.eventReminder.visibility = View.VISIBLE
            }
        }
    }

    class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        return ChatRoomViewHolder(
            ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val chatRoom = getItem(position)

        holder.itemView.setOnClickListener {
            onClickListener.onClick(chatRoom)
        }

        holder.bind(chatRoom)
    }

    class OnClickListener(val clickListener: (chatRoom: ChatRoom) -> Unit) {
        fun onClick(chatRoom: ChatRoom) = clickListener(chatRoom)
    }
}